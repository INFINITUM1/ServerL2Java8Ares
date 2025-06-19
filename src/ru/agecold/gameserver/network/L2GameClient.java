/* This program is free software; you can redistribute it and/or modify
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
package ru.agecold.gameserver.network;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javolution.util.FastList;
import ru.agecold.Config;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.GameServer;
import ru.agecold.gameserver.LoginServerThread;
import ru.agecold.gameserver.LoginServerThread.SessionKey;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.model.CharSelectInfoPackage;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.L2Event;
import ru.agecold.gameserver.model.entity.olympiad.Olympiad;
import ru.agecold.gameserver.network.gameserverpackets.SetPhoneNumber;
import ru.agecold.gameserver.network.serverpackets.ActionFailed;
import ru.agecold.gameserver.network.serverpackets.CharSelected;
import ru.agecold.gameserver.network.serverpackets.L2GameServerPacket;
import ru.agecold.gameserver.network.serverpackets.NetPing;
import ru.agecold.gameserver.network.serverpackets.ServerClose;
import ru.agecold.gameserver.network.serverpackets.ServerCloseSocket;
import ru.agecold.gameserver.network.serverpackets.UserInfo;
import ru.agecold.gameserver.taskmanager.LazyPrecisionTaskManager;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;
import ru.agecold.util.EventData;
import org.mmocore.network.nio.impl.MMOClient;
import org.mmocore.network.nio.impl.MMOConnection;

/**
 * Represents a client connected on Game Server
 *
 * @author KenM
 */
public final class L2GameClient extends MMOClient<MMOConnection<L2GameClient>> {
    protected static final Logger _log = Logger.getLogger(L2GameClient.class.getName());

    /**
     * CONNECTED	- client has just connected AUTHED	- client has authed but
     * doesnt has character attached to it yet IN_GAME	- client has selected a
     * char and is in game
     *
     * @author KenM
     */
    public static enum GameClientState {

        CONNECTED, AUTHED, IN_GAME, DISCONNECTED
    };
    public GameClientState state;
    // Info
    public String accountName;
    public SessionKey sessionId;
    public L2PcInstance activeChar;
    private ReentrantLock _activeCharLock = new ReentrantLock();
    private long _connectionStartTime;
    private List<Integer> _charSlotMapping = new FastList<Integer>();
    // Task
    protected /*
             * final
             */ ScheduledFuture<?> _autoSaveInDB;
    protected ScheduledFuture<?> _cleanupTask = null;
    // Crypt
    public GameCrypt gameCrypt = null;
    // Flood protection
    private int _upTryes = 0;
    private long _upLastConnection = 0;
    //
    private int _lastServerId = -1;
    /*
     * public byte packetsSentInSec = 0; public int packetsSentStartTick = 0;
     * private int unknownPacketCount = 0;
     */
    private String _ip;

    public L2GameClient(MMOConnection<L2GameClient> con) {
        super(con);
        state = GameClientState.CONNECTED;
        _connectionStartTime = System.currentTimeMillis();
        gameCrypt = new GameCrypt();

        _autoSaveInDB = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoSaveTask(), 300000L, 900000L);
        _ip = con.getSocket().getInetAddress().getHostAddress();
    }

    public L2GameClient(MMOConnection<L2GameClient> con, boolean offline) {
        super(con);
        state = GameClientState.IN_GAME;
    }

    public byte[] enableCrypt() {
        byte[] key = BlowFishKeygen.getRandomKey();
        gameCrypt.setKey(key);
        return key;
    }

    public GameClientState getState() {
        return state;
    }

    public void setState(GameClientState pState) {
        state = pState;
    }

    public long getConnectionStartTime() {
        return _connectionStartTime;
    }

    @Override
    public boolean decrypt(ByteBuffer buf, int size) {
        /*crypt.decrypt(buf.array(), buf.position(), size);
        return true;*/
        return gameCrypt.decrypt(buf.array(), buf.position(), size);
    }

    @Override
    public boolean encrypt(final ByteBuffer buf, final int size) {
        /*crypt.encrypt(buf.array(), buf.position(), size);
        buf.position(buf.position() + size);
        return true;*/
        gameCrypt.encrypt(buf.array(), buf.position(), size);
        buf.position(buf.position() + size);
        return true;
    }

    public L2PcInstance getActiveChar() {
        return activeChar;
    }

    public void setActiveChar(L2PcInstance pActiveChar) {
        activeChar = pActiveChar;
        if (activeChar != null) {
            L2World.getInstance().storeObject(getActiveChar());
        }
    }

    public ReentrantLock getActiveCharLock() {
        return _activeCharLock;
    }

    public void setAccountName(String pAccountName) {
        accountName = pAccountName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setSessionId(SessionKey sk) {
        sessionId = sk;
    }

    public SessionKey getSessionId() {
        return sessionId;
    }

    public void sendPacket(L2GameServerPacket gsp) {
        if (gsp == null) {
            return;
        }

        if (!isConnected()) {
            return;
        }

        getConnection().sendPacket(gsp);
    }

    public String getIpAddr() {
        return _ip;
    }

    public L2PcInstance markToDeleteChar(int charslot) throws Exception {
        //have to make sure active character must be nulled
        /*
         * if (getActiveChar() != null) { saveCharToDisk(getActiveChar()); if
         * (Config.DEBUG) { _log.fine("active Char saved"); }
         * this.setActiveChar(null); }
         */

        int objid = getObjectIdForSlot(charslot);
        if (objid < 0) {
            return null;
        }

        L2PcInstance character = L2PcInstance.load(objid);
        if (character.getClanId() != 0) {
            return character;
        }

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE obj_id=?");
            statement.setLong(1, System.currentTimeMillis() + Config.DELETE_DAYS * 86400000L); // 24*60*60*1000 = 86400000
            statement.setInt(2, objid);
            statement.execute();
        } catch (Exception e) {
            _log.warning("Data error on update delete time of char: " + e);
        } finally {
            Close.CS(con, statement);
        }
        return null;
    }

    public L2PcInstance deleteChar(int charslot) throws Exception {
        //have to make sure active character must be nulled
        /*
         * if (getActiveChar() != null) { saveCharToDisk (getActiveChar()); if
         * (Config.DEBUG) _log.fine("active Char saved");
         * this.setActiveChar(null); }
         */

        int objid = getObjectIdForSlot(charslot);
        if (objid < 0) {
            return null;
        }

        L2PcInstance character = L2PcInstance.load(objid);
        if (character.getClanId() != 0) {
            return character;
        }

        deleteCharByObjId(objid);
        return null;
    }

    public void markRestoredChar(int charslot) throws Exception {
        //have to make sure active character must be nulled
        /*
         * if (getActiveChar() != null) { saveCharToDisk (getActiveChar()); if
         * (Config.DEBUG) _log.fine("active Char saved");
         * this.setActiveChar(null); }
         */

        int objid = getObjectIdForSlot(charslot);

        if (objid < 0) {
            return;
        }

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
        } catch (Exception e) {
            _log.severe("Data error on restoring char: " + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    public static void deleteCharByObjId(int objid) {
        if (objid < 0) {
            return;
        }

        Connect con = null;
        PreparedStatement statement = null;

        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? OR friend_id=?");
            statement.setInt(1, objid);
            statement.setInt(2, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM character_buffs WHERE char_obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM heroes WHERE char_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE char_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM seven_signs WHERE char_obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM character_settings WHERE char_obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);

            statement = con.prepareStatement("DELETE FROM characters WHERE obj_Id=?");
            statement.setInt(1, objid);
            statement.execute();
            Close.S(statement);
        } catch (Exception e) {
            _log.warning("Data error on deleting char: " + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    public L2PcInstance loadCharFromDisk(int charslot) {
        Integer objectId = getObjectIdForSlot(charslot);
        if (objectId == -1) {
            return null;
        }

        L2PcInstance ingame = L2World.getInstance().getPlayer(objectId);
        if (ingame != null) {
            ingame.kick(true);
            /*
             * if (ingame.getClient() != null) { ingame.getClient().closeNow();
             * } else { ingame.deleteMe(); }
             */
            return null;
        }

        L2PcInstance character = L2PcInstance.load(objectId);
        if (character != null) {
            //restoreInventory(character);
            //restoreSkills(character);
            //character.restoreSkills();
            //restoreShortCuts(character);
            //restoreWarehouse(character);

            // preinit some values for each login
            character.setRunning();	// running is default
            character.standUp();		// standing is default

            character.refreshOverloaded();
            character.refreshExpertisePenalty();
            character.sendPacket(new UserInfo(character));
            character.broadcastKarma();
            character.setOnlineStatus(true);
        } else {
            _log.severe("could not restore in slot: " + charslot);
        }

        //setCharacter(character);
        return character;
    }

    /**
     * @param chars
     */
    public void setCharSelection(CharSelectInfoPackage[] chars) {
        _charSlotMapping.clear();

        for (int i = 0; i < chars.length; i++) {
            int objectId = chars[i].getObjectId();
            _charSlotMapping.add(Integer.valueOf(objectId));
        }
    }

    public void setCharSelection(int c) {
        _charSlotMapping.clear();
        _charSlotMapping.add(c);
    }

    public void close(L2GameServerPacket gsp) {
        if (gsp == null) {
            return;
        }

        if (!isConnected()) {
            return;
        }

        getConnection().close(gsp);
    }

    /**
     * @param charslot
     * @return
     */
    private int getObjectIdForSlot(int charslot) {
        if (charslot < 0 || charslot >= _charSlotMapping.size()) {
            _log.warning(toString() + " tried to delete Character in slot " + charslot + " but no characters exits at that slot.");
            return -1;
        }
        Integer objectId = _charSlotMapping.get(charslot);
        return objectId.intValue();
    }

    @Override
    protected void onForcedDisconnection() {
        //_log.info(TimeLogger.getLogTime()+"Client "+toString()+" disconnected abnormally. // last packet: " + getLastSendedPacket());
    }

    @Override
    protected void onDisconnection() {
        //System.out.println("##onDisconnection##1#");
        if (getAccountName() == null || getAccountName().equals("") || state != GameClientState.IN_GAME && state != GameClientState.AUTHED) {
            return;
        }

        try {
            stopPingTask();
            _autoSaveInDB.cancel(true);

            L2PcInstance player = L2GameClient.this.getActiveChar();
            if (player != null) // this should only happen on connection loss
            {
                if (player.getActiveTradeList() != null) {
                    //player.getTransactionRequester().onTradeCancel(player);
                    //player.onTradeCancel(player.getTransactionRequester());
                    player.cancelActiveTrade();
                    if (player.getTransactionRequester() != null) {
                        player.getTransactionRequester().setTransactionRequester(null);
                    }
                    player.setTransactionRequester(null);
                }

                if (player.isInOfflineMode()) //LSConnection.getInstance().sendPacket(new PlayerLogout(getLoginName()));
                {
                    return;
                }

                // we store all data from players who are disconnected while in an event in order to restore it in the next login
                if (player.atEvent) {
                    EventData data = new EventData(player.eventX, player.eventY, player.eventZ, player.eventkarma, player.eventpvpkills, player.eventpkkills, player.eventTitle, player.kills, player.eventSitForced);
                    L2Event.connectionLossData.put(player.getName(), data);
                }

                if (player.isCastingNow()) {
                    player.abortCast();
                }

                if (Olympiad.isRegisteredInComp(player) || player.isInOlympiadMode() || player.getOlympiadGameId() > -1) {
                    Olympiad.logoutPlayer(player);
                }

                if (player.isFlying()) {
                    player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
                }

                if (player.getPet() != null) {
                    player.getPet().unSummon(player);
                }

                saveCharToDisk(player);

                // notify the world about our disconnect
                player.deleteMe();

                if (player.getClient() != null) {
                    player.closeNetConnection();
                    player.setClient(null);
                }

                player.gc();
                player = null;
            }
            L2GameClient.this.setActiveChar(null);
        } catch (Exception e1) {
            _log.log(Level.WARNING, "error while disconnecting client", e1);
        } finally {
            LoginServerThread.getInstance().sendLogout(L2GameClient.this.getAccountName());
        }

        state = GameClientState.DISCONNECTED;
    }

    public void playerSelected(int charSlot)
    {
        //int objId = getObjectIdForSlot(charSlot);
        //if(objId <= 0)
        //{
        //    sendPacket(new ActionFailed());
        //    return;
        //}

        if (getActiveCharLock().tryLock()) {
            try {
                //System.out.println("###2#");
                LoginServerThread.checkLogoutRoom(getAccountName(), this);
                // should always be null
                // but if not then this is repeated packet and nothing should be done here
                if(getActiveChar() != null)
                {
                    sendPacket(new ActionFailed());
                    return;
                }
                //if(!isAuthed())
                //{
                //    _log.info("Not authed client from IP: " + getIpAddr() + " Acc: " + getAccountName());
                //    close(Static.ServerClose);
                //    return;
                //}
                //if(AutoBan.isBanned(objId))
                //{
                //    _log.info("Try enter banned char[" + objId + "] from IP: " + getIpAddr() + " Acc: " + getAccountName());
                //    close(Static.ServerClose);
                //    return;
                //}
                //System.out.println("###3#");
                // The L2PcInstance must be created here, so that it can be attached to the L2GameClient
                //if (Config.DEBUG)
                ///{
                //	_log.fine("selected slot:" + _charSlot);
                //}

                //load up character from disk
                L2PcInstance cha = loadCharFromDisk(charSlot);
                if (cha == null) {
                    _log.severe("Character could not be loaded (slot:" + charSlot + ")");
                    sendPacket(new ActionFailed());
                    return;
                }
                //System.out.println("###4#");
                if (cha.getAccessLevel() < 0) {
                    cha.closeNetConnection();
                    return;
                }

                //System.out.println("###5#");
                cha.setClient(this);
                //System.out.println("###6#");
                setActiveChar(cha);
                //System.out.println("###7#");

                //cha.setEnterWorld();
                /*if (!Config.CATS_GUARD) {
                 sendGameGuardRequest();
                 }*/
                cha.storeHWID(_hwid);
                //System.out.println("###8#");
                setState(GameClientState.IN_GAME);
                //System.out.println("###9#");
                //CharSelected cs = new CharSelected(cha, getSessionId().playOkID1);
                sendPacket(new CharSelected(cha, getSessionId().playOkID1));
                // System.out.println("###10#");
            } finally {
                getActiveCharLock().unlock();
            }
        }
    }

    /**
     * Save the L2PcInstance to the database.
     */
    public static void saveCharToDisk(L2PcInstance cha) {
        try {
            cha.getInventory().updateDatabase(true);
            cha.store();
        } catch (Exception e) {
            _log.severe("Error saving player character " + cha.getName() + ": " + e);
        }
    }

    /**
     * Produces the best possible string representation of this client.
     */
    @Override
    public String toString() {
        try {
            InetAddress address = getConnection().getSocket().getInetAddress();
            switch (getState()) {
                case CONNECTED:
                    return "[IP: " + _ip + "]";
                case AUTHED:
                    return "[Account: " + getAccountName() + " - IP: " + _ip + "]";
                case IN_GAME:
                    return "[Character: " + (getActiveChar() == null ? "disconnected" : getActiveChar().getName()) + " - Account: " + getAccountName() + " - IP: " + _ip + " - HWID: " + _hwid + "]";
                case DISCONNECTED:
                    return "[Disconnected]";
                default:
                    throw new IllegalStateException("Missing state on switch");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            return "[Character read failed due to disconnect]";
        }
    }

    class AutoSaveTask implements Runnable {

        public void run() {
            try {
                L2PcInstance player = L2GameClient.this.getActiveChar();
                if (player != null) {
                    saveCharToDisk(player);
                }
            } catch (Throwable e) {
                _log.severe(e.toString());
            }
        }
    }

    public void addUPTryes() {
        if (_upLastConnection != 0 && System.currentTimeMillis() - _upLastConnection > 700) {
            _upTryes = 0;
        }
        _upTryes++;
        _upLastConnection = System.currentTimeMillis();
    }

    public int getUPTryes() {
        if (_upLastConnection != 0 && System.currentTimeMillis() - _upLastConnection > 700) {
            _upTryes = 0;
            _upLastConnection = System.currentTimeMillis();
        }
        return _upTryes;
    }

    public boolean isFlooder() {
        return _upTryes > 4;
    }
    //
    private String _lastSendedPacket = "";

    public void setLastSendedPacket(String packet) {
        _lastSendedPacket = packet;
    }

    public String getLastSendedPacket() {
        return _lastSendedPacket;
    }

    public void close() {
        //System.out.println("##close()##1#");
        L2PcInstance player = L2GameClient.this.getActiveChar();
        if (player != null) {
            player.sendPacket(Static.YOU_HAVE_BEEN_DISCONNECTED);
            player.sendPacket(new ServerClose());
            player.sendPacket(new ServerCloseSocket());
        }
        closeNow();
    }

    public void close(boolean kiknw) {
        //System.out.println("##close(boolean kiknow)##1#");
        L2PcInstance charw = L2GameClient.this.getActiveChar();
        if (kiknw && charw != null) {
            charw.sendPacket(Static.YOU_HAVE_BEEN_DISCONNECTED);
            charw.sendPacket(new ServerClose());
            charw.sendPacket(new ServerCloseSocket());
        }
        closeNow();
    }

    public void kick(String account) {
        L2PcInstance player = L2GameClient.this.getActiveChar();
        if (player != null) {
            if (player.isInOfflineMode()) {
                player.kick();
                return;
            } else {
                player.incAccKickCount();
                player.sendMessage("���-�� �������� ����� �� ������ ���������!");
                //LoginController.getInstance().sendPacket(new PlayerLogout(player.getAccountName()));
            }

            if (Config.KICK_USED_ACCOUNT_TRYES == 0) {
                player.kick();
                closeNow();
                LoginServerThread.getInstance().sendLogout(account);
                return;
            } else {
                if (player.getAccKickCount() > Config.KICK_USED_ACCOUNT_TRYES) {
                    player.kick();
                    closeNow();
                    LoginServerThread.getInstance().sendLogout(account);
                }
                return;
            }
        }
        closeNow();
        LoginServerThread.getInstance().sendLogout(account);
    }

    // ������ HWID: 867157d6890adda666c4fe110df01fda
    // ����������
    private String _hwid = "none";
    private String _myhwid = "none";

    public void setHWID(String hwid) {
        _hwid = hwid;
    }

    public String getHWID() {
        return _hwid;
    }

    public String getMyHWID() {
        return _myhwid;
    }

    public void setMyHWID(String hwid) {
        _myhwid = hwid;
    }

    //������������
    public void saveHWID(boolean f) {
        LoginServerThread.getInstance().setHwid(getAccountName(), (f ? getHWID() : ""));
        setMyHWID(f ? getHWID() : "none");
    }

    public boolean acceptHWID(String hwid) {
        if (hwid.equalsIgnoreCase("none")) {
            return true;
        }

        if (_hwid.equalsIgnoreCase(hwid)) {
            _myhwid = hwid;
            return true;
        }
        return false;
    }

    private boolean _hasEmail = false;

    public void setHasEmail(boolean hasEmail) {
        _hasEmail = hasEmail;
    }

    public boolean hasEmail() {
        return _hasEmail;
    }

    public String getHWid() {
        return this._hwid;
    }

    public String getFingerPrints() {
        return "Account: " + accountName + ", ip: " + this.getIpAddr() + ", " + "hwid: " + _hwid;
    }

    //
    public void setLastServerId(int id) {
        _lastServerId = id;
    }

    public int getLastServerId() {
        return _lastServerId;
    }

    private int _failedPackets;
    private int _unknownPackets;

    public void onPacketReadFail() {
        if (_failedPackets++ >= 10) {
            _log.warning("Too many client packet fails, connection closed : " + this);
            closeNow(true);
        }
    }

    public void onUnknownPacket() {
        if (_unknownPackets++ >= 10) {
            _log.warning("Too many client unknown packets, connection closed : " + this);
            closeNow(true);
        }
    }

    //
    private Future<?> _pingTask;
    private int _pingTryCount;
    private int _pingTime;
    private String _phoneNum = "";

    public void startPingTask() {
        _pingTask = LazyPrecisionTaskManager.getInstance().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (++_pingTryCount > 2) {
                    closeNow(true);
                    //System.out.println("###NetPing##Player is offline, closing connection...");// NetPing
                } else {
                    int time = (int) (System.currentTimeMillis() - GameServer.getInstance().getServerStartTime());
                    sendPacket(new NetPing(time));
                }
            }
        }, 15000L, 15000L);
    }

    public void onNetPing(int time) {
        //System.out.println("###onNetPing##");
        _pingTryCount--;
        _pingTime = (int) (System.currentTimeMillis() - GameServer.getInstance().getServerStartTime() - time);
        _pingTime /= 2;
    }

    public int getPing() {
        return _pingTime;
    }

    public void stopPingTask() {
        if (_pingTask != null) {
            _pingTask.cancel(false);
            _pingTask = null;
        }
    }

    public void setPhoneNum(String num) {
        _phoneNum = num;
    }

    public String getPhoneNum() {
        return _phoneNum;
    }

    public void storePhoneNum(String phone) {
        _phoneNum = phone;
        try {
            LoginServerThread.getInstance().sendPacket(new SetPhoneNumber(accountName, phone));
        }
        catch (IOException ex) {
            Logger.getLogger(L2GameClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int revision = 0;

    public int getRevision()
    {
        return revision;
    }

    public void setRevision(final int revision)
    {
        this.revision = revision;
    }

    public boolean isHWIDBanned()
    {
        boolean result = false;
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try
        {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("SELECT * FROM hwid_bans WHERE HWID=? LIMIT 1");
            statement.setString(1, _hwid);
            rs = statement.executeQuery();
            if(rs.next())
            {
                long time = rs.getLong("end_date");
                if(time <= 0 || time > System.currentTimeMillis())
                    result = true;
                else
                {
                    Close.SR(statement, rs);
                    statement = con.prepareStatement("DELETE FROM hwid_bans WHERE HWID=?");
                    statement.setString(1, _hwid);
                    statement.execute();
                }
            }

        }
        catch(Exception E)
        {}
        finally
        {
            Close.CSR(con, statement, rs);
        }
        return result;
    }
}
