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
package scripts.commands.admincommandhandlers;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import ru.agecold.Config;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.LoginServerThread;
import ru.agecold.gameserver.instancemanager.PlayerManager;
import ru.agecold.gameserver.model.GMAudit;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.olympiad.Olympiad;
import ru.agecold.gameserver.network.L2GameClient;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.gameserverpackets.ChangeAccessLevel;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.util.AutoBan;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;
import scripts.commands.IAdminCommandHandler;

/**
 * This class handles following admin commands:
 * - ban account_name = changes account access level to -100 and logs him off. If no account is specified, target's account is used.
 * - unban account_name = changes account access level to 0.
 * - jail charname [penalty_time] = jails character. Time specified in minutes. For ever if no time is specified.
 * - unjail charname = Unjails player, teleport him to Floran.
 *
 * @version $Revision: 1.1.6.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminBan implements IAdminCommandHandler {

    private static final String[] ADMIN_COMMANDS = {"admin_ban", "admin_unban", "admin_jail", "admin_unjail", "admin_unhban", "admin_isban", "admin_accsbanhwid", "admin_hban", "admin_hnban", "admin_hwidban" };
    private static final int REQUIRED_LEVEL = Config.GM_BAN;

    public boolean useAdminCommand(String command, L2PcInstance adm) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (!(checkLevel(adm.getAccessLevel()))) {
                return false;
            }
        }

        String account_name = "";
        String player = "";
        L2PcInstance plyr = null;
        if (command.startsWith("admin_ban")) {
            try {
                StringTokenizer st = new StringTokenizer(command);
                if(st.countTokens() > 1)
                {
                    st.nextToken();
                    player = st.nextToken();
                    plyr = L2World.getInstance().getPlayer(player);
                }
            } catch (Exception e) {
                L2Object target = adm.getTarget();
                if (target != null && target.isPlayer()) {
                    plyr = (L2PcInstance) target;
                } else {
                    adm.sendAdmResultMessage("Usage: //ban [account_name] (if none, target char's account gets banned)");
                }
            }
            if (plyr != null && plyr.equals(adm)) {
                plyr.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_USE_ON_YOURSELF));
            } else if (plyr == null) {
                account_name = player;
                LoginServerThread.getInstance().sendAccessLevel(account_name, 0);
                adm.sendAdmResultMessage("Ban request sent for account " + account_name + ". If you need a playername based commmand, see //ban_menu");
            } else {
                Olympiad.clearPoints(plyr.getObjectId());
                plyr.setAccountAccesslevel(-100);
                account_name = plyr.getAccountName();
                //RegionBBSManager.getInstance().changeCommunityBoard();
                plyr.logout();
                adm.sendAdmResultMessage("Account " + account_name + " banned.");
            }
        } else if (command.startsWith("admin_unban")) {
            try {
                StringTokenizer st = new StringTokenizer(command);
                if(st.countTokens() > 1)
                {
                    st.nextToken();
                    account_name = st.nextToken();
                    LoginServerThread.getInstance().sendAccessLevel(account_name, 0);
                    adm.sendAdmResultMessage("Unban request sent for account " + account_name + ". If you need a playername based commmand, see //unban_menu");
                }
            } catch (Exception e) {
                adm.sendAdmResultMessage("Usage: //unban <account_name>");
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        } else if (command.startsWith("admin_jail")) {
            try {
                StringTokenizer st = new StringTokenizer(command);
                st.nextToken();
                player = st.nextToken();
                int delay = 0;
                try {
                    delay = Integer.parseInt(st.nextToken());
                } catch (NumberFormatException nfe) {
                    adm.sendAdmResultMessage("Usage: //jail <charname> [penalty_minutes]");
                } catch (NoSuchElementException nsee) {
                }
                L2PcInstance playerObj = L2World.getInstance().getPlayer(player);
                if (playerObj != null) {
                    playerObj.setInJail(true, delay);
                    adm.sendAdmResultMessage("Character " + player + " jailed for " + (delay > 0 ? delay + " minutes." : "ever!"));
                } else {
                    jailOfflinePlayer(adm, player, delay);
                }
            } catch (NoSuchElementException nsee) {
                adm.sendAdmResultMessage("Usage: //jail <charname> [penalty_minutes]");
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        } else if (command.startsWith("admin_unjail")) {
            try {
                StringTokenizer st = new StringTokenizer(command);
                st.nextToken();
                player = st.nextToken();
                L2PcInstance playerObj = L2World.getInstance().getPlayer(player);

                if (playerObj != null) {
                    playerObj.setInJail(false, 0);
                    adm.sendAdmResultMessage("Character " + player + " removed from jail");
                } else {
                    unjailOfflinePlayer(adm, player);
                }
            } catch (NoSuchElementException nsee) {
                adm.sendAdmResultMessage("Specify a character name.");
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        /*else if(command.startsWith("admin_unhban")) {
            StringTokenizer st = new StringTokenizer(command);
            if(st.countTokens() > 1)
            {
                st.nextToken();
                String name = st.nextToken();
                Connect con = null;
                PreparedStatement statement = null;
                ResultSet rset = null;
                try
                {
                    con = L2DatabaseFactory.get();
                    statement = con.prepareStatement("SELECT char_name,LastHWID FROM characters WHERE char_name=? LIMIT 1");
                    statement.setString(1, name);
                    rset = statement.executeQuery();
                    if(rset.next())
                    {
                        name = rset.getString("char_name");
                        String hwid = rset.getString("LastHWID");
                        Close.SR(statement, rset);
                        statement = con.prepareStatement("DELETE FROM hwid_bans WHERE HWID=? OR player=?");
                        statement.setString(1, hwid);
                        statement.setString(2, name);
                        statement.execute();
                        adm.sendAdmResultMessage("Deleted from hwid_bans: " + name + " HWID: " + hwid);
                    }
                    else
                        adm.sendAdmResultMessage("Can't find char: " + name);
                }
                catch(Exception e)
                {}
                finally
                {
                    Close.CSR(con, statement, rset);
                }
            }
        }*/
        /*else if(command.startsWith("admin_isban"))
        {
            StringTokenizer st = new StringTokenizer(command);
            if(st.countTokens() > 1)
            {
                String name = st.nextToken();
                int id = PlayerManager.getObjectIdByName(name);
                if(id <= 0)
                {
                    adm.sendAdmResultMessage("Player " + name + " not exist.");
                    return false;
                }
                name = PlayerManager.getNameByObjectId(id);
                Connect con = null;
                PreparedStatement statement = null;
                ResultSet rset = null;
                String hwid = PlayerManager.getLastHWIDByName(name);
                if(hwid.isEmpty())
                    adm.sendAdmResultMessage("No hwid for char: " + name);
                else
                {
                    try
                    {
                        con = L2DatabaseFactory.get();
                        statement = con.prepareStatement("SELECT reason,end_date FROM hwid_bans WHERE HWID=? LIMIT 1");
                        statement.setString(1, hwid);
                        rset = statement.executeQuery();
                        if(rset.next())
                        {
                            long time = rset.getLong("end_date");
                            adm.sendAdmResultMessage(name + " banned by hwid." + (time > 0L ? "EndDate: " + TimeUtils.toSimpleFormat(time) : "") + " Reason: " + rset.getString("reason"));
                        }
                        else
                            adm.sendAdmResultMessage("No hwid ban for char: " + name);
                    }
                    catch(Exception e)
                    {}
                    finally
                    {
                        Close.CSR(con, statement, rset);
                    }
                }
                String account = PlayerManager.getAccNameByName(name);
                try
                {
                    con = L2DatabaseFactory.getInstanceLogin().getConnection();
                    statement = con.prepareStatement("SELECT access_level,last_ip FROM accounts WHERE login=? LIMIT 1");
                    statement.setString(1, account);
                    rset = statement.executeQuery();
                    String lastIp = "";
                    if(rset.next())
                    {
                        if(rset.getInt("access_level") < 0)
                            adm.sendAdmResultMessage(name + " banned by account.");
                        else
                            adm.sendAdmResultMessage("No account ban for char: " + name);
                        lastIp = rset.getString("last_ip");
                    }
                    if(!lastIp.isEmpty())
                    {
                        Close.SR(statement, rset);
                        statement = con.prepareStatement("SELECT * FROM banned_ips WHERE ip=? LIMIT 1");
                        statement.setString(1, lastIp);
                        rset = statement.executeQuery();
                        if(rset.next())
                            adm.sendAdmResultMessage(name + " banned by IP: " + lastIp);
                        else
                            adm.sendAdmResultMessage("No IP ban for char: " + name);
                    }
                }
                catch(Exception e)
                {}
                finally
                {
                    Close.CSR(con, statement, rset);
                }
                if(AutoBan.isBanned(id))
                    adm.sendAdmResultMessage(name + " is banned.");
                else
                    adm.sendAdmResultMessage("No ban for char: " + name);
            }
        }*/
        /*else if(command.startsWith("admin_accsbanhwid"))
        {
            StringTokenizer st = new StringTokenizer(command);
            if(st.countTokens() > 1)
            {
                st.nextToken();
                String name = st.nextToken();
                int id = PlayerManager.getObjectIdByName(name);
                if(id <= 0)
                {
                    adm.sendAdmResultMessage("Player " + name + " not exist.");
                    return true;
                }
                String hwid = PlayerManager.getLastHWIDByName(name);
                if(hwid.isEmpty())
                {
                    adm.sendAdmResultMessage("No hwid for player " + name);
                    return true;
                }

                List<String> accs = new ArrayList<>();
                Connect con = null;
                PreparedStatement statement = null;
                ResultSet rset = null;
                try
                {
                    con = L2DatabaseFactory.get();
                    statement = con.prepareStatement("SELECT account_name FROM characters WHERE LastHWID=?");
                    statement.setString(1, hwid);
                    rset = statement.executeQuery();
                    while(rset.next())
                    {
                        String ac = rset.getString("account_name");
                        if(!accs.contains(ac))
                            accs.add(ac);
                    }
                }
                catch(Exception e)
                {}
                finally
                {
                    Close.CSR(con, statement, rset);
                }
                if(accs.isEmpty())
                {
                    adm.sendAdmResultMessage("No accounts!");
                    return true;
                }
                String reason = "";
                long time = 0L;
                if(st.hasMoreTokens())
                    time = Integer.parseInt(st.nextToken()) * 1000L * 60L * 60L * 24L + System.currentTimeMillis();
                if(st.countTokens() >= 1)
                {
                    reason = st.nextToken();
                    while(st.hasMoreTokens())
                        reason += " " + st.nextToken();
                }
                if(AutoBan.addHwidBan(name, hwid, reason, time, adm.getName()))
                    adm.sendAdmResultMessage("You banned " + name + " by hwid.");
                reason = "";
                for(String i : accs)
                {
                    try
                    {
                        LoginServerThread.getInstance().sendPacket(new ChangeAccessLevel(i, -100));
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                    reason += i + " ";
                }
                adm.sendAdmResultMessage("Banned accounts: " + reason);
                accs = null;
                L2PcInstance p = L2World.getInstance().getPlayer(name);
                if(p != null)
                {
                    if(p.isInOfflineMode())
                        p.setOfflineMode(false);
                    p.kick(true);
                }
            }
        }
        else if(command.startsWith("admin_hban"))
        {
            L2Object t = adm.getTarget();
            if(t != null && t != adm && t.isPlayer())
            {
                StringTokenizer st = new StringTokenizer(command);
                String reason = "";
                long time = 0L;
                if(st.countTokens() > 1)
                {
                    st.nextToken();
                    time = System.currentTimeMillis() + 86400000L * Long.parseLong(st.nextToken());
                    if(st.countTokens() >= 1)
                    {
                        reason = st.nextToken();
                        while(st.hasMoreTokens())
                            reason += " " + st.nextToken();
                    }
                }
                L2PcInstance p = (L2PcInstance) t;
                if(AutoBan.addHwidBan(p.getName(), p.getHWID(), reason, time, adm.getName()))
                {
                    try
                    {
                        LoginServerThread.getInstance().sendPacket(new ChangeAccessLevel(p.getAccountName(), -100));
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                    adm.sendAdmResultMessage("You banned " + p.getName() + " by hwid and acc [" + p.getAccountName() + "]");
                    p.sendMessage("Admin banned you!");
                    if(p.isInOfflineMode())
                        p.setOfflineMode(false);
                    p.kick(true);
                }
                else
                    adm.sendAdmResultMessage("Impossible!");
            }
            else
                adm.sendAdmResultMessage("Incorrect target.");
        }
        else if(command.startsWith("admin_hnban"))
        {
            StringTokenizer st = new StringTokenizer(command);
            if(st.countTokens() > 1)
            {
                st.nextToken();
                String name = st.nextToken();
                String reason = "";
                long time = 0L;
                if(st.hasMoreTokens())
                    time = System.currentTimeMillis() + 86400000L * Long.parseLong(st.nextToken());
                if(st.countTokens() >= 1)
                {
                    reason = st.nextToken();
                    while(st.hasMoreTokens())
                        reason += " " + st.nextToken();
                }
                L2PcInstance p = L2World.getInstance().getPlayer(name);
                if(p != null)
                {
                    if(AutoBan.addHwidBan(p.getName(), p.getHWID(), reason, time, adm.getName()))
                    {
                        try
                        {
                            LoginServerThread.getInstance().sendPacket(new ChangeAccessLevel(p.getAccountName(), -100));
                        }
                        catch(IOException e)
                        {
                            e.printStackTrace();
                        }
                        adm.sendAdmResultMessage("You banned " + p.getName() + " by hwid and acc [" + p.getAccountName() + "]");
                        p.sendMessage("Admin banned you!");
                        if(p.isInOfflineMode())
                            p.setOfflineMode(false);
                        p.kick(true);
                    }
                    else
                        adm.sendAdmResultMessage("Impossible!");
                }
                else
                {
                    Connect con = null;
                    PreparedStatement statement = null;
                    ResultSet rset = null;
                    try
                    {
                        con = L2DatabaseFactory.get();
                        statement = con.prepareStatement("SELECT LastHWID,char_name,account_name FROM characters WHERE char_name=? LIMIT 1");
                        statement.setString(1, name);
                        rset = statement.executeQuery();
                        if(rset.next())
                        {
                            String hwid = rset.getString("LastHWID");
                            name = rset.getString("char_name");
                            String acc = rset.getString("account_name");
                            if(hwid.isEmpty())
                                adm.sendAdmResultMessage("No char or hwid for name: " + name);
                            else if(AutoBan.addHwidBan(name, hwid, reason, time, adm.getName()))
                            {
                                LoginServerThread.getInstance().sendPacket(new ChangeAccessLevel(acc, -100));
                                adm.sendAdmResultMessage("You banned " + name + " by hwid and acc [" + acc + "]");
                            }
                        }
                        else
                            adm.sendAdmResultMessage("Player " + name + " not exist.");
                    }
                    catch(Exception e)
                    {}
                    finally
                    {
                        Close.CSR(con, statement, rset);
                    }
                }
            }
        }*/

        GMAudit.auditGMAction(adm.getName(), command, player, "");
        return true;
    }

    private void jailOfflinePlayer(L2PcInstance adm, String name, int delay) {
        Connect con = null;
        try {
            con = L2DatabaseFactory.get();

            PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, in_jail=?, jail_timer=? WHERE char_name=?");
            statement.setInt(1, -114356);
            statement.setInt(2, -249645);
            statement.setInt(3, -2984);
            statement.setInt(4, 1);
            statement.setLong(5, delay * 60000L);
            statement.setString(6, name);

            statement.execute();
            int count = statement.getUpdateCount();
            statement.close();

            if (count == 0) {
                adm.sendAdmResultMessage("Character not found!");
            } else {
                adm.sendAdmResultMessage("Character " + name + " jailed for " + (delay > 0 ? delay + " minutes." : "ever!"));
            }
        } catch (SQLException se) {
            adm.sendAdmResultMessage("SQLException while jailing player");
            if (Config.DEBUG) {
                se.printStackTrace();
            }
        } finally {
            try {
                con.close();
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void unjailOfflinePlayer(L2PcInstance adm, String name) {
        Connect con = null;
        try {
            con = L2DatabaseFactory.get();
            PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, in_jail=?, jail_timer=? WHERE char_name=?");
            statement.setInt(1, 17836);
            statement.setInt(2, 170178);
            statement.setInt(3, -3507);
            statement.setInt(4, 0);
            statement.setLong(5, 0);
            statement.setString(6, name);
            statement.execute();
            int count = statement.getUpdateCount();
            statement.close();
            if (count == 0) {
                adm.sendAdmResultMessage("Character not found!");
            } else {
                adm.sendAdmResultMessage("Character " + name + " removed from jail");
            }
        } catch (SQLException se) {
            adm.sendAdmResultMessage("SQLException while jailing player");
            if (Config.DEBUG) {
                se.printStackTrace();
            }
        } finally {
            try {
                con.close();
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }
}
