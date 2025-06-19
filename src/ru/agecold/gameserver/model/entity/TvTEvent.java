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

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastMap;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import javolution.util.FastList;

import ru.agecold.Config;
import ru.agecold.Config.EventReward;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.DoorTable;
import ru.agecold.gameserver.datatables.ItemTable;
import ru.agecold.gameserver.datatables.NpcTable;
import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.instancemanager.EventManager;
import ru.agecold.gameserver.instancemanager.GrandBossManager;
import ru.agecold.gameserver.model.*;
import ru.agecold.gameserver.model.actor.instance.L2DoorInstance;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.olympiad.Olympiad;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.*;
import ru.agecold.gameserver.taskmanager.EffectTaskManager;
import ru.agecold.util.Location;
import ru.agecold.util.log.AbstractLogger;
import ru.agecold.util.Rnd;
import scripts.autoevents.basecapture.BaseCapture;
import scripts.autoevents.lasthero.LastHero;
import scripts.autoevents.masspvp.massPvp;

/**
 * @author FBIagent
 */
public class TvTEvent {

    private static final Logger _log = AbstractLogger.getLogger(TvTEvent.class.getName());

    enum EventState {

        INACTIVE,
        INACTIVATING,
        PARTICIPATING,
        STARTING,
        STARTED,
        REWARDING
    }
    /**
     * The teams of the TvTEvent<br>
     */
    private static final TvTEventTeam[] _teams = new TvTEventTeam[2]; // event only allow max 2 teams
    /**
     * The state of the TvTEvent<br>
     */
    private static EventState _state = EventState.INACTIVE;
    /**
     * The spawn of the participation npc<br>
     */
    private static final FastList<L2Spawn> _npcSpawns = new FastList<>();
    private static final FastList<Location> _npcLocs = new FastList<>();
    private static int _npcLocsSize = 0;
    private static int _returnLocsSize = 0;
    private static final ConcurrentLinkedQueue<String> _ips = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<String> _hwids = new ConcurrentLinkedQueue<>();
    private static final Map<Integer, Integer> _kills = new ConcurrentHashMap<>();
    //
    private static final boolean STORE_PLAYERS = true;
    private static final boolean TELE_FIX = true;
    private static final ConcurrentLinkedQueue<Integer> _players = new ConcurrentLinkedQueue<>();
    //
    private static final Map<String, ConcurrentLinkedQueue<Integer>> _playerTeams = new ConcurrentHashMap<>();
    //
    private static final Map<Integer, FastList<Integer>> _tvtItems = new ConcurrentHashMap<>();
    //
    private static final Map<Integer, FastList<Integer>> _storedItems = new ConcurrentHashMap<>();
    //
    private static final Map<Integer, FastList<StoredBuff>> _storedBuffs = new ConcurrentHashMap<>();
    //
    private static long _endTime;
    //
    private static final Map<Integer, Location> _storedLocs = new ConcurrentHashMap<Integer, Location>();
    //
    private static final ConcurrentLinkedQueue<Integer> _forKick = new ConcurrentLinkedQueue<Integer>();
    //
    private static ScheduledFuture<?> _scoreTask;

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

    /**
     * No instance of this class!<br>
     */
    private TvTEvent() {
    }

    /**
     * Teams initializing<br>
     */
    public static void init() {
        _endTime = 0;
        _teams[0] = new TvTEventTeam(Config.TVT_EVENT_TEAM_1_NAME, Config.TVT_EVENT_TEAM_1_COORDINATES);
        _teams[1] = new TvTEventTeam(Config.TVT_EVENT_TEAM_2_NAME, Config.TVT_EVENT_TEAM_2_COORDINATES);

        _playerTeams.put(_teams[0].getName(), new ConcurrentLinkedQueue<>());
        _playerTeams.put(_teams[1].getName(), new ConcurrentLinkedQueue<>());

        _npcLocs.addAll(Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES);
        _npcLocsSize = _npcLocs.size() - 1;
        _returnLocsSize = Config.TVT_RETURN_COORDINATES.size() - 1;
        if (Config.TVT_CUSTOM_ITEMS) {
            loadItemSettings();
        }
    }

    private static void loadItemSettings() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/tvt_items.xml");
            if (!file.exists()) {
                _log.config("TvTEvent [ERROR]: data/tvt_items.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            //FastList<String> strings = new FastList<String>();
            //ItemTable it = ItemTable.getInstance();
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("class".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            FastList<Integer> list = new FastList<Integer>();
                            int classId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("equipment".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    String[] items = attrs.getNamedItem("items").getNodeValue().split(",");
                                    for (String item : items) {
                                        if (item.equalsIgnoreCase("")) {
                                            continue;
                                        }

                                        //int item_id = Integer.parseInt(item);
                                        list.add(Integer.parseInt(item));
                                        /*if (Config.TVT_CUSTOM_ENCHANT > 0) {
                                         it.getTemplate(item_id).setMaxEnchant(0);
                                         }*/
                                    }
                                }
                            }
                            _tvtItems.put(classId, list);
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("TvTEvent [ERROR]: loadItemSettings() " + e.toString());
        }
    }

    /**
     * Starts the participation of the TvTEvent<br>
     * 1. Get L2NpcTemplate by Config.TVT_EVENT_PARTICIPATION_NPC_ID<br>
     * 2. Try to spawn a new npc of it<br><br>
     *
     * @return boolean<br>
     */
    public static boolean startParticipation() {
        if (NpcTable.getInstance().getTemplate(Config.TVT_EVENT_PARTICIPATION_NPC_ID) == null) {
            _log.warning("TvTEventEngine[TvTEvent.startParticipation()]: L2NpcTemplate is a NullPointer -> Invalid npc id in configs?");
            return false;
        }

        L2Spawn spawn = null;
        L2NpcInstance npc = null;
        GrandBossManager gb = GrandBossManager.getInstance();
        for (FastList.Node<Location> n = _npcLocs.head(), end = _npcLocs.tail(); (n = n.getNext()) != end;) {
            Location loc = n.getValue();
            if (loc == null) {
                continue;
            }

            spawn = gb.createOneSpawnEx(Config.TVT_EVENT_PARTICIPATION_NPC_ID, loc.x, loc.y, loc.z, 30000, false);
            _npcSpawns.add(spawn);
            npc = spawn.spawnOne();
            npc.setTitle("TvT Event Participation");
            npc.decayMe();
            npc.spawnMe(loc.x, loc.y, loc.z);
            npc.broadcastPacket(new MagicSkillUser(npc, npc, 1034, 1, 1, 1));
        }
        npc = null;
        spawn = null;

        if (Config.EVENT_REG_POPUP) {
            for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
                if (player == null || player.getLevel() < Config.EVENT_REG_POPUPLVL) {
                    continue;
                }

                if (player.isShowPopup()) {
                    player.sendPacket(new ConfirmDlg(614, "������� ������� � ������ -TvT-?", 108));
                }
            }
        }

        setState(EventState.PARTICIPATING);
        return true;
    }

    /**
     * Starts the TvTEvent fight<br>
     * 1. Set state EventState.STARTING<br>
     * 2. Close doors specified in configs<br>
     * 3. Abort if not enought participants(return false)<br>
     * 4. Set state EventState.STARTED<br>
     * 5. Teleport all participants to team spot<br><br>
     *
     * @return boolean<br>
     */
    public static boolean startFight() {
        setState(EventState.STARTING);

        // not enought participants
        if (_teams[0].getParticipatedPlayerCount() < Config.TVT_EVENT_MIN_PLAYERS_IN_TEAMS || _teams[1].getParticipatedPlayerCount() < Config.TVT_EVENT_MIN_PLAYERS_IN_TEAMS) {
            _endTime = 0;
            setState(EventState.INACTIVE);
            _teams[0].cleanMe();
            _teams[1].cleanMe();
            _ips.clear();
            _hwids.clear();
            _kills.clear();
            _players.clear();
            _storedItems.clear();
            _storedBuffs.clear();
            _forKick.clear();
            _storedLocs.clear();
            _playerTeams.put(_teams[0].getName(), new ConcurrentLinkedQueue<Integer>());
            _playerTeams.put(_teams[1].getName(), new ConcurrentLinkedQueue<Integer>());
            unSpawnNpc();
            return false;
        }

        closeDoors();
        setState(EventState.STARTED); // set state to STARTED here, so TvTEventTeleporter know to teleport to team spot

        // teleport all participants to there team spot
        L2PcInstance player = null;
        for (ConcurrentLinkedQueue<Integer> team : _playerTeams.values()) {
            if (team == null) {
                continue;
            }
            for (Integer player_id : team) {
                if (player_id == null) {
                    continue;
                }

                try {
                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        //removeParticipant(playerName);
                        continue;
                    }

                    if (player.getPet() != null)
                    {
                        L2Summon summon = player.getPet();
                        if (summon.isPet()) {
                            summon.unSummon(player);
                        } else {
                            summon.stopAllEffects();
                        }
                    }

                    if (Config.TVT_SAVE_BUFFS)
                    {
                        FastList<StoredBuff> sb = new FastList<StoredBuff>();
                        for (L2Effect e : player.getAllEffects()) {
                            if (e == null) {
                                continue;
                            }
                            if (!e.getInUse()) {
                                continue;
                            }
                            if (e.getSkill().isToggle()) {
                                continue;
                            }
                            if (e.getSkill().isAugment()) {
                                continue;
                            }

                            sb.add(new StoredBuff(e.getId(), e.getLevel(), e.getCount(), e.getTime()));
                        }
                        _storedBuffs.put(player_id, sb);
                    }
                    if (Config.TVT_NO_DEADS && player.isDead())
                    {
                        player.restoreExp(100.0);
                        player.doRevive();
                    }

                    if (player.isInParty()) {
                        player.getParty().oustPartyMember(player);
                    }

                    player.setChannel(8);
                    player.setIsParalyzed(false);
                } catch (Exception e) {
                    //removeParticipant(playerName);
                }
            }
        }
        player = null;

        for (TvTEventTeam eteam : _teams) {
            if (eteam == null) {
                continue;
            }

            ConcurrentLinkedQueue<Integer> team = _playerTeams.get(eteam.getName());
            for (Integer player_id : team) {
                if (player_id == null) {
                    continue;
                }

                try {
                    player = L2World.getInstance().getPlayer(player_id);
                    if (player == null) {
                        continue;
                    }

                    player.setEventChannel(8);

                    if (Config.TVT_CUSTOM_ITEMS) {
                        equipPlayer(player, _tvtItems.get(player.getClassId().getId()));
                    }

                    for (L2Skill s : player.getAllSkills()) {
                        if (s == null) {
                            continue;
                        }
                        if (s.isForbidEvent()) {
                            player.removeStatsOwner(s);
                        }
                    }

                    if (player.isInParty()) {
                        player.getParty().oustPartyMember(player);
                    }

                    // implements Runnable and starts itself in constructor
                    new TvTEventTeleporter(player, eteam.getCoordinates(), false, false);
                } catch (Exception e) {
                    //removeParticipant(playerName);
                }
            }
        }

        ThreadPoolManager.getInstance().scheduleGeneral(new RefreshPlayers(), 3000);
        ThreadPoolManager.getInstance().scheduleGeneral(new CheckZone(), 30000);

        if (Config.TVT_KILLS_OVERLAY) {
            ThreadPoolManager.getInstance().scheduleGeneral(new KillsOverlay(), 5000);
        }
        if (Config.TVT_AFK_CHECK) {
            ThreadPoolManager.getInstance().scheduleGeneral(new CheckAFK(), Config.TVT_AFK_CHECK_DELAY);
        }
        if (Config.TVT_CAHT_SCORE > 0) {
            _scoreTask = EffectTaskManager.getInstance().scheduleAtFixedRate(new ScoreTask(), 1000, Config.TVT_CAHT_SCORE);
        }
        _endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Config.TVT_EVENT_RUNNING_TIME);
        return true;
    }

    static class RefreshPlayers implements Runnable {

        RefreshPlayers() {
        }

        public void run() {
            L2PcInstance player = null;
            for (Integer char_id : _players) {
                if (char_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(char_id);
                if (player == null) {
                    continue;
                }

                player.setTvtPassive(true);
                _kills.put(char_id, 0);
                _teams[0].refreshPoints();
                _teams[1].refreshPoints();
            }
        }
    }

    static class ScoreTask implements Runnable {

        ScoreTask() {
        }

        public void run() {
            String rsp = _teams[0].getName() +" : " + String.valueOf(_teams[0].getPoints()) + " | " + _teams[1].getName() + " : " +String.valueOf(_teams[1].getPoints()) + "";

            for (Integer char_id : _players) {
                if (char_id == null) {
                    continue;
                }

                L2PcInstance player = L2World.getInstance().getPlayer(char_id);
                if (player == null) {
                    continue;
                }

                player.sendCritMessage(rsp);
            }
        }
    }

    static class CheckZone implements Runnable {

        CheckZone() {
        }

        public void run() {
            L2PcInstance player = null;
            for (Integer char_id : _players) {
                if (char_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(char_id);
                if (player == null) {
                    continue;
                }

                if (Config.TVT_POLY.contains(player.getX(), player.getY(), player.getZ())) {
                    continue;
                }

                removeParticipant(player.getName());
                player.setEventChannel(1);
                player.setTvtPassive(true);
                player.teleToClosestTown();
            }
        }
    }

    /**
     * Calculates the TvTEvent reward<br>
     * 1. If both teams are at a tie(points equals), send it as system message
     * to all participants, if one of the teams have 0 participants left online
     * abort rewarding<br>
     * 2. Wait till teams are not at a tie anymore<br>
     * 3. Set state EvcentState.REWARDING<br>
     * 4. Reward team with more points<br>
     * 5. Show win html to wining team participants<br><br>
     *
     * @return String<br>
     */
    //@Test
    public static String calculateRewards() {
        if (_teams[0].getPoints() == _teams[1].getPoints()) {
            if (_teams[0].getParticipatedPlayerCount() == 0 || _teams[1].getParticipatedPlayerCount() == 0) {
                // the fight cannot be completed
                setState(EventState.REWARDING);
                _hwids_rewards.clear();
                return Static.TVT_FINISHED_INACTIVE;
            }

            sysMsgToAllParticipants(Static.TVT_TEAMS_IN_TIE);

            // the fight cannot be completed
            setState(EventState.REWARDING);

            for (TvTEventTeam team : _teams) {
                if (team == null) {
                    continue;
                }
                ConcurrentLinkedQueue<Integer> winners = _playerTeams.get(team.getName());
                for (Integer char_id : winners) {
                    if (char_id == null) {
                        continue;
                    }

                    giveRewardTie(L2World.getInstance().getPlayer(char_id));
                }
            }

            _hwids_rewards.clear();

            return Static.TVT_FINISHED_TIE;
        }

        setState(EventState.REWARDING); // after state REWARDING is set, nobody can point anymore

        byte teamId = (byte) (_teams[0].getPoints() > _teams[1].getPoints() ? 0 : 1); // which team wins?
        TvTEventTeam team = _teams[teamId];

        FastList<Integer> rewarded_top = new FastList<>();
        if (Config.TVT_REWARD_TOP) {
            Announcements.getInstance().announceToAll("TvT: ��� " + Config.TVT_REWARD_TOP_COUNT + " �� ���������:");
            ValueComparator vc = new ValueComparator(_kills);
            TreeMap<Integer, Integer> sortedKills = new TreeMap<Integer, Integer>(vc);
            sortedKills.putAll(_kills);
            int i = 1;
            for (Map.Entry<Integer, Integer> entry : sortedKills.entrySet()) {
                Integer id = entry.getKey();
                Integer count = entry.getValue();
                if (id == null || count == null) {
                    continue;
                }

                L2PcInstance player = L2World.getInstance().getPlayer(id);
                if (player == null) {
                    continue;
                }
                try {
                    for (int[] reward : player.isPremium() ? Config.TVT_EVENT_REWARDS_TOP_PREMIUM : Config.TVT_EVENT_REWARDS_TOP) {
                        player.addItem("TvtTop " + Config.TVT_REWARD_TOP_COUNT + " Reward", reward[0], reward[1], player, true);
                    }
                } catch (Exception e) {
                    //
                }
                Announcements.getInstance().announceToAll("To�" + i + ": " + player.getName() + "!");
                i++;
                rewarded_top.add(id);
                if (i > Config.TVT_REWARD_TOP_COUNT) {
                    break;
                }
            }
        }

        ConcurrentLinkedQueue<Integer> winners = _playerTeams.get(team.getName());
        for (Integer char_id : winners) {
            if (char_id == null) {
                continue;
            }

            if (rewarded_top.contains(char_id)) {
                continue;
            }

            giveReward(L2World.getInstance().getPlayer(char_id));
        }

        if (Config.TVT_REWARD_LOOSER > 0) {
            TvTEventTeam team_looser = _teams[teamId != 1 ? 1 : 0];
            ConcurrentLinkedQueue<Integer> loosers = _playerTeams.get(team_looser.getName());
            for (Integer char_id : loosers) {
                if (char_id == null) {
                    continue;
                }

                if (rewarded_top.contains(char_id)) {
                    continue;
                }

                giveRewardLooser(L2World.getInstance().getPlayer(char_id));
            }
        }

        _hwids_rewards.clear();

        String event_end = Static.TVT_S1_WIN_KILLS_S2.replaceAll("%a%", team.getName());
        return (event_end.replaceAll("%b%", String.valueOf(team.getPoints())));
    }

    private static final ConcurrentLinkedQueue<String> _hwids_rewards = new ConcurrentLinkedQueue<String>();

    private static void giveRewardLooser(L2PcInstance player) {
        if (player == null) {
            return;
        }

        if (Config.TVT_NO_PASSIVE
                && player.isTvtPassive()) {
            return;
        }

        if (Config.TVT_REWARD_CHECK && !player.isEnabledTvtReward()) {
            return;
        }

        if (player.getTvtKills() < Config.TVT_REWARD_LOOSER) {
            return;
        }

        if (!Config.EVENTS_SAME_HWID) {
            if (_hwids_rewards.contains(player.getHWID())) {
                player.sendHtmlMessage("TvT Event", "� ������ ���������� ��� ������� ����");
                return;
            }
            _hwids_rewards.add(player.getHWID());
        }

        SystemMessage sm = null;
        for (int[] reward : Config.TVT_EVENT_REWARDS_LOOSER) {
            PcInventory inv = player.getInventory();

            if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable()) {
                inv.addItem("TvT Event Loose", reward[0], reward[1], player, player);
            } else {
                for (int i = 0; i < reward[1]; i++) {
                    inv.addItem("TvT Event Loose", reward[0], 1, player, player);
                }
            }

            if (reward[1] > 1) {
                sm = SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(reward[0]).addNumber(reward[1]);
            }
            else {
                sm = SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(reward[0]);
            }

            player.sendPacket(sm);
        }
    }

    private static void giveReward(L2PcInstance player) {
        if (player == null) {
            return;
        }

        if (Config.TVT_NO_PASSIVE
                && player.isTvtPassive()) {
            return;
        }

        if (Config.TVT_REWARD_CHECK && !player.isEnabledTvtReward()) {
            return;
        }

        if (player.getTvtKills() < Config.TVT_MIN_KILLS_FOR_REWARDS) {
            return;
        }

        if (!Config.EVENTS_SAME_HWID) {
            if (_hwids_rewards.contains(player.getHWID())) {
                player.sendHtmlMessage("TvT Event", "� ������ ���������� ��� ������� ����");
                return;
            }
            _hwids_rewards.add(player.getHWID());
        }

        SystemMessage sm = null;
        List<int[]> rewards = player.isPremium() ? Config.TVT_EVENT_REWARDS_PREMIUM : Config.TVT_EVENT_REWARDS;
        for (int[] reward : rewards) {

            PcInventory inv = player.getInventory();

            if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable()) {
                inv.addItem("TvT Event", reward[0], reward[1], player, player);
            } else {
                for (int i = 0; i < reward[1]; i++) {
                    inv.addItem("TvT Event", reward[0], 1, player, player);
                }
            }

            if (reward[1] > 1) {
                sm = SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(reward[0]).addNumber(reward[1]);
            } else {
                sm = SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(reward[0]);
            }
            player.sendPacket(sm);
            sm = null;
        }

        /*if (Config.TVT_EVENT_REWARDS_TOP_KILLS > 0 && player.getTvtKills() >= Config.TVT_EVENT_REWARDS_TOP_KILLS) {
            for (FastList.Node<EventReward> k = Config.TVT_PVP_REWARDS.head(), endk = Config.TVT_PVP_REWARDS.tail(); (k = k.getNext()) != endk;) {
                EventReward reward = k.getValue();
                if (reward == null) {
                    continue;
                }

                if (Rnd.get(100) < reward.chance) {
                    player.addItem("giveItem", reward.id, reward.count, player, true);
                }
            }
        }*/

        StatusUpdate statusUpdate = new StatusUpdate(player.getObjectId());

        statusUpdate.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
        player.sendPacket(statusUpdate);

        NpcHtmlMessage npcHtmlMessage = NpcHtmlMessage.id(0);

        npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>������! ��������� � ���������, �������� �������.</body></html>");
        player.sendPacket(npcHtmlMessage);
        player.setTvtPassive(true);
    }

    private static void giveRewardTie(L2PcInstance player) {
        if (player == null) {
            return;
        }

        if (Config.TVT_NO_PASSIVE
                && player.isTvtPassive()) {
            return;
        }

        if (Config.TVT_REWARD_CHECK && !player.isEnabledTvtReward()) {
            return;
        }

        if (player.getTvtKills() < Config.TVT_MIN_KILLS_TIE_FOR_REWARDS) {
            return;
        }

        if (!Config.EVENTS_SAME_HWID) {
            if (_hwids_rewards.contains(player.getHWID())) {
                player.sendHtmlMessage("TvT Event", "� ������ ���������� ��� ������� ����");
                return;
            }
            _hwids_rewards.add(player.getHWID());
        }

        SystemMessage sm = null;
        List<int[]> rewards = Config.TVT_EVENT_REWARDS_TIE;
        for (int[] reward : rewards) {

            PcInventory inv = player.getInventory();

            if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable()) {
                inv.addItem("TvT Event Tie", reward[0], reward[1], player, player);
            } else {
                for (int i = 0; i < reward[1]; i++) {
                    inv.addItem("TvT Event Tie", reward[0], 1, player, player);
                }
            }

            if (reward[1] > 1) {
                sm = SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(reward[0]).addNumber(reward[1]);
            } else {
                sm = SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(reward[0]);
            }
            player.sendPacket(sm);
            sm = null;
        }

        /*if (Config.TVT_EVENT_REWARDS_TOP_KILLS > 0 && player.getTvtKills() >= Config.TVT_EVENT_REWARDS_TOP_KILLS) {
            for (FastList.Node<EventReward> k = Config.TVT_PVP_REWARDS.head(), endk = Config.TVT_PVP_REWARDS.tail(); (k = k.getNext()) != endk;) {
                EventReward reward = k.getValue();
                if (reward == null) {
                    continue;
                }

                if (Rnd.get(100) < reward.chance) {
                    player.addItem("giveItem", reward.id, reward.count, player, true);
                }
            }
        }*/

        StatusUpdate statusUpdate = new StatusUpdate(player.getObjectId());

        statusUpdate.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
        player.sendPacket(statusUpdate);

        NpcHtmlMessage npcHtmlMessage = NpcHtmlMessage.id(0);

        npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>������� ������� ������! ��������� � ���������, �������� ������� �� �������. </body></html>");
        player.sendPacket(npcHtmlMessage);
        player.setTvtPassive(true);
    }

    static class ValueComparator implements Comparator<Integer> {

        Map<Integer, Integer> base;

        public ValueComparator(Map<Integer, Integer> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.
        @Override
        public int compare(Integer a, Integer b) {
            if (base.get(a) >= base.get(b)) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }

    /**
     * Stops the TvTEvent fight<br>
     * 1. Set state EventState.INACTIVATING<br>
     * 2. Remove tvt npc from world<br>
     * 3. Open doors specified in configs<br>
     * 4. Teleport all participants back to participation npc location<br>
     * 5. Teams cleaning<br>
     * 6. Set state EventState.INACTIVE<br>
     */
    public static void stopFight() {
        setState(EventState.INACTIVATING);
        unSpawnNpc();
        openDoors();

        L2PcInstance player = null;
        if (STORE_PLAYERS) {
            for (Integer char_id : _players) {
                if (char_id == null) {
                    continue;
                }

                player = L2World.getInstance().getPlayer(char_id);
                if (player == null) {
                    continue;
                }

                if (Config.TVT_CUSTOM_ITEMS) {
                    restoreItems(player);
                }
                for (L2Skill s : player.getAllSkills()) {
                    if (s == null) {
                        continue;
                    }

                    if (s.isForbidEvent()) {
                        player.addStatFuncs(s.getStatFuncs(null, player));
                    }
                }
                // ������ ������
                new TvTEventTeleporter(player, getRandomRetrunLoc(), false, false, _storedBuffs.get(char_id));
            }
        } else {
            for (TvTEventTeam team : _teams) {
                for (String playerName : team.getParticipatedPlayerNames()) {
                    player = team.getParticipatedPlayers().get(playerName);
                    if (player == null) {
                        continue;
                    }

                    new TvTEventTeleporter(player, getRandomRetrunLoc(), false, false);
                }
            }
        }

        _endTime = 0;
        if (_scoreTask != null) {
            _scoreTask.cancel(false);
            _scoreTask = null;
        }
        _teams[0].cleanMe();
        _teams[1].cleanMe();
        _ips.clear();
        _hwids.clear();
        _kills.clear();
        _players.clear();
        //_playersActive.clear();
        _storedItems.clear();
        _storedBuffs.clear();
        _forKick.clear();
        _storedLocs.clear();
        _playerTeams.put(_teams[0].getName(), new ConcurrentLinkedQueue<Integer>());
        _playerTeams.put(_teams[1].getName(), new ConcurrentLinkedQueue<Integer>());
        setState(EventState.INACTIVE);
    }

    public static Location getRandomLoc() {
        return _npcLocs.get(Rnd.get(_npcLocsSize));
    }

    public static Location getRandomRetrunLoc() {
       return Config.TVT_RETURN_COORDINATES.get(Rnd.get(_returnLocsSize));
    }

    /**
     * Adds a player to a TvTEvent team<br>
     * 1. Calculate the id of the team in which the player should be added<br>
     * 2. Add the player to the calculated team<br><br>
     *
     * @param player<br>
     * @return boolean<br>
     */
    public static synchronized boolean addParticipant(L2PcInstance player) {
        if (player == null) {
            return false;
        }

        byte teamId = 0;

        if (_teams[0].getParticipatedPlayerCount() == _teams[1].getParticipatedPlayerCount()) {
            teamId = (byte) (Rnd.get(2));
        } else if (_teams[0].getParticipatedPlayerCount() > _teams[1].getParticipatedPlayerCount()) {
            teamId = (byte) (1);
        } else {
            teamId = (byte) (0);
        }
        if (!Config.EVENTS_SAME_IP) {
            _ips.add(player.getIP());
        }

        if (!Config.EVENTS_SAME_HWID) {
            _hwids.add(player.getHWID());
        }

        _kills.put(player.getObjectId(), 0);
        _players.add(player.getObjectId());

        _playerTeams.get(_teams[teamId].getName()).add(player.getObjectId());

        return _teams[teamId].addPlayer(player);
    }

    /**
     * Removes a TvTEvent player from it's team<br>
     * 1. Get team id of the player<br>
     * 2. Remove player from it's team<br><br>
     *
     * @param playerName<br>
     * @return boolean<br>
     */
    public static boolean removeParticipant(String playerName) {
        byte teamId = getParticipantTeamId(playerName);

        if (teamId == -1) {
            return false;
        }

        _teams[teamId].removePlayer(playerName);
        //
        if (!Config.EVENTS_SAME_IP
                || !Config.EVENTS_SAME_HWID
                || STORE_PLAYERS) {
            L2PcInstance player = L2World.getInstance().getPlayer(playerName);
            if (player != null) {
                if (!Config.EVENTS_SAME_IP) {
                    _ips.remove(player.getIP());
                }
                if (!Config.EVENTS_SAME_HWID) {
                    _hwids.remove(player.getHWID());
                }
                _kills.remove(player.getObjectId());
                _players.remove(player.getObjectId());

                _playerTeams.get(_teams[teamId].getName()).remove(player.getObjectId());
            }

        }
        return true;
    }

    /**
     * Send a SystemMessage to all participated players<br>
     * 1. Send the message to all players of team number one<br>
     * 2. Send the message to all players of team number two<br><br>
     *
     * @param message<br>
     */
    public static void sysMsgToAllParticipants(String message) {
        for (L2PcInstance playerInstance : _teams[0].getParticipatedPlayers().values()) {
            if (playerInstance != null) {
                playerInstance.sendMessage(message);
            }
        }

        for (L2PcInstance playerInstance : _teams[1].getParticipatedPlayers().values()) {
            if (playerInstance != null) {
                playerInstance.sendMessage(message);
            }
        }
    }

    public static void spMsgToAllParticipants(String message) {
        CreatureSay cs = new CreatureSay(0, 18, " ", message);

        for (L2PcInstance playerInstance : _teams[0].getParticipatedPlayers().values()) {
            if (playerInstance != null) {
                playerInstance.sendPacket(cs);
            }
        }

        for (L2PcInstance playerInstance : _teams[1].getParticipatedPlayers().values()) {
            if (playerInstance != null) {
                playerInstance.sendPacket(cs);
            }
        }
    }

    /**
     * Close doors specified in configs
     */
    private static void closeDoors() {
        for (int doorId : Config.TVT_EVENT_DOOR_IDS) {
            L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(doorId);

            if (doorInstance != null) {
                doorInstance.closeMe();
            }
        }
    }

    /**
     * Open doors specified in configs
     */
    private static void openDoors() {
        for (int doorId : Config.TVT_EVENT_DOOR_IDS) {
            L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(doorId);

            if (doorInstance != null) {
                doorInstance.openMe();
            }
        }
    }

    /**
     * UnSpawns the TvTEvent npc
     */
    private static void unSpawnNpc() {
        L2Spawn spawn = null;
        for (FastList.Node<L2Spawn> n = _npcSpawns.head(), end = _npcSpawns.tail(); (n = n.getNext()) != end;) {
            spawn = n.getValue(); // No typecast necessary.
            if (spawn == null || spawn.getLastSpawn() == null) {
                continue;
            }

            spawn.getLastSpawn().deleteMe();
        }
        spawn = null;
        _npcSpawns.clear();
    }

    /**
     * Called when a player logs in<br><br>
     *
     * @param playerInstance<br>
     */
    /*public static void onLogin(L2PcInstance playerInstance)
     {
     if (playerInstance == null || (!isStarting() && !isStarted()))
     return;
    
     byte teamId = getParticipantTeamId(playerInstance.getName());
    
     if (teamId == -1)
     return;
    
     _teams[teamId].addPlayer(playerInstance);
     new TvTEventTeleporter(playerInstance, _teams[teamId].getCoordinates(), true, false);
     }*/
    /**
     * Called on every bypass by npc of type L2TvTEventNpc<br>
     * Needs synchronization cause of the max player check<br><br>
     *
     * @param command<br>
     * @param player<br>
     */
    public static synchronized void onBypass(String command, L2PcInstance player) {
        if (player == null || !isParticipating()) {
            return;
        }

        if (command.equals("tvt_event_participation")) {
            if (!Config.TVT_EVENT_ENABLED) {
                player.sendHtmlMessage("����� -���- ��������.");
                return;
            }
            if (Config.TVT_NOBL && !player.isNoble()) {
                player.sendHtmlMessage("������ �������� ����� ������������.");
                return;
            }
            if (Config.MASS_PVP && massPvp.getEvent().isReg(player)) {
                player.sendHtmlMessage("����� �� ������ -���� ���-.");
                return;
            }
            if (Config.ELH_ENABLE && LastHero.getEvent().isRegged(player)) {
                player.sendHtmlMessage("�� ��� ���������������� � ������ -��������� �����-.");
                return;
            }
            if (Config.EBC_ENABLE && BaseCapture.getEvent().isRegged(player)) {
                player.sendHtmlMessage("����� �� ������ -������ ����-");
                return;
            }
            if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode()) {
                player.sendHtmlMessage("�� ��� ���������������� �� ���������.");
                return;
            }

            if (!TvTEvent.isParticipating()) {
                player.sendHtmlMessage("����������� � ������ ������ �� �������.");
                return;
            }

            if (TvTEvent.isPlayerParticipant(player.getName())) {
                player.sendHtmlMessage("�� ��� ����������������.");
                return;
            }
            NpcHtmlMessage npcHtmlMessage = NpcHtmlMessage.id(0);
            int playerLevel = player.getLevel();

            if (player.isCursedWeaponEquiped()) {
                npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>��������� � ��������� ������� �� ����� ������������.</body></html>");
            } else if (player.getKarma() > 0) {
                npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>� ��� ������ �����.</body></html>");
            } else if (Config.TVT_NOBL && !player.isNoble()) {
                npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>������ �������� ����� ��������� �������.</body></html>");
            } else if (_teams[0].getParticipatedPlayerCount() >= Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS && _teams[1].getParticipatedPlayerCount() >= Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS) {
                npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>���� ���!</body></html>");
            } else if (playerLevel < Config.TVT_EVENT_MIN_LVL || playerLevel > Config.TVT_EVENT_MAX_LVL) {
                npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>������ ��������� � " + Config.TVT_EVENT_MIN_LVL + " ������ �� " + Config.TVT_EVENT_MAX_LVL + " ����� ������������.</body></html>");
            } else if (_teams[0].getParticipatedPlayerCount() > Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS && _teams[1].getParticipatedPlayerCount() > Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS) {
                npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>���� ���! �������� " + Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS + "  ���������� � ������ �������.</body></html>");
            } else if (!Config.EVENTS_SAME_IP && _ips.contains(player.getIP())) {
                npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>���-�� ��� ���������� � ������ IP.</body></html>");
            } else if (!Config.EVENTS_SAME_HWID && _hwids.contains(player.getHWID())) {
                npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>���-�� ��� ���������� � ������ IP.</body></html>");
            } else if (addParticipant(player)) {
                npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>�� ������������������.</body></html>");
            } else // addParticipant returned false cause player == null
            {
                return;
            }

            player.sendPacket(npcHtmlMessage);
        } else if (command.equals("tvt_event_remove_participation")) {
            removeParticipant(player.getName());

            NpcHtmlMessage npcHtmlMessage = NpcHtmlMessage.id(0);

            npcHtmlMessage.setHtml("<html><head><title>TvT Event</title></head><body>� �� ����, �� � �����?</body></html>");
            player.sendPacket(npcHtmlMessage);
        }
    }

    /**
     * Called on every onAction in L2PcIstance<br><br>
     *
     * @param playerName<br>
     * @param targetPlayerName<br>
     * @return boolean<br>
     */
    public static boolean onAction(String playerName, String targetPlayerName) {
        if (!isStarted()) {
            return true;
        }

        L2PcInstance playerInstance = L2World.getInstance().getPlayer(playerName);

        if (playerInstance == null) {
            return false;
        }

        if (playerInstance.isGM()) {
            return true;
        }

        byte playerTeamId = getParticipantTeamId(playerName);
        byte targetPlayerTeamId = getParticipantTeamId(targetPlayerName);

        if (playerTeamId != -1 && targetPlayerTeamId == -1) {
            return true;
        }

        if (playerTeamId == -1 && targetPlayerTeamId != -1) {
            return false;
        }

        if (playerTeamId != -1 && targetPlayerTeamId != -1 && playerTeamId == targetPlayerTeamId && !Config.TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED) {
            return false;
        }

        return true;
    }

    /**
     * Called on every potion use<br><br>
     *
     * @param playerName<br>
     * @return boolean<br>
     */
    public static boolean onPotionUse(String playerName, int itemId) {
        if (!isStarted() || !isPlayerParticipant(playerName)) {
            return true;
        }

        if (Config.TVT_WHITE_POTINS.contains(itemId)) {
            return true;
        }

        switch (itemId) {
            case 734:
            case 735:
            case 1062:
            case 1374:
            case 1375:
            case 1539:
            case 6035:
            case 6036:
                return true;
        }

        if (!Config.TVT_EVENT_POTIONS_ALLOWED) {
            return false;
        }

        return true;
    }

    /**
     * Called on every escape use(thanks to nbd)<br><br>
     *
     * @param playerName<br>
     * @return boolean<br>
     */
    public static boolean onEscapeUse(String playerName) {
        if (!isStarted()) {
            return true;
        }

        if (isPlayerParticipant(playerName)) {
            return false;
        }

        return true;
    }

    /**
     * Called on every summon item use<br><br>
     *
     * @param playerName<br>
     * @return boolean<br>
     */
    public static boolean onItemSummon(String playerName) {
        if (!isStarted()) {
            return true;
        }

        if (isPlayerParticipant(playerName) && !Config.TVT_EVENT_SUMMON_BY_ITEM_ALLOWED) {
            return false;
        }

        return true;
    }

    /**
     * Is called when a player is killed<br><br>
     *
     * @param killer<br>
     * @param killedPlayerInstance<br>
     */
    public static void onKill(L2Character killer, L2PcInstance killedPlayerInstance) {
        if (killer == null || killedPlayerInstance == null
                || (!(killer.isPlayer()) && !(killer.isPet()) && !(killer.isSummon()))
                || !isStarted()) {
            return;
        }

        L2PcInstance pcKiller = killer.getPlayer();
        if (pcKiller == null) {
            return;
        }

        pcKiller.setTvtPassive(false);

        String playerName = pcKiller.getName();
        byte killerTeamId = getParticipantTeamId(playerName);

        playerName = killedPlayerInstance.getName();

        byte killedTeamId = getParticipantTeamId(playerName);

        if (killerTeamId != -1 && killedTeamId != -1 && killerTeamId != killedTeamId) {
            _teams[killerTeamId].increasePoints();

            updateKillsForPlayer(pcKiller.getObjectId());

            if (Config.TVT_KILL_REWARD) {
                EventManager.getInstance().giveEventKillReward(killer.getPlayer(), null, Config.TVT_KILLSITEMS);
            }

            if (Config.TVT_KILL_PVP) {
                pcKiller.increasePvpKills();
                pcKiller.broadcastUserInfo();
            }
        }

        if (killedTeamId != -1) {
            new TvTEventTeleporter(killedPlayerInstance, _teams[killedTeamId].getCoordinates(), false, false);
        }
    }

    private static void updateKillsForPlayer(int objId) {
        updateKills(objId, getTvtKillsp(_kills.get(objId)));
    }

    private static void updateKills(int objId, int count) {
        count++;
        _kills.put(objId, count);
    }

    public static int getTvtKills(int objId) {
        return getTvtKillsp(_kills.get(objId));
    }

    private static int getTvtKillsp(Integer count) {
        if (count == null) {
            count = 0;
        }
        return count;
    }

    /**
     * Sets the TvTEvent state<br><br>
     *
     * @param state<br>
     */
    private static void setState(EventState state) {
        synchronized (_state) {
            _state = state;
        }
    }

    /**
     * Is TvTEvent inactive?<br><br>
     *
     * @return boolean<br>
     */
    public static boolean isInactive() {
        boolean isInactive;

        synchronized (_state) {
            isInactive = _state == EventState.INACTIVE;
        }

        return isInactive;
    }

    /**
     * Is TvTEvent in inactivating?<br><br>
     *
     * @return boolean<br>
     */
    public static boolean isInactivating() {
        boolean isInactivating;

        synchronized (_state) {
            isInactivating = _state == EventState.INACTIVATING;
        }

        return isInactivating;
    }

    /**
     * Is TvTEvent in participation?<br><br>
     *
     * @return boolean<br>
     */
    public static boolean isParticipating() {
        boolean isParticipating;

        synchronized (_state) {
            isParticipating = _state == EventState.PARTICIPATING;
        }

        return isParticipating;
    }

    /**
     * Is TvTEvent starting?<br><br>
     *
     * @return boolean<br>
     */
    public static boolean isStarting() {
        boolean isStarting;

        synchronized (_state) {
            isStarting = _state == EventState.STARTING;
        }

        return isStarting;
    }

    /**
     * Is TvTEvent started?<br><br>
     *
     * @return boolean<br>
     */
    public static boolean isStarted() {
        boolean isStarted;

        synchronized (_state) {
            isStarted = _state == EventState.STARTED;
        }

        return isStarted;
    }

    /**
     * Is TvTEvent rewadrding?<br><br>
     *
     * @return boolean<br>
     */
    public static boolean isRewarding() {
        boolean isRewarding;

        synchronized (_state) {
            isRewarding = _state == EventState.REWARDING;
        }

        return isRewarding;
    }

    /**
     * Returns the team id of a player, if player is not participant it returns
     * -1<br><br>
     *
     * @param playerName<br>
     * @return byte<br>
     */
    public static byte getParticipantTeamId(String playerName) {
        return (byte) (_teams[0].containsPlayer(playerName) ? 0 : (_teams[1].containsPlayer(playerName) ? 1 : -1));
    }

    /**
     * Returns the team coordinates in which the player is in, if player is not
     * in a team return null<br><br>
     *
     * @param playerName<br>
     * @return int[]<br>
     */
    public static Location getParticipantTeamCoordinates(String playerName) {
        return _teams[0].containsPlayer(playerName) ? _teams[0].getCoordinates() : (_teams[1].containsPlayer(playerName) ? _teams[1].getCoordinates() : null);
    }

    /**
     * Is given player participant of the event?<br><br>
     *
     * @param playerName<br>
     * @return boolean<br>
     */
    public static boolean isPlayerParticipant(String playerName) {
        if (_players.isEmpty()) {
            return false;
        }
        return _teams[0].containsPlayer(playerName) || _teams[1].containsPlayer(playerName);
    }

    /**
     * Returns participated player count<br><br>
     *
     * @return int<br>
     */
    public static int getParticipatedPlayersCount() {
        return _teams[0].getParticipatedPlayerCount() + _teams[1].getParticipatedPlayerCount();
    }

    /**
     * Returns teams names<br><br>
     *
     * @return String[]<br>
     */
    public static String[] getTeamNames() {
        return new String[]{_teams[0].getName(), _teams[1].getName()};
    }

    /**
     * Returns player count of both teams<br><br>
     *
     * @return int[]<br>
     */
    public static int[] getTeamsPlayerCounts() {
        return new int[]{_teams[0].getParticipatedPlayerCount(), _teams[1].getParticipatedPlayerCount()};
    }

    /**
     * Returns points count of both teams
     *
     * @return int[]
     */
    public static int[] getTeamsPoints() {
        return new int[]{_teams[0].getPoints(), _teams[1].getPoints()};
    }

    /**
     * Called when a player logs out<br><br>
     *
     * @param player<br>
     */
    public static void onLogout(L2PcInstance player) {
        if (player == null || !Config.TVT_EVENT_ENABLED/* || (!isStarting() && !isStarted())*/) {
            return;
        }

        if (_state != EventState.STARTED) {
            return;
        }

        byte teamId = getParticipantTeamId(player.getName());
        if (teamId == -1) {
            return;
        }

        TvTEventTeam team = _teams[teamId];
        player.teleToLocation(team.getCoordinates());
    }

    public static void onLogin(L2PcInstance player) {
        if (player == null) {
            return;
        }
        if (_state != EventState.STARTED) {
            return;
        }

        byte teamId = getParticipantTeamId(player.getName());
        if (teamId == -1) {
            return;
        }

        TvTEventTeam team = _teams[teamId];
        player.setEventChannel(8);
        player.setTvtPassive(true);

        if (Config.TVT_CUSTOM_ITEMS) {
            equipPlayer(player, _tvtItems.get(player.getClassId().getId()));
        }

        for (L2Skill s : player.getAllSkills()) {
            if (s == null) {
                continue;
            }
            if (s.isForbidEvent()) {
                player.removeStatsOwner(s);
            }
        }

        // implements Runnable and starts itself in constructor
        new TvTEventTeleporter(player, team.getCoordinates(), false, false);
    }

    public static boolean isInSameTeam(int charId, int targId) {
        if (_state != EventState.STARTED) {
            return false;
        }

        return (_playerTeams.get(_teams[0].getName()).contains(charId) && _playerTeams.get(_teams[0].getName()).contains(targId))
                || (_playerTeams.get(_teams[1].getName()).contains(charId) && _playerTeams.get(_teams[1].getName()).contains(targId));
    }

    public static boolean isRegisteredPlayer(int charId) {
        if (_state != EventState.STARTED) {
            return false;
        }

        return _playerTeams.get(_teams[0].getName()).contains(charId) || _playerTeams.get(_teams[1].getName()).contains(charId);
    }

    static class KillsOverlay implements Runnable {
        public void run() {
            if (_state != EventState.STARTED) {
                sendKillsOverlay(true);
                return;
            }
            sendKillsOverlay(false);
            ThreadPoolManager.getInstance().scheduleGeneral(new KillsOverlay(), Config.TVT_EVENT_KILLS_OVERLAY * 1000);
        }
    }

    private static void sendKillsOverlay(boolean end)
    {
        String screenText = _teams[0].getName() + " : " + String.valueOf(_teams[0].getPoints()) + " | " + _teams[1].getName() + " : " +String.valueOf(_teams[1].getPoints()) + "";

        for (Integer char_id : _players) {
            if (char_id == null) {
                continue;
            }

            L2PcInstance player = L2World.getInstance().getPlayer(char_id);
            if (player == null) {
                continue;
            }

            if(end)
                player.sendPacket(new ExShowScreenMessage(screenText, 5000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, false, 1, -1, true));
            else
                player.sendPacket(new ExShowScreenMessage(screenText, 1000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, false, 1, -1, true));
        }
    }

    static class CheckAFK implements Runnable
    {
        public void run()
        {
            if (!Config.TVT_AFK_CHECK || !isStarted()) {
                return;
            }
            NpcHtmlMessage afk_warn = NpcHtmlMessage.id(0);
            afk_warn.setFile("data/html/events/tvt_afk_warning.htm");

            NpcHtmlMessage afk_kick = NpcHtmlMessage.id(0);
            afk_kick.setFile("data/html/events/tvt_afk_kicked.htm");
            for (Integer char_id : _players) {
                if (char_id == null) {
                    continue;
                }

                L2PcInstance player = L2World.getInstance().getPlayer(char_id);
                if (player == null)
                {
                    continue;
                }

                Location loc_stored = _storedLocs.get(char_id);
                if (loc_stored == null) {
                    _storedLocs.put(char_id, player.getLoc());
                } else {
                    Location loc = player.getLoc();
                    if (loc_stored.equals(loc)) {
                        if (!Config.TVT_AFK_CHECK_WARN || _forKick.contains(char_id)) {
                            removeParticipant(player.getName());
                            player.setEventChannel(1);
                            player.setTvtPassive(true);
                            player.teleToClosestTown();
                            player.sendPacket(afk_kick);
                            _forKick.remove(char_id);
                        }
                        else {
                            _forKick.add(char_id);
                            player.sendPacket(afk_warn);
                        }
                    }
                    else {
                        _storedLocs.put(char_id, loc);
                    }
                }
            }
            ThreadPoolManager.getInstance().scheduleGeneral(new CheckAFK(), Config.TVT_AFK_CHECK_DELAY);
        }
    }

    public static void buffPlayer(L2PcInstance player) {
        if (player == null || !Config.ALT_TVT_BUFFS) {
            return;
        }

        FastMap<Integer, Integer> buffs = null;
        if (player.isMageClass()) {
            buffs = Config.TVT_MAGE_BUFFS;
        } else {
            buffs = Config.TVT_FIGHTER_BUFFS;
        }

        player.stopAllEffects();
        Integer id = null;
        Integer lvl = null;
        SkillTable _st = SkillTable.getInstance();
        FastMap.Entry<Integer, Integer> e = buffs.head();
        for (FastMap.Entry<Integer, Integer> end = buffs.tail(); (e = e.getNext()) != end;)
        {
            id = e.getKey();
            lvl = e.getValue();
            if (id == null || lvl == null) {
                continue;
            }

            _st.getInfo(id, lvl).getEffects(player, player);
        }
    }

    private static void equipPlayer(L2PcInstance player, FastList<Integer> items) {
        if (player == null
                || items == null
                || items.isEmpty()) {
            return;
        }
        //6379,512,6380,6381,7577
        //int[] items = {6379, 512, 6380, 6381, 7577, 858, 858, 889, 889, 920};

        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (item.isEquipped()) {
                storeItem(player.getObjectId(), item.getObjectId());
                player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
            }
        }

        L2ItemInstance item = null;
        for (int itemId : items) {
            item = player.getInventory().addItem("TVT", itemId, 1, player, null);
            if (item == null) {
                continue;
            }

            if (Config.TVT_CUSTOM_ENCHANT > 0) {
                item.setEnchantLevel(Config.TVT_CUSTOM_ENCHANT);
            }

            item.setTimeOfUse(1);
            player.getInventory().equipItemAndRecord(item);
        }

        //
        player.sendItems(false);
        player.broadcastUserInfo();
        //
    }

    private static void storeItem(int charId, int itemObjId) {
        putStoredItem(charId, itemObjId, _storedItems.get(charId));
    }

    private static void putStoredItem(int charId, int itemObjId, FastList<Integer> items) {
        if (items == null) {
            items = new FastList<Integer>();
        }
        items.add(itemObjId);
        _storedItems.put(charId, items);
    }

    private static void restoreItems(L2PcInstance player) {
        equipStoredItems(player, _storedItems.get(player.getObjectId()));
    }

    private static void equipStoredItems(L2PcInstance player, FastList<Integer> items) {
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (item.isEquipped()) {
                player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
            }

            if (item.getTimeOfUse() > 0) {
                player.getInventory().destroyItem("TVT", item, player, player);
            }
        }

        if (items == null || items.isEmpty()) {
            return;
        }

        L2ItemInstance item = null;
        for (int itemId : items) {
            item = player.getInventory().getItemByObjectId(itemId);
            if (item == null) {
                continue;
            }

            if (item.getOwnerId() != player.getObjectId()) {
                continue;
            }

            if (item.getLocation() != L2ItemInstance.ItemLocation.INVENTORY || item.getLocation() == L2ItemInstance.ItemLocation.PAPERDOLL) {
                continue;
            }

            player.getInventory().equipItemAndRecord(item);
        }

        //
        player.sendItems(false);
        player.broadcastUserInfo();
        //
    }

    public static void setPeaceZone() {
        L2PcInstance player;
        for (Integer char_id : _players) {
            if (char_id == null) {
                continue;
            }

            player = L2World.getInstance().getPlayer(char_id);
            if (player == null) {
                continue;
            }

            player.setEventWait(true);
        }
    }

    public static void paralyzePlayers() {
        L2PcInstance player;
        for (Integer char_id : _players) {
            if (char_id == null) {
                continue;
            }

            player = L2World.getInstance().getPlayer(char_id);
            if (player == null) {
                continue;
            }

            player.setIsParalyzed(true);
        }
    }

    public static boolean isInBattle2(int playerId) {
        //System.out.println("##isInBattle#1#");
        if (!Config.TVT_EVENT_ENABLED) {
            return false;
        }
        //System.out.println("##isInBattle#2#");

        if (_state != EventState.STARTED) {
            return false;
        }

        //System.out.println("##isInBattle#4#");
        return _players.contains(playerId);
    }

    public static boolean isRegisteredPlayer2(int charId) {
        if (!Config.TVT_EVENT_ENABLED) {
            return false;
        }

        return _players.contains(charId);
    }
}
