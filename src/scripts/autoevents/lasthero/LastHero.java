package scripts.autoevents.lasthero;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javolution.util.FastList;

import ru.agecold.Config;
import ru.agecold.Config.EventReward;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.ItemTable;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.TvTEvent;
import ru.agecold.gameserver.model.entity.olympiad.Olympiad;
import ru.agecold.gameserver.network.serverpackets.ConfirmDlg;
import ru.agecold.util.Location;
import ru.agecold.util.Rnd;
import scripts.autoevents.basecapture.BaseCapture;
import scripts.autoevents.masspvp.massPvp;

public class LastHero {

    protected static final Logger _log = Logger.getLogger(LastHero.class.getName());
    private static final long _arRestart = (Config.ELH_ARTIME * 60000);
    private static final long _regTime = (Config.ELH_REGTIME * 60000);
    private static final long _anTime = (Config.ELH_ANNDELAY * 60000);
    private static final long _tpDelay = (Config.ELH_TPDELAY * 60000);
    private static final long _nextTime = (Config.ELH_NEXT * 60000);
    private static final int _minPlayers = Config.ELH_MINP;
    private static final int _maxPlayers = Config.ELH_MAXP;
    private static final Location _tpLoc = Config.ELH_TPLOC;
    private static final int _ticketId = Config.ELH_TICKETID;
    private static final int _ticketCount = Config.ELH_TICKETCOUNT;
    private static final ConcurrentLinkedQueue<Integer> _players = new ConcurrentLinkedQueue<>();
    private static final FastList<Config.EventReward> _rewards = Config.ELH_REWARDS;
    private static final ConcurrentLinkedQueue<String> _ips = new ConcurrentLinkedQueue<>();
    //

    static enum EventState {

        WAIT,
        REG,
        BATTLE
    }
    private static EventState _state = EventState.WAIT;
    private static LastHero _event;

    public static void init() {
        _event = new LastHero();
        _event.load();
    }

    public static LastHero getEvent() {
        return _event;
    }

    public void load() {
        checkTimer();
    }

    public void checkTimer() {
        ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _arRestart);
        System.out.println("EventManager: Last Hero, start after " + (_arRestart / 60000) + " min.");
    }

    public class StartTask implements Runnable {

        public void run() {
            if (_state == EventState.WAIT) {
                startEvent();
            }
        }
    }

    private void startEvent() {
        _state = EventState.REG;

        announce(Static.LH_STARTED);
        announce(Static.LH_REG_FOR_S1.replace("%a%", String.valueOf(((_regTime / 60000) + 1))));
        System.out.println("EventManager: Last Hero, registration opened.");

        if (Config.EVENT_REG_POPUP) {
            for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
                if (player == null || player.getLevel() < Config.EVENT_REG_POPUPLVL) {
                    continue;
                }

                if (player.isShowPopup()) {
                    player.sendPacket(new ConfirmDlg(614, "Принять участие в ивенте -Последний герой-?", 106));
                }
            }
        }

        _ips.clear();
        _players.clear();
        ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceTask(), _anTime);
    }

    public class AnnounceTask implements Runnable {

        @Override
        public void run() {
            if (_state != EventState.REG) {
                return;
            }

            long regMin = _regTime;
            for (int i = 0; i < _regTime; i += _anTime) {
                try {
                    regMin -= _anTime;
                    announce(Static.LH_REG_IN);
                    announce(Static.LH_REG_LOST_S1.replace("%a%", String.valueOf(((regMin / 60000) + 1))));
                    if (_players.isEmpty()) {
                        announce(Static.LH_NO_PLAYESR_YET);
                    } else {
                        announce(Static.LH_REGD_PLAYESR.replace("%a%", String.valueOf(_players.size())));
                    }
                    Thread.sleep(_anTime);
                } catch (InterruptedException e) {
                }
            }
            announce(Static.LH_REG_CLOSED);
            _state = EventState.BATTLE;

            if (_players.size() < _minPlayers) {
                _state = EventState.WAIT;
                announce(Static.LH_CANC_NO_PLAYERS);
                announce(Static.LH_NEXT_TIME.replace("%a%", String.valueOf((_nextTime / 60000))));
                System.out.println("EventManager: Last Hero, canceled: no players.");
                System.out.println("EventManager: Last Hero, next start after " + (_nextTime / 60000) + " min.");
                ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
                return;
            }
            announce(Static.LH_TELEPORT_AFTER.replace("%a%", String.valueOf((_tpDelay / 60000))));
            System.out.println("EventManager: Last Hero, teleport start after " + (_tpDelay / 60000) + " min.");
            ThreadPoolManager.getInstance().scheduleGeneral(new StartTeleport(), _tpDelay);
        }
    }

    public class StartTeleport implements Runnable {

        public StartTeleport() {
        }

        @Override
        public void run() {
            L2PcInstance player = null;
            for (Integer player_id : _players) {
                if (player_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(player_id);
                if (player == null) {
                    _players.remove(player_id);
                    continue;
                }

                /*if (player.isDead()) {
                 notifyDeath(player);
                 }*/
                if (player.isDead()) {
                    player.restoreExp(100.0);
                    player.doRevive();
                }
            }

            for (Integer player_id : _players) {
                if (player_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(player_id);
                if (player == null) {
                    _players.remove(player_id);
                    continue;
                }

                if (Config.FORBIDDEN_EVENT_ITMES) {
                    // снятие переточеных вещей
                    for (L2ItemInstance item : player.getInventory().getItems()) {
                        if (item == null) {
                            continue;
                        }

                        if (item.notForOly()) {
                            player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
                        }
                    }
                }

                for (L2Skill s : player.getAllSkills()) {
                    if (s == null) {
                        continue;
                    }
                    if (s.isForbidEvent()) {
                        player.removeStatsOwner(s);
                    }
                }

                player.setEventWait(true);
                player.setEventChannel(6);

                if (player.getParty() != null) {
                    player.getParty().oustPartyMember(player);
                }

                player.teleToLocationEvent(_tpLoc.x + Rnd.get(300), _tpLoc.y + Rnd.get(300), _tpLoc.z);
                player.stopAllEffects();
                player.setCurrentCp(player.getMaxCp());
                player.setCurrentHp(player.getMaxHp());
                player.setCurrentMp(player.getMaxMp());
            }
            player = null;
            announce(Static.LH_BATTLE_AFTER.replace("%a%", String.valueOf(Config.ELH_BATTLEDELAY / 1000)));
            ThreadPoolManager.getInstance().scheduleGeneral(new StartBattle(), Config.ELH_BATTLEDELAY);
            System.out.println("EventManager: Last Hero, battle start after " + Config.ELH_BATTLEDELAY / 1000 + " sec.");
        }
    }

    public class StartBattle
            implements Runnable
    {
        public StartBattle() {}

        public void run()
        {
            L2PcInstance player = null;
            for (Integer player_id : _players) {
                if (player_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(player_id);
                if (player == null) {
                    _players.remove(player_id);
                }
                else
                {
                    player.setTeam(1);
                    player.setPVPArena(true);
                    player.setEventWait(false);
                }
            }
            player = null;
            announce(Static.LH_BATTLE_START);
            ThreadPoolManager.getInstance().scheduleGeneral(new WinTask(), 10000);
            System.out.println("EventManager: Last Hero, battle started.");
        }
    }

    public class WinTask implements Runnable {

        public WinTask() {
        }

        @Override
        public void run() {
            if (_players.size() == 1) {
                announceWinner(_players.peek());
            } else {
                ThreadPoolManager.getInstance().scheduleGeneral(new WinTask(), 10000);
            }
        }
    }

    public void announceWinner(Integer player_id) {
        if (player_id == null) {
            return;
        }

        L2PcInstance player = L2World.getInstance().getPlayer(player_id);
        if (player == null) {
            return;
        }

        player.setEventChannel(1);
        _state = EventState.WAIT;
        announce(Static.LH_DONE);
        announce(Static.LH_WINNER.replace("%a%", player.getName()));
        announce(Static.LH_NEXT_AFTER.replace("%a%", String.valueOf((_nextTime / 60000))));
        System.out.println("EventManager: Last Hero, finished; player " + player.getName() + " win.");
        System.out.println("EventManager: Last Hero, next start after " + (_nextTime / 60000) + " min.");
        if (!player.isHero()) {
            player.setHero(Config.ELH_HERO_DAYS);
        }
        player.setPVPArena(false);

        for (FastList.Node<EventReward> k = _rewards.head(), endk = _rewards.tail(); (k = k.getNext()) != endk;) {
            EventReward reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (Rnd.get(100) < reward.chance) {
                L2ItemInstance rewItem = ItemTable.getInstance().createItem("LastHero", reward.id, reward.count, player, null);
                player.getInventory().addItem("LastHero", rewItem, player, null);
            }
        }

        player.restoreEventSkills();
        player.teleToLocation(82737, 148571, -3470);

        ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
    }

    private boolean foundIp(String ip) {
        return (_ips.contains(ip));
    }

    public void regPlayer(L2PcInstance player) {
        if (_state != EventState.REG) {
            player.sendHtmlMessage("-Последний герой-", "Регистрация еще не обьявлялась<br1> Приходите позже ;).");
            return;
        }

        if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(player.getName())) {
            player.sendHtmlMessage("-Последний герой-", "Вы уже зарегистрированы на TvT.");
            return;
        }

        if (player.getKarma() > 0 || player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("У вас плохая карма.");
            return;
        }

        if (Config.MASS_PVP && massPvp.getEvent().isReg(player)) {
            player.sendHtmlMessage("-Последний герой-", "Удачи на евенте -Масс ПВП-");
            return;
        }
        if (Config.EBC_ENABLE && BaseCapture.getEvent().isRegged(player)) {
            player.sendHtmlMessage("-Последний герой-", "Удачи на евенте -Захват базы-");
            return;
        }

        if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode()) {
            player.sendHtmlMessage("-Последний герой-", "Вы уже зарегистрированы на олимпиаде.");
            return;
        }

        if (player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("-Последний герой-", "С проклятым оружием нельзя.");
            return;
        }

        if (_players.size() >= _maxPlayers) {
            player.sendHtmlMessage("-Последний герой-", "Достигнут предел игроков.");
            return;
        }

        if (_players.contains(player.getObjectId())) {
            player.sendHtmlMessage("-Последний герой-", "Вы уже зарегистрированы.");
            return;
        }

        if (!Config.EVENTS_SAME_IP && foundIp(player.getIP())) {
            player.sendHtmlMessage("-Последний герой-", "С вашего IP уже есть игрок.");
            return;
        }

        if (_ticketId > 0) {
            L2ItemInstance coin = player.getInventory().getItemByItemId(_ticketId);
            if (coin == null || coin.getCount() < _ticketCount) {
                player.sendHtmlMessage("-Последний герой-", "Участив в ивенте платное.");
                return;
            }
            player.destroyItemByItemId("lasthero", _ticketId, _ticketCount, player, true);
        }

        _players.add(player.getObjectId());
        if (!Config.EVENTS_SAME_IP) {
            _ips.add(player.getIP());
        }
        player.sendHtmlMessage("-Последний герой-", "Регистрация завершена.");
    }

    public void delPlayer(L2PcInstance player) {
        if (!(_players.contains(player.getObjectId()))) {
            player.sendHtmlMessage("-Последний герой-", "Вы не зарегистрированы.");
            return;
        }

        _players.remove(player.getObjectId());

        if (!Config.EVENTS_SAME_IP) {
            _ips.remove(player.getIP());
        }
        player.sendHtmlMessage("-Последний герой-", "Регистрация отменена.");
    }

    public void notifyFail(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return;
        }

        if (_players.contains(player.getObjectId())) {
            _players.remove(player.getObjectId());
            if (!Config.EVENTS_SAME_IP) {
                _ips.remove(player.getIP());
            }
            player.setEventChannel(1);
            player.setXYZ(82737, 148571, -3470);
            player.setPVPArena(false);
        }
    }

    public void notifyDeath(L2PcInstance player) {
        if (_state == EventState.WAIT || _state == EventState.REG) {
            return;
        }

        if (_players.contains(player.getObjectId())) {
            _players.remove(player.getObjectId());
            if (!Config.EVENTS_SAME_IP) {
                _ips.remove(player.getIP());
            }

            player.sendCritMessage("Вы проиграли...");
            try {
                player.teleToLocationEvent(82737, 148571, -3470);
            } catch (Exception e) {
            }
            player.setEventChannel(1);
            player.doRevive();
            player.setCurrentHp(player.getMaxHp());
            player.setCurrentMp(player.getMaxMp());
            player.setCurrentCp(player.getMaxCp());
            player.setPVPArena(false);
            player.setTeam(0);
            player.restoreEventSkills();
        }
    }

    public boolean isRegged(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return false;
        }

        return _players.contains(player.getObjectId());
    }

    private void announce(String text) {
        Announcements.getInstance().announceToAll(text);
    }

    public boolean isInBattle() {
        return (_state == EventState.BATTLE);
    }

    public boolean isStarted() {
        return _state == EventState.REG || _state == EventState.BATTLE;
    }

    public boolean forbPotion(int itemId) {
        return Config.ELH_FORB_POTIONS.contains(itemId);
    }
}
