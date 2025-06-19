package ru.agecold.gameserver.model.entity;

import javolution.util.FastList;
import ru.agecold.Config;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.model.L2Summon;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.TvTEvent.StoredBuff;
import ru.agecold.util.Location;
import ru.agecold.util.Rnd;

public class TvTEventTeleporter implements Runnable {

    /**
     * The instance of the player to teleport
     */
    private L2PcInstance _player;
    /**
     * Coordinates of the spot to teleport to
     */
    private Location _coordinates;
    /**
     * Admin removed this player from event
     */
    private boolean _adminRemove;

    private FastList<StoredBuff> _sb;

    /**
     * Initialize the teleporter and start the delayed task
     *
     * @param playerInstance
     * @param coordinates
     * @param reAdd
     */
    public TvTEventTeleporter(L2PcInstance playerInstance, Location coordinates, boolean fastSchedule, boolean adminRemove) {
        createTask(playerInstance, coordinates, fastSchedule, adminRemove, null);
    }

    public TvTEventTeleporter(L2PcInstance playerInstance, Location coordinates, boolean fastSchedule, boolean adminRemove, FastList<StoredBuff> sb) {
        createTask(playerInstance, coordinates, fastSchedule, adminRemove, sb);
    }

    private void createTask(L2PcInstance playerInstance, Location coordinates, boolean fastSchedule, boolean adminRemove, FastList<StoredBuff> sb) {
        _player = playerInstance;
        _coordinates = coordinates;
        _adminRemove = adminRemove;
        _sb = sb;

        // in config as seconds
        long delay = (TvTEvent.isStarted() ? Config.TVT_EVENT_RESPAWN_TELEPORT_DELAY : Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000;

        if (fastSchedule) {
            delay = 0;
        }

        ThreadPoolManager.getInstance().scheduleGeneral(this, delay);
    }

    /**
     * The task method to teleport the player<br> 1. Unsummon pet if there is
     * one 2. Remove all effects 3. Revive and full heal the player 4. Teleport
     * the player 5. Broadcast status and user info
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if (_player == null) {
            return;
        }

        if (TvTEvent.isStarted() && !_adminRemove) {
            if (!TvTEvent.isRegisteredPlayer2(_player.getObjectId())) {
                return;
            }
        }

        L2Summon summon = _player.getPet();

        if (summon != null) {
            summon.unSummon(_player);
        }

        _player.stopAllEffects();
        _player.doRevive();

        if (TvTEvent.isStarted() && !_adminRemove) {
            _player.setEventChannel(8);
            _player.setTeam(TvTEvent.getParticipantTeamId(_player.getName()) + 1);
        } else {
            _player.setEventChannel(1);
            _player.setTeam(0);
            _player.restoreEventSkills();

            if (Config.TVT_KILLS_OVERLAY) {
                //String rsp = _teams[0].getName() +" (" + String.valueOf(_teams[0].getPoints()) + ") x " + _teams[1].getName() + " (" +String.valueOf(_teams[1].getPoints()) + ")";

                    //_player.sendMessage(rsp);
            }

            if (Config.TVT_SAVE_BUFFS && _sb != null) {
                _player.setBuffing(true);
                SkillTable st = SkillTable.getInstance();
                for (StoredBuff s : _sb) {
                    if (s == null) {
                        continue;
                    }

                    _player.stopSkillEffects(s.id);
                    if (Config.TVT_SAVE_BUFFS_TIME) {
                        st.getInfo(s.id, s.lvl).getEffects(_player, _player, s.count, s.time);
                    } else {
                        st.getInfo(s.id, s.lvl).getEffects(_player, _player);
                    }
                }
                _player.setBuffing(false);
                _player.updateEffectIcons();
            }
        }

        _player.setCurrentCp(_player.getMaxCp());
        _player.setCurrentHp(_player.getMaxHp());
        _player.setCurrentMp(_player.getMaxMp());

        if (Config.TVT_TELE_PROTECT > 0) {
            _player.setTvtProtection(Config.TVT_TELE_PROTECT);
        }
        _player.setEventWait(false);
        _player.teleToLocationEvent(_coordinates.x + Rnd.get(100), _coordinates.y + Rnd.get(100), _coordinates.z, false);

        _player.broadcastStatusUpdate();
    }
}
