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
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.agecold.gameserver.model.quest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.agecold.Config;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.HtmCache;
import ru.agecold.gameserver.datatables.NpcTable;
import ru.agecold.gameserver.instancemanager.QuestManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Party;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2Spawn;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.mysql.Connect;
import ru.agecold.mysql.Close;
import ru.agecold.util.log.AbstractLogger;
import ru.agecold.util.Rnd;

/**
 * @author Luis Arias
 *
 */
public abstract class Quest {

    protected static final Logger _log = AbstractLogger.getLogger(Quest.class.getName());
    /** HashMap containing events from String value of the event */
    private static Map<String, Quest> _allEventsS = new FastMap<String, Quest>();
    /** HashMap containing lists of timers from the name of the timer */
    private static Map<String, FastList<QuestTimer>> _allEventTimers = new FastMap<String, FastList<QuestTimer>>();
    public static String SOUND_ITEMGET = "ItemSound.quest_itemget";
    public static String SOUND_ACCEPT = "ItemSound.quest_accept";
    public static String SOUND_MIDDLE = "ItemSound.quest_middle";
    public static String SOUND_FINISH = "ItemSound.quest_finish";
    public static String SOUND_GIVEUP = "ItemSound.quest_giveup";
    public static String SOUND_TUTORIAL = "ItemSound.quest_tutorial";
    public static String SOUND_JACKPOT = "ItemSound.quest_jackpot";
    public static String SOUND_HORROR2 = "SkillSound5.horror_02";
    public static String SOUND_BEFORE_BATTLE = "Itemsound.quest_before_battle";
    public static String SOUND_FANFARE_MIDDLE = "ItemSound.quest_fanfare_middle";
    public static String SOUND_FANFARE2 = "ItemSound.quest_fanfare_2";
    public static String SOUND_BROKEN_KEY = "ItemSound2.broken_key";
    public static String SOUND_ENCHANT_SUCESS = "ItemSound3.sys_enchant_sucess";
    public static String SOUND_ENCHANT_FAILED = "ItemSound3.sys_enchant_failed";
    public static String SOUND_ED_CHIMES05 = "AmdSound.ed_chimes_05";
    public static String SOUND_ARMOR_WOOD_3 = "ItemSound.armor_wood_3";
    public static String SOUND_ITEM_DROP_EQUIP_ARMOR_CLOTH = "ItemSound.item_drop_equip_armor_cloth";
    private final int _questId;
    private final String _name;
    private final String _prefixPath;	// used only for admin_quest_reload
    private final String _descr;
    private State _initialState;
    private Map<String, State> _states;
    private FastList<Integer> _questItemIds;

    /**
     * Return collection view of the values contains in the allEventS
     * @return Collection<Quest>
     */
    public static Collection<Quest> findAllEvents() {
        return _allEventsS.values();
    }

    /**
     * (Constructor)Add values to class variables and put the quest in HashMaps.
     * @param questId : int pointing out the ID of the quest
     * @param name : String corresponding to the name of the quest
     * @param descr : String for the description of the quest
     */
    public Quest(int questId, String name, String descr) {
        _questId = questId;
        _name = name;
        _descr = descr;
        _states = new FastMap<String, State>();

        // Given the quest instance, create a string representing the path and questName 
        // like a simplified version of a canonical class name.  That is, if a script is in 
        // DATAPACK_PATH/jscript/quests/abc the result will be quests.abc
        // Similarly, for a script in DATAPACK_PATH/jscript/ai/individual/myClass.py
        // the result will be ai.individual.myClass
        // All quests are to be indexed, processed, and reloaded by this form of pathname.
        StringBuffer temp = new StringBuffer(getClass().getCanonicalName());
        temp.delete(0, temp.indexOf(".jscript.") + 9);
        temp.delete(temp.indexOf(getClass().getSimpleName()), temp.length());
        _prefixPath = temp.toString();
        if (questId != 0) {
            QuestManager.getInstance().addQuest(Quest.this);
        } else {
            _allEventsS.put(name, this);
        }
        init_LoadGlobalData();
    }

    public Quest(int questId, String name, String descr, int ex) {
        _questId = questId;
        _name = name;
        _descr = descr;
        _states = new FastMap<String, State>();
        _prefixPath = "no";

        QuestManager.getInstance().addQuest(Quest.this);
        init_LoadGlobalData();
    }

    /**
     * The function init_LoadGlobalData is, by default, called by the constructor of all quests.
     * Children of this class can implement this function in order to define what variables
     * to load and what structures to save them in.  By default, nothing is loaded.
     */
    protected void init_LoadGlobalData() {
        ;
    }

    /**
     * The function saveGlobalData is, by default, called at shutdown, for all quests, by the QuestManager.
     * Children of this class can implement this function in order to convert their structures 
     * into <var, value> tuples and make calls to save them to the database, if needed.
     * By default, nothing is saved.
     */
    public void saveGlobalData() {
        ;
    }

    public static enum QuestEventType {

        NPC_FIRST_TALK(false), // control the first dialog shown by NPCs when they are clicked (some quests must override the default npc action)
        QUEST_START(true), // onTalk action from start npcs
        QUEST_TALK(true), // onTalk action from npcs participating in a quest
        MOBGOTATTACKED(true), // onAttack action triggered when a mob gets attacked by someone
        MOBKILLED(true), // onKill action triggered when a mob gets killed. 
        MOB_TARGETED_BY_SKILL(true),
        ONSPAWN(true), // onSkillUse action triggered when a character uses a skill on a mob
        ONFOCUS(true);  // onFocus
        // control whether this event type is allowed for the same npc template in multiple quests
        // or if the npc must be registered in at most one quest for the specified event 
        private boolean _allowMultipleRegistration;

        QuestEventType(boolean allowMultipleRegistration) {
            _allowMultipleRegistration = allowMultipleRegistration;
        }

        public boolean isMultipleRegistrationAllowed() {
            return _allowMultipleRegistration;
        }
    }

    /**
     * Return ID of the quest
     * @return int
     */
    public int getQuestIntId() {
        return _questId;
    }

    /**
     * Set the initial state of the quest with parameter "state"
     * @param state
     */
    public void setInitialState(State state) {
        _initialState = state;
    }

    /**
     * Add a new QuestState to the database and return it.
     * @param player
     * @return QuestState : QuestState created
     */
    public QuestState newQuestState(L2PcInstance player) {
        QuestState qs = new QuestState(this, player, getInitialState(), false);
        Quest.createQuestInDb(qs);
        return qs;
    }

    /**
     * Return initial state of the quest
     * @return State
     */
    public State getInitialState() {
        return _initialState;
    }

    /**
     * Return name of the quest
     * @return String
     */
    public String getName() {
        return _name;
    }

    /**
     * Return name of the prefix path for the quest, down to the last "."
     * For example "quests." or "ai.individual."
     * @return String
     */
    public String getPrefixPath() {
        return _prefixPath;
    }

    /**
     * Return description of the quest
     * @return String
     */
    public String getDescr() {
        return _descr;
    }

    /**
     * Add a state to the quest
     * @param state
     * @return state added
     */
    public State addState(State state) {
        _states.put(state.getName(), state);
        return state;
    }

    /**
     * Add a timer to the quest, if it doesn't exist already
     * @param name: name of the timer (also passed back as "event" in onAdvEvent)
     * @param time: time in ms for when to fire the timer
     * @param npc:  npc associated with this timer (can be null)
     * @param player: player associated with this timer (can be null)
     */
    public void startQuestTimer(String name, long time, L2NpcInstance npc, L2PcInstance player) {
        // Add quest timer if timer doesn't already exist
        FastList<QuestTimer> timers = getQuestTimers(name);
        // no timer exists with the same name, at all 
        if (timers == null) {
            timers = new FastList<QuestTimer>();
            timers.add(new QuestTimer(this, name, time, npc, player));
            _allEventTimers.put(name, timers);
        } // a timer with this name exists, but may not be for the same set of npc and player
        else {
            // if there exists a timer with this name, allow the timer only if the [npc, player] set is unique
            // nulls act as wildcards
            if (getQuestTimer(name, npc, player) == null) {
                timers.add(new QuestTimer(this, name, time, npc, player));
            }
        }
        // ignore the startQuestTimer in all other cases (timer is already started)
    }

    public QuestTimer getQuestTimer(String name, L2NpcInstance npc, L2PcInstance player) {
        if (_allEventTimers.get(name) == null) {
            return null;
        }
        for (QuestTimer timer : _allEventTimers.get(name)) {
            if (timer.isMatch(this, name, npc, player)) {
                return timer;
            }
        }
        return null;
    }

    public FastList<QuestTimer> getQuestTimers(String name) {
        return _allEventTimers.get(name);
    }

    public void cancelQuestTimer(String name, L2NpcInstance npc, L2PcInstance player) {
        QuestTimer timer = getQuestTimer(name, npc, player);
        if (timer != null) {
            timer.cancel();
        }
    }

    public void removeQuestTimer(QuestTimer timer) {
        if (timer == null) {
            return;
        }
        FastList<QuestTimer> timers = getQuestTimers(timer.getName());
        if (timers == null) {
            return;
        }
        timers.remove(timer);
    }

    // these are methods to call from java
    public final boolean notifySpawn(L2NpcInstance npc) {
        try {
            onSpawn(npc);
        } catch (Exception e) {
            System.out.println("((((((" + e);
        }
        return true;
    }

    public final boolean notifyFocus(L2NpcInstance npc, L2PcInstance attacker) {
        try {
            onFocus(npc, attacker);
        } catch (Exception e) {
            System.out.println("((((" + e);
        }
        return true;
    }

    public final boolean notifyAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet) {
        String res = null;
        try {
            res = onAttack(npc, attacker, damage, isPet);
        } catch (Exception e) { /*return showError(attacker, e);*/ }
        return showResult(attacker, res);
    }

    public final boolean notifyDeath(L2Character killer, L2Character victim, QuestState qs) {
        String res = null;
        try {
            res = onDeath(killer, victim, qs);
        } catch (Exception e) {
            return showError(qs.getPlayer(), e);
        }
        return showResult(qs.getPlayer(), res);
    }

    public final boolean notifyEvent(String event, L2NpcInstance npc, L2PcInstance player) {
        String res = null;
        try {
            res = onAdvEvent(event, npc, player);
        } catch (Exception e) {
            //return showError(player, e); 
            return false;
        }
        return showResult(player, res);
    }

    public final boolean notifyKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet) {
        String res = null;
        try {
            res = onKill(npc, killer, isPet);
        } catch (Exception e) {
            return showError(killer, e);
        }
        return showResult(killer, res);
    }

    public final boolean notifyTalk(L2NpcInstance npc, QuestState qs) {
        String res = null;
        try {
            if (!Config.ALLOW_CURSED_QUESTS && qs.getPlayer().isCursedWeaponEquiped()) {
                return showResult(qs.getPlayer(), "<html><body>Вы не можете продолжать квест с проклятым оружием.</body></html>");
            }

            res = onTalk(npc, qs.getPlayer());
        } catch (Exception e) {
            return showError(qs.getPlayer(), e);
        }
        qs.getPlayer().setLastQuestNpcObject(npc.getObjectId());
        return showResult(qs.getPlayer(), res);
    }
    // override the default NPC dialogs when a quest defines this for the given NPC

    public final boolean notifyFirstTalk(L2NpcInstance npc, L2PcInstance player) {
        if (!Config.ALLOW_CURSED_QUESTS && player.isCursedWeaponEquiped()) {
            return showResult(player, "<html><body>Вы не можете продолжать квест с проклятым оружием.</body></html>");
        }

        String res = null;
        try {
            res = onFirstTalk(npc, player);
        } catch (Exception e) {
            return showError(player, e);
        }
        player.setLastQuestNpcObject(npc.getObjectId());
        // if the quest returns text to display, display it.  Otherwise, use the default npc text.
        if (res != null && res.length() > 0) {
            return showResult(player, res);
        }
        npc.showChatWindow(player);
        return true;
    }

    public final boolean notifySkillUse(L2NpcInstance npc, L2PcInstance caster, L2Skill skill) {
        String res = null;
        try {
            res = onSkillUse(npc, caster, skill);
        } catch (Exception e) {
            return showError(caster, e);
        }
        return showResult(caster, res);
    }

    // these are methods that java calls to invoke scripts
    public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet) {
        return null;
    }

    public String onSpawn(L2NpcInstance npc) {
        return null;
    }

    public String onFocus(L2NpcInstance npc, L2PcInstance attacker) {
        return null;
    }

    public String onDeath(L2Character killer, L2Character victim, QuestState qs) {
        if (killer.isL2Npc()) {
            return onAdvEvent("", (L2NpcInstance) killer, qs.getPlayer());
        } else {
            return onAdvEvent("", null, qs.getPlayer());
        }
    }

    public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player) {
        // if not overriden by a subclass, then default to the returned value of the simpler (and older) onEvent override
        // if the player has a state, use it as parameter in the next call, else return null
        QuestState qs = player.getQuestState(getName());
        if (qs != null) {
            return onEvent(event, qs);
        }

        return null;
    }

    public String onEvent(String event, QuestState qs) {
        return null;
    }

    public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet) {
        return null;
    }

    public String onTalk(L2NpcInstance npc, L2PcInstance talker) {
        return null;
    }

    public String onFirstTalk(L2NpcInstance npc, L2PcInstance player) {
        return null;
    }

    public String onSkillUse(L2NpcInstance npc, L2PcInstance caster, L2Skill skill) {
        return null;
    }

    /**
     * Show message error to player who has an access level greater than 0
     * @param player : L2PcInstance
     * @param t : Throwable
     * @return boolean
     */
    private boolean showError(L2PcInstance player, Throwable t) {
        /*_log.log(Level.WARNING, "", t);
        if (player.getAccessLevel() > 0) 
        {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        String res = "<html><body><title>Script error</title>"+sw.toString()+"</body></html>";
        return showResult(player, res);
        }*/
        return false;
    }

    /**
     * Show a message to player.<BR><BR>
     * <U><I>Concept : </I></U><BR>
     * 3 cases are managed according to the value of the parameter "res" :<BR>
     * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI>
     * <LI><U>"res" starts with "<html>" :</U> the message hold in "res" is shown in a dialog box</LI>
     * <LI><U>otherwise :</U> the message hold in "res" is shown in chat box</LI>
     * @param qs : QuestState 
     * @param res : String pointing out the message to show at the player
     * @return boolean
     */
    private boolean showResult(L2PcInstance player, String res) {
        if (res == null) {
            return true;
        }
        if (res.endsWith(".htm")) {
            showHtmlFile(player, res);
        } else if (res.startsWith("<html>")) {
            NpcHtmlMessage npcReply = NpcHtmlMessage.id(5);
            npcReply.setHtml(res);
            player.sendPacket(npcReply);
        } else {
            player.sendPacket(SystemMessage.id(SystemMessageId.S1_S2).addString(res));
        }
        return false;
    }

    /**
     * Add quests to the L2PCInstance of the player.<BR><BR>
     * <U><I>Action : </U></I><BR>
     * Add state of quests, drops and variables for quests in the HashMap _quest of L2PcInstance
     * @param player : Player who is entering the world
     */
    public static void playerEnter(L2PcInstance player) {

        Connect con = null;
        PreparedStatement statement = null;
        PreparedStatement invalidQuestData = null;
        PreparedStatement invalidQuestDataVar = null;
        ResultSet rs = null;
        try {
            // Get list of quests owned by the player from database
            con = L2DatabaseFactory.get();

            invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? and name=?");
            invalidQuestDataVar = con.prepareStatement("delete FROM character_quests WHERE char_id=? and name=? and var=?");

            statement = con.prepareStatement("SELECT name,value FROM character_quests WHERE char_id=? AND var=?");
            statement.setInt(1, player.getObjectId());
            statement.setString(2, "<state>");
            rs = statement.executeQuery();
            while (rs.next()) {

                // Get ID of the quest and ID of its state
                String questId = rs.getString("name");
                String stateId = rs.getString("value");

                // Search quest associated with the ID
                Quest q = QuestManager.getInstance().getQuest(questId);
                if (q == null) {
                    _log.finer("Unknown quest " + questId + " for player " + player.getName());
                    if (Config.AUTODELETE_INVALID_QUEST_DATA) {
                        invalidQuestData.setInt(1, player.getObjectId());
                        invalidQuestData.setString(2, questId);
                        invalidQuestData.executeUpdate();
                    }
                    continue;
                }

                // Identify the state of the quest for the player
                boolean completed = false;
                if (stateId.equals("Completed")) {
                    completed = true;
                }

                // Create an object State containing the state of the quest
                State state = q._states.get(stateId);
                if (state == null) {
                    _log.finer("Unknown state in quest " + questId + " for player " + player.getName());
                    if (Config.AUTODELETE_INVALID_QUEST_DATA) {
                        invalidQuestData.setInt(1, player.getObjectId());
                        invalidQuestData.setString(2, questId);
                        invalidQuestData.executeUpdate();
                    }
                    continue;
                }
                // Create a new QuestState for the player that will be added to the player's list of quests
                new QuestState(q, player, state, completed);
            }
            Close.SR(statement, rs);
            Close.S(invalidQuestData);

            // Get list of quests owned by the player from the DB in order to add variables used in the quest.
            statement = con.prepareStatement("SELECT name,var,value FROM character_quests WHERE char_id=?");
            statement.setInt(1, player.getObjectId());
            rs = statement.executeQuery();
            while (rs.next()) {
                String questId = rs.getString("name");
                String var = rs.getString("var");
                String value = rs.getString("value");
                // Get the QuestState saved in the loop before
                QuestState qs = player.getQuestState(questId);
                if (qs == null) {
                    _log.finer("Lost variable " + var + " in quest " + questId + " for player " + player.getName());
                    if (Config.AUTODELETE_INVALID_QUEST_DATA) {
                        invalidQuestDataVar.setInt(1, player.getObjectId());
                        invalidQuestDataVar.setString(2, questId);
                        invalidQuestDataVar.setString(3, var);
                        invalidQuestDataVar.executeUpdate();
                    }
                    continue;
                }
                // Add parameter to the quest
                qs.setInternal(var, value);
            }
            Close.SR(statement, rs);
            Close.S(invalidQuestDataVar);

        } catch (Exception e) {
            _log.log(Level.WARNING, "could not insert char quest:", e);
        } finally {
            Close.S(invalidQuestData);
            Close.S(invalidQuestDataVar);
            Close.CSR(con, statement, rs);
        }

        // events
        for (String name : _allEventsS.keySet()) {
            player.processQuestEvent(name, "enter");
        }
    }

    /**
     * Insert (or Update) in the database variables that need to stay persistant for this quest after a reboot.
     * This function is for storage of values that do not related to a specific player but are
     * global for all characters.  For example, if we need to disable a quest-gatekeeper until 
     * a certain time (as is done with some grand-boss gatekeepers), we can save that time in the DB.  
     * @param var : String designating the name of the variable for the quest
     * @param value : String designating the value of the variable for the quest
     */
    public final void saveGlobalQuestVar(String var, String value) {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("REPLACE INTO quest_global_data (quest_name,var,value) VALUES (?,?,?)");
            st.setString(1, getName());
            st.setString(2, var);
            st.setString(3, value);
            st.executeUpdate();
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not insert global quest variable:", e);
        } finally {
            Close.CS(con, st);
        }
    }

    /**
     * Read from the database a previously saved variable for this quest.
     * Due to performance considerations, this function should best be used only when the quest is first loaded.
     * Subclasses of this class can define structures into which these loaded values can be saved.
     * However, on-demand usage of this function throughout the script is not prohibited, only not recommended. 
     * Values read from this function were entered by calls to "saveGlobalQuestVar"
     * @param var : String designating the name of the variable for the quest
     * @return String : String representing the loaded value for the passed var, or an empty string if the var was invalid
     */
    public final String loadGlobalQuestVar(String var) {
        String result = "";
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?");
            st.setString(1, getName());
            st.setString(2, var);
            rs = st.executeQuery();
            if (rs.first()) {
                result = rs.getString(1);
            }
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not load global quest variable:", e);
        } finally {
            Close.CSR(con, st, rs);
        }
        return result;
    }

    /**
     * Permanently delete from the database a global quest variable that was previously saved for this quest.
     * @param var : String designating the name of the variable for the quest
     */
    public final void deleteGlobalQuestVar(String var) {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ? AND var = ?");
            st.setString(1, getName());
            st.setString(2, var);
            st.executeUpdate();
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not delete global quest variable:", e);
        } finally {
            Close.CS(con, st);
        }
    }

    /**
     * Permanently delete from the database all global quest variables that was previously saved for this quest.
     */
    public final void deleteAllGlobalQuestVars() {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ?");
            st.setString(1, getName());
            st.executeUpdate();
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not delete global quest variables:", e);
        } finally {
            Close.CS(con, st);
        }
    }

    /**
     * Insert in the database the quest for the player.
     * @param qs : QuestState pointing out the state of the quest
     * @param var : String designating the name of the variable for the quest
     * @param value : String designating the value of the variable for the quest
     */
    public static void createQuestVarInDb(QuestState qs, String var, String value) {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("REPLACE INTO character_quests (char_id,name,var,value) VALUES (?,?,?,?)");
            st.setInt(1, qs.getPlayer().getObjectId());
            st.setString(2, qs.getQuestName());
            st.setString(3, var);
            st.setString(4, value);
            st.executeUpdate();
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not insert char quest:", e);
        } finally {
            Close.CS(con, st);
        }
    }

    /**
     * Update the value of the variable "var" for the quest.<BR><BR>
     * <U><I>Actions :</I></U><BR>
     * The selection of the right record is made with :
     * <LI>char_id = qs.getPlayer().getObjectID()</LI>
     * <LI>name = qs.getQuest().getName()</LI>
     * <LI>var = var</LI>
     * <BR><BR>
     * The modification made is :
     * <LI>value = parameter value</LI>
     * @param qs : Quest State
     * @param var : String designating the name of the variable for quest
     * @param value : String designating the value of the variable for quest
     */
    public static void updateQuestVarInDb(QuestState qs, String var, String value) {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("UPDATE character_quests SET value=? WHERE char_id=? AND name=? AND var = ?");
            st.setString(1, value);
            st.setInt(2, qs.getPlayer().getObjectId());
            st.setString(3, qs.getQuestName());
            st.setString(4, var);
            st.executeUpdate();
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not update char quest:", e);
        } finally {
            Close.CS(con, st);
        }
    }

    /**
     * Delete a variable of player's quest from the database.
     * @param qs : object QuestState pointing out the player's quest
     * @param var : String designating the variable characterizing the quest
     */
    public static void deleteQuestVarInDb(QuestState qs, String var) {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=? AND var=?");
            st.setInt(1, qs.getPlayer().getObjectId());
            st.setString(2, qs.getQuestName());
            st.setString(3, var);
            st.executeUpdate();
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not delete char quest:", e);
        } finally {
            Close.CS(con, st);
        }
    }

    /**
     * Delete the player's quest from database.
     * @param qs : QuestState pointing out the player's quest
     */
    public static void deleteQuestInDb(QuestState qs) {
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=?");
            st.setInt(1, qs.getPlayer().getObjectId());
            st.setString(2, qs.getQuestName());
            st.executeUpdate();
        } catch (Exception e) {
            _log.log(Level.WARNING, "could not delete char quest:", e);
        } finally {
            Close.CS(con, st);
        }
    }

    /**
     * Create a record in database for quest.<BR><BR>
     * <U><I>Actions :</I></U><BR>
     * Use fucntion createQuestVarInDb() with following parameters :<BR>
     * <LI>QuestState : parameter sq that puts in fields of database :
     * 	 <UL type="square">
     *     <LI>char_id : ID of the player</LI>
     *     <LI>name : name of the quest</LI>
     *   </UL>
     * </LI>
     * <LI>var : string "&lt;state&gt;" as the name of the variable for the quest</LI>
     * <LI>val : string corresponding at the ID of the state (in fact, initial state)</LI>
     * @param qs : QuestState
     */
    public static void createQuestInDb(QuestState qs) {
        createQuestVarInDb(qs, "<state>", qs.getStateId());
    }

    /**
     * Update informations regarding quest in database.<BR>
     * <U><I>Actions :</I></U><BR>
     * <LI>Get ID state of the quest recorded in object qs</LI>
     * <LI>Test if quest is completed. If true, add a star (*) before the ID state</LI>
     * <LI>Save in database the ID state (with or without the star) for the variable called "&lt;state&gt;" of the quest</LI>
     * @param qs : QuestState
     */
    public static void updateQuestInDb(QuestState qs) {
        String val = qs.getStateId();
        //if (qs.isCompleted())
        //	val = "*" + val;
        updateQuestVarInDb(qs, "<state>", val);
    }

    /**
     * Add this quest to the list of quests that the passed mob will respond to for the specified Event type.<BR><BR>
     * @param npcId : id of the NPC to register
     * @param eventType : type of event being registered 
     * @return L2NpcTemplate : Npc Template corresponding to the npcId, or null if the id is invalid
     */
    public L2NpcTemplate addEventId(int npcId, QuestEventType eventType) {
        try {
            L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
            if (t != null) {
                t.addQuestEvent(eventType, this);
            }
            return t;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Add the quest to the NPC's startQuest
     * @param npcId
     * @return L2NpcTemplate : Start NPC
     */
    public L2NpcTemplate addStartNpc(int npcId) {
        return addEventId(npcId, Quest.QuestEventType.QUEST_START);
    }

    /**
     * Add the quest to the NPC's first-talk (default action dialog)
     * @param npcId
     * @return L2NpcTemplate : Start NPC
     */
    public L2NpcTemplate addFirstTalkId(int npcId) {
        return addEventId(npcId, Quest.QuestEventType.NPC_FIRST_TALK);
    }

    /**
     * Add this quest to the list of quests that the passed mob will respond to for Attack Events.<BR><BR>
     * @param attackId
     * @return int : attackId
     */
    public L2NpcTemplate addAttackId(int attackId) {
        return addEventId(attackId, Quest.QuestEventType.MOBGOTATTACKED);
    }

    /**
     * Событие при спавне НПЦ
     */
    public L2NpcTemplate addSpawnId(int attackId) {
        return addEventId(attackId, Quest.QuestEventType.ONSPAWN);
    }

    /**
     * Событие при взятии нпц в таргет
     */
    public L2NpcTemplate addFocusId(int attackId) {
        return addEventId(attackId, Quest.QuestEventType.ONFOCUS);
    }

    /**
     * Add this quest to the list of quests that the passed mob will respond to for Kill Events.<BR><BR>
     * @param killId
     * @return int : killId
     */
    public L2NpcTemplate addKillId(int killId) {
        return addEventId(killId, Quest.QuestEventType.MOBKILLED);
    }

    /**
     * Add this quest to the list of quests that the passed npc will respond to for Talk Events.<BR><BR>
     * @param talkId : ID of the NPC
     * @return int : ID of the NPC
     */
    public L2NpcTemplate addTalkId(int talkId) {
        return addEventId(talkId, Quest.QuestEventType.QUEST_TALK);
    }

    /**
     * Add this quest to the list of quests that the passed npc will respond to for Skill-Use Events.<BR><BR>
     * @param npcId : ID of the NPC
     * @return int : ID of the NPC
     */
    public L2NpcTemplate addSkillUseId(int npcId) {
        return addEventId(npcId, Quest.QuestEventType.MOB_TARGETED_BY_SKILL);
    }

    // returns a random party member's L2PcInstance for the passed player's party
    // returns the passed player if he has no party.
    public L2PcInstance getRandomPartyMember(L2PcInstance player) {
        // NPE prevention.  If the player is null, there is nothing to return
        if (player == null) {
            return null;
        }
        if ((player.getParty() == null) || (player.getParty().getPartyMembers().size() == 0)) {
            return player;
        }
        L2Party party = player.getParty();
        return party.getPartyMembers().get(Rnd.get(party.getPartyMembers().size()));
    }

    /**
     * Auxilary function for party quests. 
     * Note: This function is only here because of how commonly it may be used by quest developers.
     * For any variations on this function, the quest script can always handle things on its own
     * @param player: the instance of a player whose party is to be searched
     * @param value: the value of the "cond" variable that must be matched
     * @return L2PcInstance: L2PcInstance for a random party member that matches the specified 
     * 			condition, or null if no match.
     */
    public L2PcInstance getRandomPartyMember(L2PcInstance player, String value) {
        return getRandomPartyMember(player, "cond", value);
    }

    /**
     * Auxilary function for party quests. 
     * Note: This function is only here because of how commonly it may be used by quest developers.
     * For any variations on this function, the quest script can always handle things on its own
     * @param player: the instance of a player whose party is to be searched
     * @param var/value: a tuple specifying a quest condition that must be satisfied for
     *     a party member to be considered.
     * @return L2PcInstance: L2PcInstance for a random party member that matches the specified 
     * 				condition, or null if no match.  If the var is null, any random party 
     * 				member is returned (i.e. no condition is applied).
     * 				The party member must be within 1500 distance from the target of the reference
     * 				player, or if no target exists, 1500 distance from the player itself.
     */
    public L2PcInstance getRandomPartyMember(L2PcInstance player, String var, String value) {
        // if no valid player instance is passed, there is nothing to check...
        if (player == null) {
            return null;
        }

        // for null var condition, return any random party member.
        if (var == null) {
            return getRandomPartyMember(player);
        }


        // normal cases...if the player is not in a party, check the player's state
        QuestState temp = null;
        L2Party party = player.getParty();
        // if this player is not in a party, just check if this player instance matches the conditions itself
        if ((party == null) || (party.getPartyMembers().size() == 0)) {
            temp = player.getQuestState(getName());
            if ((temp != null) && (temp.get(var) != null) && ((String) temp.get(var)).equalsIgnoreCase(value)) {
                return player;  // match
            }
            return null;	// no match
        }

        // if the player is in a party, gather a list of all matching party members (possibly 
        // including this player) 
        FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();

        // get the target for enforcing distance limitations.
        L2Object target = player.getTarget();
        if (target == null) {
            target = player;
        }

        for (L2PcInstance partyMember : party.getPartyMembers()) {
            temp = partyMember.getQuestState(getName());
            if ((temp != null) && (temp.get(var) != null) && ((String) temp.get(var)).equalsIgnoreCase(value)
                    && partyMember.isInsideRadius(target, 1500, true, false)) {
                candidates.add(partyMember);
            }
        }
        // if there was no match, return null...
        if (candidates.size() == 0) {
            return null;
        }

        // if a match was found from the party, return one of them at random.
        return candidates.get(Rnd.get(candidates.size()));
    }

    /**
     * Auxilary function for party quests. 
     * Note: This function is only here because of how commonly it may be used by quest developers.
     * For any variations on this function, the quest script can always handle things on its own
     * @param player: the instance of a player whose party is to be searched
     * @param state: the state in which the party member's queststate must be in order to be considered.
     * @return L2PcInstance: L2PcInstance for a random party member that matches the specified 
     * 				condition, or null if no match.  If the var is null, any random party 
     * 				member is returned (i.e. no condition is applied).
     */
    public L2PcInstance getRandomPartyMemberState(L2PcInstance player, State state) {
        // if no valid player instance is passed, there is nothing to check...
        if (player == null) {
            return null;
        }

        // for null var condition, return any random party member.
        if (state == null) {
            return getRandomPartyMember(player);
        }

        // normal cases...if the player is not in a partym check the player's state
        QuestState temp = null;
        L2Party party = player.getParty();
        // if this player is not in a party, just check if this player instance matches the conditions itself
        if ((party == null) || (party.getPartyMembers().size() == 0)) {
            temp = player.getQuestState(getName());
            if ((temp != null) && (temp.getState() == state)) {
                return player;  // match
            }
            return null;	// no match
        }

        // if the player is in a party, gather a list of all matching party members (possibly 
        // including this player) 
        FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();

        // get the target for enforcing distance limitations.
        L2Object target = player.getTarget();
        if (target == null) {
            target = player;
        }

        for (L2PcInstance partyMember : party.getPartyMembers()) {
            temp = partyMember.getQuestState(getName());
            if ((temp != null) && (temp.getState() == state) && partyMember.isInsideRadius(target, 1500, true, false)) {
                candidates.add(partyMember);
            }
        }
        // if there was no match, return null...
        if (candidates.size() == 0) {
            return null;
        }

        // if a match was found from the party, return one of them at random.
        return candidates.get(Rnd.get(candidates.size()));
    }

    /**
     * Show HTML file to client
     * @param fileName
     * @return String : message sent to client 
     */
    public String showHtmlFile(L2PcInstance player, String fileName) {
        if (player == null) {
            return null;
        }

        String questId = getName();

        //Create handler to file linked to the quest
        String directory = getDescr().toLowerCase();
        String content = HtmCache.getInstance().getHtm("data/jscript/" + directory + "/" + questId + "/" + fileName);

        if (content == null) {
            content = HtmCache.getInstance().getHtm("data/jscript/quests/" + questId + "/" + fileName);
        }

        if (content == null) {
            content = HtmCache.getInstance().getHtm("data/scripts/" + directory + "/" + questId + "/" + fileName);
        }

        if (content == null) {
            content = HtmCache.getInstance().getHtm("data/scripts/quests/" + questId + "/" + fileName);
        }

        //Send message to client if message not empty     
        if (content != null && player.getTarget() != null) {
            content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));
            NpcHtmlMessage npcReply = NpcHtmlMessage.id(5);
            npcReply.setHtml(content);
            player.sendPacket(npcReply);
        }

        return content;
    }

    // =========================================================
    //  QUEST SPAWNS
    // =========================================================
    public static class DeSpawnScheduleTimerTask implements Runnable {

        L2NpcInstance _npc = null;

        public DeSpawnScheduleTimerTask(L2NpcInstance npc) {
            _npc = npc;
        }

        public void run() {
            _npc.onDecay();
        }
    }

    // Method - Public
    /**
     * Add a temporary (quest) spawn
     * Return instance of newly spawned npc
     */
    public L2NpcInstance addSpawn(int npcId, L2Character cha) {
        return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0);
    }

    public L2NpcInstance addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay) {
        L2NpcInstance result = null;
        try {
            L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
            if (template != null) {
                // Sometimes, even if the quest script specifies some xyz (for example npc.getX() etc) by the time the code
                // reaches here, xyz have become 0!  Also, a questdev might have purposely set xy to 0,0...however,
                // the spawn code is coded such that if x=y=0, it looks into location for the spawn loc!  This will NOT work
                // with quest spawns!  For both of the above cases, we need a fail-safe spawn.  For this, we use the 
                // default spawn location, which is at the player's loc.
                if ((x == 0) && (y == 0)) {
                    _log.log(Level.SEVERE, "Failed to adjust bad locks for quest spawn!  Spawn aborted!");
                    return null;
                }
                if (randomOffset) {
                    int offset;

                    offset = Rnd.get(2); // Get the direction of the offset
                    if (offset == 0) {
                        offset = -1;
                    } // make offset negative
                    offset *= Rnd.get(50, 100);
                    x += offset;

                    offset = Rnd.get(2); // Get the direction of the offset
                    if (offset == 0) {
                        offset = -1;
                    } // make offset negative
                    offset *= Rnd.get(50, 100);
                    y += offset;
                }
                L2Spawn spawn = new L2Spawn(template);
                spawn.setHeading(heading);
                spawn.setLocx(x);
                spawn.setLocy(y);
                spawn.setLocz(z + 20);
                spawn.stopRespawn();
                result = spawn.spawnOne();

                if (despawnDelay > 0) {
                    ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(result), despawnDelay);
                }

                return result;
            }
        } catch (Exception e1) {
            _log.warning("Could not spawn Npc " + npcId);
        }

        return null;
    }

    public void registerItem(int itemId) {
        if (_questItemIds == null) {
            _questItemIds = new FastList<Integer>();
        }
        _questItemIds.add(itemId);
    }

    public FastList<Integer> getRegisteredItemIds() {
        return _questItemIds;
    }
}
