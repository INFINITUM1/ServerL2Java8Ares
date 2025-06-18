package scripts.autoevents.basecapture;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import javolution.util.FastList;
import javolution.util.FastMap;
import ru.agecold.Config;
import ru.agecold.Config.EventReward;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.instancemanager.EventManager;
import ru.agecold.gameserver.model.*;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.TvTEvent;
import ru.agecold.gameserver.model.entity.olympiad.Olympiad;
import ru.agecold.gameserver.network.serverpackets.ConfirmDlg;
import ru.agecold.util.Location;
import ru.agecold.util.Rnd;
import scripts.ai.CaptureBase;
import scripts.autoevents.lasthero.LastHero;
import scripts.autoevents.masspvp.massPvp;

public class BaseCapture {

    protected static final Logger _log = Logger.getLogger(BaseCapture.class.getName());
    private static final long _arRestart = (Config.EBC_ARTIME * 60000);
    private static final long _regTime = (Config.EBC_REGTIME * 60000);
    private static final long _anTime = (Config.EBC_ANNDELAY * 60000);
    private static final long _tpDelay = Config.EBC_TPDELAY;
    private static final long _deathDelay = (Config.EBC_DEATHLAY * 1000);
    private static final long _nextTime = (Config.EBC_NEXT * 60000);
    private static final int _minLvl = Config.EBC_MINLVL;
    private static CaptureBase _base1 = null;
    private static CaptureBase _base2 = null;
    private static final FastList<Location> _tpLoc1 = Config.EBC_TPLOC1;
    private static final FastList<Location> _tpLoc2 = Config.EBC_TPLOC2;
    private int _returnCount = 0;
    private final FastList<Location> _returnLocs = Config.EBC_RETURN_COORDINATES;
    //private FastList<L2PcInstance> _team1 = new FastList<L2PcInstance>();
    //private FastList<L2PcInstance> _team2 = new FastList<L2PcInstance>();
    private static final FastMap<Integer, ConcurrentLinkedQueue<Integer>> _teams = new FastMap<>();
    private static final FastList<EventReward> _rewards = Config.EBC_REWARDS;
    private static final FastList<EventReward> _rewardsPremium = Config.EBC_REWARDS_PREM_LIST;
    //private static final FastList<Location> _locs = new FastList<>();
    private static final FastList<String> _teamNames = new FastList<>();
    private static final ConcurrentLinkedQueue<String> _ips = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<String> _hwids = new ConcurrentLinkedQueue<>();
    //

    private static final Map<Integer, FastList<StoredBuff>> _storedBuffs = new ConcurrentHashMap<>();
    private static final FastList<EventReward> _times = new FastList<>();

    public static class StoredBuff {
        public int id;
        public int lvl;
        public int count;
        public int time;

        public StoredBuff(int id, int lvl, int count, int time) {
            this.id = id;
            this.lvl = lvl;
            this.count = count;
            this.time = time;
        }
    }

    static enum EventState {

        WAIT,
        REG,
        BATTLE
    }
    private static EventState _state = EventState.WAIT;
    private static BaseCapture _event;

    public static void init() {
        _event = new BaseCapture();
        _event.load();
    }

    public static BaseCapture getEvent() {
        return _event;
    }

    public void load() {

        _teamNames.add(Config.EBC_BASE1NAME);
        _teamNames.add(Config.EBC_BASE2NAME);

        _ips.clear();
        _hwids.clear();
        _teams.clear();
        _storedBuffs.clear();
        _teams.put(0, new ConcurrentLinkedQueue<>());
        _teams.put(1, new ConcurrentLinkedQueue<>());

        _returnCount = _returnLocs.size() - 1;

        checkTimer();
    }

    public void checkTimer() {
        Config.EventReward task;
        FastList.Node<Config.EventReward> n;
        if(Config.BASE_CAPTURE_TIME_START.isEmpty()) {
            ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _arRestart);
            System.out.println("EventManager: Base Capture, start after " + _arRestart / 60000L + " min.");
        }
        else {
            String[] data = Config.BASE_CAPTURE_TIME_START.split(";");
            for(String hour : data) {
                String[] time = hour.split(":");
                try
                {
                    _times.add(new EventReward(Integer.parseInt(time[0]), Integer.parseInt(time[1]), 0));
                }
                catch(NumberFormatException nfe)
                {}
            }
            task = null;
            n = _times.head();
            for(FastList.Node<EventReward> end = _times.tail(); (n = n.getNext()) != end;)
            {
                task = n.getValue();
                if(task == null) {
                    continue;
                }
                System.out.println("EventManager: Base Capture, sheduled at " + task.id + ":" + task.count + ".");
                ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), getNextStart(0, false, task.id, task.count) - System.currentTimeMillis());
            }
        }
    }

    private static long getNextStart(long start, boolean nextday, int hour, int minute)
    {
        Calendar tomorrow = new GregorianCalendar();
        if(nextday)
        {
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        }
        Calendar result = new GregorianCalendar(tomorrow.get(Calendar.YEAR), tomorrow.get(Calendar.MONTH), tomorrow.get(Calendar.DAY_OF_MONTH), hour, minute);

        start = result.getTimeInMillis();
        if(start < System.currentTimeMillis())
        {
            return getNextStart(0, true, hour, minute);
        }
        return start;
    }

    public class StartTask implements Runnable {

        @Override
        public void run() {
            if (_state == EventState.WAIT) {
                startEvent();
            }
        }
    }

    public void startEvent() {
        _ips.clear();
        _hwids.clear();
        _storedBuffs.clear();
        _teams.clear();
        _teams.put(0, new ConcurrentLinkedQueue<>());
        _teams.put(1, new ConcurrentLinkedQueue<>());

        _state = EventState.REG;

        announce(Static.EBC_STARTED);
        announce(Static.EBC_REG_FOR_S1.replace("%a%", String.valueOf(((_regTime / 60000) + 1))));
        System.out.println("EventManager: Base Capture, registration opened.");

        if (Config.EVENT_REG_POPUP) {
            for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
                if (player == null || player.getLevel() < Config.EVENT_REG_POPUPLVL) {
                    continue;
                }

                player.sendPacket(new ConfirmDlg(614, "Принять участие в ивенте -Захват базы-?", 104));
            }
        }

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
                    announce(Static.EBC_STARTED);
                    announce(Static.EBC_REG_LOST_S1.replace("%a%", String.valueOf(((regMin / 60000) + 1))));
                    if (_teams.get(0).isEmpty() || _teams.get(1).isEmpty()) {
                        announce(Static.EBC_NO_PLAYESR_YET);
                    } else {
                        String announs = Static.EBC_PLAYER_TEAMS.replace("%a%", String.valueOf(_teams.get(0).size()));
                        announs = announs.replace("%b%", Config.EBC_BASE1NAME);
                        announs = announs.replace("%c%", String.valueOf(_teams.get(1).size()));
                        announs = announs.replace("%d%", Config.EBC_BASE2NAME);
                        announce(announs);
                    }
                    Thread.sleep(_anTime);
                } catch (InterruptedException e) {
                }
            }
            announce(Static.EBC_REG_CLOSED);
            _state = EventState.BATTLE;

            if (_teams.get(0).size() < Config.EBC_MINP || _teams.get(1).size() < Config.EBC_MINP) {
                announce(Static.EBC_NO_PLAYERS);
                System.out.println("EventManager: Base Capture, canceled: no players.");
                _state = EventState.WAIT;
                if (Config.BASE_CAPTURE_TIME_START.isEmpty())
                {
                    ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
                    announce(Static.EBC_NEXT_AFTER.replace("%a%", String.valueOf((_nextTime / 60000))));
                    System.out.println("EventManager: Base Capture, next start after " + (_nextTime / 60000) + " min.");
                }

                _state = EventState.WAIT;
                _ips.clear();
                _hwids.clear();
                _teams.clear();
                _storedBuffs.clear();
                _teams.put(0, new ConcurrentLinkedQueue<>());
                _teams.put(1, new ConcurrentLinkedQueue<>());
                return;
            }
            //announce(Static.EBC_BATTLE_STRTED_AFTER.replace("%a%", String.valueOf(_tpDelay)));

            L2PcInstance player = null;
            for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
                ConcurrentLinkedQueue<Integer> players = e.getValue();
                for (Integer player_id : players) {
                    if (player_id == null) {
                        continue;
                    }

                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        continue;
                    }

                    player.sendMessage(Static.EBC_BATTLE_STRTED_AFTER.replace("%a%", String.valueOf(_tpDelay)));
                }
            }
            System.out.println("EventManager: Base Capture, battle start after " + _tpDelay + " sec.");
            ThreadPoolManager.getInstance().scheduleGeneral(new StartBattle(), _tpDelay * 1000);
        }
    }

    public class StartBattle implements Runnable {

        public StartBattle() {
        }

        @Override
        public void run() {
            _base1 = (CaptureBase) EventManager.getInstance().doSpawn(Config.EBC_BASE1ID, Config.EBC_BASE1_LOC, 0);
            _base1.setName(Config.EBC_BASE1NAME);
            _base2 = (CaptureBase) EventManager.getInstance().doSpawn(Config.EBC_BASE2ID, Config.EBC_BASE2_LOC, 0);
            _base2.setName(Config.EBC_BASE2NAME);

            L2PcInstance player = null;
            for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
                ConcurrentLinkedQueue<Integer> players = e.getValue();

                for (Integer player_id : players) {
                    if (player_id == null) {
                        continue;
                    }

                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        continue;
                    }

                    if (Config.EBC_SAVE_BUFFS)
                    {
                        FastList<StoredBuff> sb = new FastList<StoredBuff>();
                        for (L2Effect ef : player.getAllEffects()) {
                            if (ef == null) {
                                continue;
                            }

                            if(!ef.getInUse()) {
                                continue;
                            }

                            if (ef.getSkill().isToggle()) {
                                continue;
                            }
                            if (ef.getSkill().isAugment()) {
                                continue;
                            }

                            sb.add(new StoredBuff(ef.getId(), ef.getLevel(), ef.getCount(), ef.getTime()));
                        }
                        _storedBuffs.put(player.getObjectId(), sb);
                    }

                    if (player.isDead()) {
                        player.restoreExp(100.0);
                        player.doRevive();
                    }
                }
            }

            for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
                Integer teamId = e.getKey();
                ConcurrentLinkedQueue<Integer> players = e.getValue();
                teamId += 1;

                for (Integer player_id : players) {
                    if (player_id == null) {
                        continue;
                    }

                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        continue;
                    }

                    if (Config.FORBIDDEN_EVENT_ITMES) {
                        // снятие переточеных вещей
                        for (L2ItemInstance item : player.getInventory().getItems()) {
                            if (item == null) {
                                continue;
                            }

                            if (item.notForOly()) {
                                player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
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

                    // Remove Summon's Buffs
                    if (player.getPet() != null) {
                        L2Summon summon = player.getPet();
                        summon.stopAllEffects();

                        if (summon.isPet()) {
                            summon.unSummon(player);
                        }
                    }

                    player.setChannel(4);

                    player.teleToLocationEvent(getEventLoc(teamId - 1));

                    buffPlayer(player);
                    player.setCurrentCp(player.getMaxCp());
                    player.setCurrentHp(player.getMaxHp());
                    player.setCurrentMp(player.getMaxMp());
                    player.setTeam(teamId);
                    player.setPVPArena(true);
                }
            }

            System.out.println("EventManager: Base Capture, battle started.");
            ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(), _deathDelay);
        }
    }

    public static void buffPlayer(L2PcInstance player)
    {
        if (player == null || !Config.ALT_BC_BUFFS) {
            return;
        }
        FastMap<Integer, Integer> buffs = null;
        if (player.isMageClass()) {
            buffs = Config.BC_MAGE_BUFFS;
        } else {
            buffs = Config.BC_FIGHTER_BUFFS;
        }
        player.stopAllEffects();
        player.setBuffing(true);
        Integer id = null;
        Integer lvl = null;
        SkillTable _st = SkillTable.getInstance();
        FastMap.Entry<Integer, Integer> e = buffs.head();
        for (FastMap.Entry<Integer, Integer> end = buffs.tail(); (e = e.getNext()) != end;) {
            id = e.getKey();
            lvl = e.getValue();
            if (id == null || lvl == null) {
                continue;
            }

            _st.getInfo(id, lvl).getEffects(player, player);
        }
        player.setBuffing(false);
        player.updateEffectIcons();
    }

    public class DeathTask implements Runnable {

        public DeathTask() {
        }

        public void run() {
            if (_state != EventState.BATTLE) {
                return;
            }
            L2PcInstance player = null;
            for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
                Integer teamId = e.getKey();
                ConcurrentLinkedQueue<Integer> players = e.getValue();
                teamId += 1;

                for (Integer player_id : players) {
                    if (player_id == null) {
                        continue;
                    }

                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        continue;
                    }

                    if (!player.isDead()) {
                        continue;
                    }

                    player.doRevive();

                    player.setCurrentCp(player.getMaxCp());
                    player.setCurrentHp(player.getMaxHp());
                    player.setCurrentMp(player.getMaxMp());
                    player.stopAllEffects();

                    buffPlayer(player);
                    player.broadcastStatusUpdate();
                    player.setTeam(teamId);
                    player.setChannel(4);

                    player.teleToLocationEvent(getEventLoc(teamId - 1));
                    player.setPVPArena(true);
                }
            }
            ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(), _deathDelay);
        }
    }

    public void notifyBaseDestroy(int baseId) {
        if (_state != EventState.BATTLE) {
            return;
        }

        int winTeam = 0;
        if (baseId == Config.EBC_BASE1ID) {
            winTeam = 1;
        }

        if (_base1 != null) {
            _base1.deleteMe();
        }

        if (_base2 != null) {
            _base2.deleteMe();
        }

        _base1 = null;
        _base2 = null;

        _state = EventState.WAIT;
        announce(Static.EBC_FINISHED);
        announce(Static.EBC_TEAM_S1_WIN.replace("%a%", _teamNames.get(winTeam)));

        System.out.println("EventManager: Base Capture, finished; team " + (winTeam + 1) + " win.");

        if (Config.BASE_CAPTURE_TIME_START.isEmpty())
        {
            ThreadPoolManager.getInstance().scheduleGeneral(new StartTask(), _nextTime);
            announce(Static.EBC_NEXT_AFTER.replace("%a%", String.valueOf((_nextTime / 60000))));
            System.out.println("EventManager: Base Capture, next start after " + (_nextTime / 60000) + " min.");
        }

        try {
            validateWinners(winTeam);
        } catch (Exception e) {
            //
        }

        prepareNextEvent();
    }

    private void validateWinners(int team) {
        L2PcInstance player = null;
        ConcurrentLinkedQueue<Integer> players = _teams.get(team);
        for (Integer player_id : players) {
            if (player_id == null) {
                continue;
            }

            player = L2World.getInstance().getPlayer(player_id);
            if (player == null) {
                continue;
            }

            if (player.isPremium() && Config.EBC_REWARDS_PREM) {
                for (FastList.Node<EventReward> k = _rewardsPremium.head(), endk = _rewardsPremium.tail(); (k = k.getNext()) != endk;) {
                    EventReward reward = k.getValue();
                    if (reward == null) {
                        continue;
                    }

                    if (Rnd.get(100) < reward.chance) {
                        player.addItem("Npc.giveItemPrem", reward.id, reward.count, player, true);
                    }
                }
            }
            else
            {
                for (FastList.Node<EventReward> k = _rewards.head(), endk = _rewards.tail(); (k = k.getNext()) != endk;) {
                    EventReward reward = k.getValue();
                    if (reward == null) {
                        continue;
                    }

                    if (Rnd.get(100) < reward.chance) {
                        player.addItem("Npc.giveItem", reward.id, reward.count, player, true);
                    }
                }
            }
        }
    }

    private void prepareNextEvent() {
        L2PcInstance player = null;
        for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
            //Integer teamId = e.getKey();
            ConcurrentLinkedQueue<Integer> players = e.getValue();

            for (Integer player_id : players) {
                if (player_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(player_id);
                if (player == null) {
                    continue;
                }

                if (player.isDead()) {
                    player.restoreExp(100.0);
                    player.doRevive();
                }

                if (Config.EBC_SAVE_BUFFS) {
                    FastList<StoredBuff> sb = _storedBuffs.get(player.getObjectId());
                    if (sb != null && !sb.isEmpty()) {
                        player.setBuffing(true);
                        player.stopAllEffects();
                        SkillTable st = SkillTable.getInstance();
                        for (StoredBuff s : sb) {
                            if (s == null) {
                                continue;
                            }

                            if (Config.EBC_SAVE_BUFFS_TIME) {
                                st.getInfo(s.id, s.lvl).getEffects(player, player, s.count, s.time);
                            } else {
                                st.getInfo(s.id, s.lvl).getEffects(player, player);
                            }
                        }
                        player.setBuffing(false);
                        player.updateEffectIcons();
                    }
                }
            }
        }

        for (FastMap.Entry<Integer, ConcurrentLinkedQueue<Integer>> e = _teams.head(), end = _teams.tail(); (e = e.getNext()) != end;) {
            //Integer teamId = e.getKey();
            ConcurrentLinkedQueue<Integer> players = e.getValue();

            for (Integer player_id : players) {
                if (player_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(player_id);
                if (player == null) {
                    continue;
                }

                player.setCurrentCp(player.getMaxCp());
                player.setCurrentHp(player.getMaxHp());
                player.setCurrentMp(player.getMaxMp());
                player.setChannel(1);
                player.broadcastStatusUpdate();
                player.setTeam(0);
                player.setPVPArena(false);
                player.teleToLocationEvent(getRandomReturnLoc());
            }
        }

        _ips.clear();
        _hwids.clear();
        _teams.clear();
        _storedBuffs.clear();
        _teams.put(0, new ConcurrentLinkedQueue<>());
        _teams.put(1, new ConcurrentLinkedQueue<>());
    }

    private Location getRandomReturnLoc() {
        return _returnLocs.get(Rnd.get(0, _returnCount));
    }

    private boolean foundIp(String ip) {
        return (_ips.contains(ip));
    }

    private boolean foundHWID(String id) {
        return (_hwids.contains(id));
    }

    public void regPlayer(L2PcInstance player) {
        if (_state != EventState.REG) {
            player.sendHtmlMessage("-Захват базы-", "Регистрация на эвент закрыта.");
            return;
        }
        if (_state == EventState.BATTLE) {
            player.sendHtmlMessage("-Захват базы-", "Битва уже обьявлена!");
            return;
        }

        if (player.getKarma() > 0 || player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("У вас плохая карма.");
            return;
        }

        if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(player.getName())) {
            player.sendHtmlMessage("-Захват базы-", "Вы уже зарегистрированы на TvT.");
            return;
        }

        if (Config.MASS_PVP && massPvp.getEvent().isReg(player)) {
            player.sendMessage("Удачи на евенте -Масс ПВП-");
            return;
        }

        if (Config.ELH_ENABLE && LastHero.getEvent().isRegged(player)) {
            player.sendHtmlMessage("-Захват базы-", "Вы уже зарегистрированы в -Последний герой- эвенте.");
            return;
        }
        if (Config.EBC_ENABLE && BaseCapture.getEvent().isRegged(player)) {
            player.sendHtmlMessage("-Захват базы-", "Удачи на евенте -Захват базы-");
            return;
        }

        if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode()) {
            player.sendHtmlMessage("-Захват базы-", "Вы уже зарегистрированы на олимпиаде.");
            return;
        }

        if (player.isCursedWeaponEquiped()) {
            player.sendHtmlMessage("-Захват базы-", "С проклятым оружием нельзя.");
            return;
        }

        if (_teams.get(0).size() >= Config.EBC_MAXP && _teams.get(1).size() >= Config.EBC_MAXP) {
            player.sendHtmlMessage("-Захват базы-", "Достигнут предел игроков.");
            return;
        }

        if (_teams.get(0).contains(player.getObjectId()) || _teams.get(1).contains(player.getObjectId())) {
            player.sendHtmlMessage("-Захват базы-", "Вы уже зарегистрированы.");
            return;
        }

        if (!Config.EVENTS_SAME_IP && foundIp(player.getIP())) {
            player.sendHtmlMessage("-Захват базы-", "С вашего IP уже есть игрок.");
            return;
        }

        if (!Config.EVENTS_SAME_HWID && foundHWID(player.getHWID())) {
            player.sendHtmlMessage("-Захват базы-", "С вашего компьюетра уже есть игрок.");
            return;
        }

        if (Config.EBC_TICKETID > 0) {
            L2ItemInstance coin = player.getInventory().getItemByItemId(Config.EBC_TICKETID);
            if (coin == null || coin.getCount() < Config.EBC_TICKETCOUNT) {
                player.sendHtmlMessage("-Захват базы-", "Участив в ивенте платное.");
                return;
            }
        }

        int team = 0;
        if (_teams.get(0).size() == _teams.get(1).size()) {
            team = Rnd.get(0, 1);
        } else if (_teams.get(0).size() > _teams.get(1).size()) {
            team = 1;
        }

        _teams.get(team).add(player.getObjectId());
        player.sendHtmlMessage("-Захват базы-", "Регистрация завершена, ваша команда: <br> " + _teamNames.get(team) + ".");
        if (!Config.EVENTS_SAME_IP) {
            _ips.add(player.getIP());
        }
        if (!Config.EVENTS_SAME_HWID) {
            _hwids.add(player.getHWID());
        }
    }

    public void delPlayer(L2PcInstance player) {
        if (_state != EventState.REG) {
            player.sendHtmlMessage("-Захват базы-", "Сейчас не регистрационный период.");
            return;
        }

        if (!(_teams.get(0).contains(player.getObjectId())) && !(_teams.get(1).contains(player.getObjectId()))) {
            player.sendHtmlMessage("-Захват базы-", "Вы не зарегистрированы.");
            return;
        }

        if (_teams.get(0).contains(player.getObjectId())) {
            _teams.get(0).remove(player.getObjectId());
        } else if (_teams.get(1).contains(player.getObjectId())) {
            _teams.get(1).remove(player.getObjectId());
        }

        if (!Config.EVENTS_SAME_IP) {
            _ips.remove(player.getIP());
        }

        if (!Config.EVENTS_SAME_HWID) {
            _hwids.remove(player.getHWID());
        }

        player.sendHtmlMessage("-Захват базы-", "Регистрация отменена.");
    }

    public static void onLogout(L2PcInstance player) {
        /*if (!isRegBattle(player)) {
         return;
         }*/

 /*Location loc = null;
         if (_teams.get(0).contains(player.getObjectId())) {
         loc = _locs.get(0);
         } else if (_teams.get(1).contains(player.getObjectId())) {
         loc = _locs.get(1);
         }

         if (loc == null) {
         return;
         }

         player.teleToLocation(loc);*/
    }

    public static void onLogin(L2PcInstance player) {
        if (!isRegBattle(player)) {
            return;
        }

        int teamId = 0;
        Location loc = null;
        if (_teams.get(0).contains(player.getObjectId())) {
            loc = getEventLoc(0);
            teamId = 1;
        } else if (_teams.get(1).contains(player.getObjectId())) {
            loc = getEventLoc(1);
            teamId = 2;
        }

        if (loc == null) {
            return;
        }

        if (player.isDead()) {
            player.restoreExp(100.0);
            player.doRevive();
        }

        if (Config.FORBIDDEN_EVENT_ITMES) {
            // снятие переточеных вещей
            for (L2ItemInstance item : player.getInventory().getItems()) {
                if (item == null) {
                    continue;
                }

                if (item.notForOly()) {
                    player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
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

        player.setChannel(4);
        player.teleToLocationEvent(loc);

        buffPlayer(player);

        player.setCurrentCp(player.getMaxCp());
        player.setCurrentHp(player.getMaxHp());
        player.setCurrentMp(player.getMaxMp());
        player.setTeam(teamId);
        player.setPVPArena(true);
    }

    public boolean isRegged(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return false;
        }

        return (_teams.get(0).contains(player.getObjectId()) || _teams.get(1).contains(player.getObjectId()));
    }

    public boolean isInBattle(L2PcInstance player) {
        if (player == null) {
            return false;
        }

        if (_state != EventState.BATTLE) {
            return false;
        }

        return (_teams.get(0).contains(player.getObjectId()) || _teams.get(1).contains(player.getObjectId()));
    }

    public static boolean isRegBattle(L2PcInstance player) {
        if (player == null) {
            return false;
        }

        if (_state != EventState.BATTLE) {
            return false;
        }

        return (_teams.get(0).contains(player.getObjectId()) || _teams.get(1).contains(player.getObjectId()));
    }

    public boolean isInSameTeam(L2PcInstance player1, L2PcInstance player2) {
        if (_state != EventState.BATTLE) {
            return false;
        }

        if (player1 == null || player2 == null) {
            return false;
        }

        return (_teams.get(0).contains(player1.getObjectId()) && _teams.get(0).contains(player2.getObjectId()))
                || (_teams.get(1).contains(player1.getObjectId()) && _teams.get(1).contains(player2.getObjectId()));
    }

    public boolean isInTeam1(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return false;
        }

        return _teams.get(0).contains(player.getObjectId());
    }

    public boolean isInTeam2(L2PcInstance player) {
        if (_state == EventState.WAIT) {
            return false;
        }

        return _teams.get(1).contains(player.getObjectId());
    }

    private void announce(String text) {
        Announcements.getInstance().announceToAll(text);
    }

    private static Location getEventLoc(int teamId) {
        switch (teamId) {
            case 0:
                //int rnd1 = (Rnd.get(0, _tpLoc1.size() - 1));
                //System.out.println("##getEventLoc##team: " + teamId + "##random: " + rnd1 + "##coord: " + _tpLoc1.get(rnd1) + "##");
                return _tpLoc1.get((Rnd.get(0, _tpLoc1.size() - 1)));
            case 1:
                //int rnd2 = (Rnd.get(0, _tpLoc2.size() - 1));
                //System.out.println("##getEventLoc##team: " + teamId + "##random: " + rnd2 + "##coord: " + _tpLoc2.get(rnd2) + "##");
                return _tpLoc2.get((Rnd.get(0, _tpLoc2.size() - 1)));
        }
        return null;
    }
}
