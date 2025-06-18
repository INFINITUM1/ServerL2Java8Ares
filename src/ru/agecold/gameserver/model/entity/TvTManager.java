/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package ru.agecold.gameserver.model.entity;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

import javolution.util.FastList;
import ru.agecold.Config;
import ru.agecold.Config.EventReward;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.util.log.AbstractLogger;

/**
 * @author FBIagent
 */
public class TvTManager implements Runnable {

    private static final Logger _log = AbstractLogger.getLogger(TvTManager.class.getName());
    /** The one and only instance of this class<br> */
    private static TvTManager _instance = null;
    private static final FastList<EventReward> _times = new FastList<EventReward>();

    /**
     * New instance only by getInstance()<br>
     */
    private TvTManager() {
        if (Config.TVT_EVENT_ENABLED) {
            if (Config.TVT_EVENT_TIME_START.isEmpty()) {
                ThreadPoolManager.getInstance().scheduleGeneral(this, 0);
            }
            else {
                String[] data = Config.TVT_EVENT_TIME_START.split(";");
                for (String hour : data)
                {
                    String[] time = hour.split(":");
                    try {
                        _times.add(new Config.EventReward(Integer.parseInt(time[0]), Integer.parseInt(time[1]), 0));
                    }
                    catch (NumberFormatException nfe)
                    {}
                }
                EventReward task = null;
                FastList.Node<EventReward> n = _times.head();
                for (FastList.Node<EventReward> end = _times.tail(); (n = n.getNext()) != end;)
                {
                    task = n.getValue();
                    if (task == null) {
                        continue;
                    }
                    System.out.println("TvTEventEngine[TvTManager.TvTManager()]: sheduled at " + task.id + ":" + task.count + ".");
                    ThreadPoolManager.getInstance().scheduleGeneral(this, getNextStart(0, false, task.id, task.count) - System.currentTimeMillis());
                }
            }
        } else {
            _log.info("TvTEventEngine[TvTManager.TvTManager()]: Engine is disabled.");
        }
    }

    private static long getNextStart(long start, boolean nextday, int hour, int minute)
    {
        Calendar tomorrow = new GregorianCalendar();
        if (nextday) {
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        }
        Calendar result = new GregorianCalendar(tomorrow.get(Calendar.YEAR), tomorrow.get(Calendar.MONTH), tomorrow.get(Calendar.DAY_OF_MONTH), hour, minute);

        start = result.getTimeInMillis();
        if (start < System.currentTimeMillis()) {
            return getNextStart(0, true, hour, minute);
        }
        return start;
    }

    /**
     * Initialize new/Returns the one and only instance<br><br>
     *
     * @return TvTManager<br>
     */
    public static TvTManager getInstance() {
        if (_instance == null) {
            _instance = new TvTManager();
        }

        return _instance;
    }

    /**
     * The task method to handle cycles of the event<br><br>
     *
     * @see java.lang.Runnable#run()<br>
     */
    public void run() {
        _log.info("TvTEventEngine[TvTManager.TvTManager()]: Started.");
        TvTEvent.init();
        if (Config.TVT_EVENT_TIME_START.isEmpty()) {
            for (;;) {
                waiter(Config.TVT_EVENT_INTERVAL * 60); // in config given as minutes

                if (!TvTEvent.startParticipation()) {
                    Announcements.getInstance().announceToAll(Static.TVT_CANCELED);
                    _log.warning("TvTEventEngine[TvTManager.run()]: Error spawning event npc for participation.");
                    continue;
                } else {
                    Announcements.getInstance().announceToAll(Static.TVT_REG_FOR_S1.replaceAll("%a%", String.valueOf(Config.TVT_EVENT_PARTICIPATION_TIME)));
                }

                waiter(Config.TVT_EVENT_PARTICIPATION_TIME * 60); // in config given as minutes

                if (!TvTEvent.startFight()) {
                    Announcements.getInstance().announceToAll(Static.TVT_CANCELED);
                    _log.info("TvTEventEngine[TvTManager.run()]: Lack of registration, abort event.");
                    continue;
                } else {
                    TvTEvent.sysMsgToAllParticipants(Static.TVT_TELE_ARENA_S1.replaceAll("%a%", String.valueOf(Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY)));
                }

                waiter(Config.TVT_EVENT_RUNNING_TIME * 60); // in config given as minutes
                Announcements.getInstance().announceToAll(TvTEvent.calculateRewards());
                TvTEvent.sysMsgToAllParticipants(Static.TVT_RETURN_TOWN_S1.replaceAll("%a%", String.valueOf(+Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY)));
                TvTEvent.stopFight();
            }
        }

        if (!TvTEvent.startParticipation()) {
            Announcements.getInstance().announceToAll(Static.TVT_CANCELED);
            _log.warning("TvTEventEngine[TvTManager.run()]: Error spawning event npc for participation.");
            return;
        }
        Announcements.getInstance().announceToAll(Static.TVT_REG_FOR_S1.replaceAll("%a%", String.valueOf(Config.TVT_EVENT_PARTICIPATION_TIME)));

        waiter(Config.TVT_EVENT_PARTICIPATION_TIME * 60);
        if (!TvTEvent.startFight()) {
            Announcements.getInstance().announceToAll(Static.TVT_CANCELED);
            _log.info("TvTEventEngine[TvTManager.run()]: Lack of registration, abort event.");
            return;
        }
        TvTEvent.sysMsgToAllParticipants(Static.TVT_TELE_ARENA_S1.replaceAll("%a%", String.valueOf(Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY)));

        waiter(Config.TVT_EVENT_RUNNING_TIME * 60);
        Announcements.getInstance().announceToAll(TvTEvent.calculateRewards());
        TvTEvent.sysMsgToAllParticipants(Static.TVT_RETURN_TOWN_S1.replaceAll("%a%", String.valueOf(Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY)));
        TvTEvent.stopFight();
    }

    /**
     * This method waits for a period time delay<br><br>
     *
     * @param interval<br>
     */
    void waiter(int seconds) {
        while (seconds > 1) {
            seconds--; // here because we don't want to see two time announce at the same time

            if (TvTEvent.isParticipating() || TvTEvent.isStarted()) {
                String winner = "Cиние";
                if (TvTEvent.isStarted()) {
                    int[] teamsPointsCounts = TvTEvent.getTeamsPoints();
                    if (teamsPointsCounts[1] > teamsPointsCounts[0]) {
                        winner = "Красные";
                    }
                }

                switch (seconds) {
                    case 3600: // 1 hour left
                        if (TvTEvent.isParticipating()) {
                            Announcements.getInstance().announceToAll(Static.TVT_REG_FOR_S1_HOURS.replaceAll("%a%", String.valueOf((seconds / 60 / 60))));
                        } else if (TvTEvent.isStarted()) {
                            TvTEvent.sysMsgToAllParticipants(Static.TVT_FINISH_FOR_S1_HOURS.replaceAll("%a%", String.valueOf((seconds / 60 / 60))));
                        }
                        break;
                    case 1800: // 30 minutes left
                    case 900: // 15 minutes left
                    case 600: //  10 minutes left
                        if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_MIN_WINNERS_S2.replaceAll("%a%", String.valueOf((seconds / 60)));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 300: // 5 minutes left
                        if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_MIN_WINNERS_S2.replaceAll("%a%", String.valueOf((seconds / 60)));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 240: // 4 minutes left
                    case 180: // 3 minutes left
                    case 120: // 2 minutes left
                    case 60: // 1 minute left
                        if (TvTEvent.isParticipating()) {
                            Announcements.getInstance().announceToAll(Static.TVT_REG_FOR_S1_MIN.replaceAll("%a%", String.valueOf((seconds / 60))));
                            if (Config.CMD_EVENTS) {
                                Announcements.getInstance().announceToAll("Регистрация на ивенты: команда .eventhelp");
                            }
                        } else if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_MIN_WINNERS_S2.replaceAll("%a%", String.valueOf((seconds / 60)));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 30: // 30 seconds left
                        if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_SECS_WINNERS_S2.replaceAll("%a%", String.valueOf(seconds));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 15: // 15 seconds left
                        if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_SECS_WINNERS_S2.replaceAll("%a%", String.valueOf(seconds));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 10: // 10 seconds left
                        if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_SECS_WINNERS_S2.replaceAll("%a%", String.valueOf(seconds));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 5: // 5 seconds left
                        if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_SECS_WINNERS_S2.replaceAll("%a%", String.valueOf(seconds));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 4: // 4 seconds left
                        if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_SEC_WINNERS_S2.replaceAll("%a%", String.valueOf(seconds));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 3: // 3 seconds left
                        if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_SEC_WINNERS_S2.replaceAll("%a%", String.valueOf(seconds));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 2: // 2 seconds left
                        if (TvTEvent.isStarted()) {
                            String event_end = Static.TVT_GAME_END_S1_SEC_WINNERS_S2.replaceAll("%a%", String.valueOf(seconds));
                            TvTEvent.spMsgToAllParticipants(event_end.replaceAll("%b%", winner));
                        }
                        break;
                    case 1: // 1 seconds left
                        if (TvTEvent.isParticipating()) {
                            Announcements.getInstance().announceToAll(Static.TVT_REG_FOR_S1_SEC.replaceAll("%a%", String.valueOf(seconds)));
                        } else if (TvTEvent.isStarted()) {
                            TvTEvent.spMsgToAllParticipants(Static.TVT_GAME_END_S1_SECS.replaceAll("%a%", String.valueOf(seconds)));
                        }
                        break;
                }
            }

            long oneSecWaitStart = System.currentTimeMillis();

            while (oneSecWaitStart + 1000L > System.currentTimeMillis()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}
