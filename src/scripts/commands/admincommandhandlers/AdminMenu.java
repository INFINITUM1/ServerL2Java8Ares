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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.LoginServerThread;
import scripts.commands.IAdminCommandHandler;
import ru.agecold.gameserver.model.GMAudit;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Clan;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles following admin commands:
 * - handles every admin menu command
 *
 * @version $Revision: 1.3.2.6.2.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminMenu implements IAdminCommandHandler
{
	private static final Logger _log = Logger.getLogger(AdminMenu.class.getName());

	private static final String[] ADMIN_COMMANDS = {
		"admin_char_manage",
		"admin_teleport_character_to_menu",
		"admin_recall_char_menu", "admin_recall_party_menu", "admin_recall_clan_menu" ,
		"admin_goto_char_menu",
		"admin_kick_menu",
		"admin_kill_menu",
		"admin_ban_menu",
		"admin_unban_menu"
	};
	private static final int REQUIRED_LEVEL = Config.GM_ACCESSLEVEL;

	public boolean useAdminCommand(String command, L2PcInstance activeChar) {
		if (!Config.ALT_PRIVILEGES_ADMIN)
			if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) return false;

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if (command.equals("admin_char_manage"))
			showMainPage(activeChar);
		else if (command.startsWith("admin_teleport_character_to_menu"))
		{
			String[] data = command.split(" ");
			if(data.length==5)
			{
				String playerName=data[1];
				L2PcInstance player = L2World.getInstance().getPlayer(playerName);
				if(player!=null)
					teleportCharacter(player,Integer.parseInt(data[2]),Integer.parseInt(data[3]),Integer.parseInt(data[4]),activeChar, "Admin is teleporting you.");
			}
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_recall_char_menu"))
		{
			try
			{
				String targetName = command.substring(23);
				L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				teleportCharacter(player,activeChar.getX(),activeChar.getY(),activeChar.getZ(),activeChar, "Admin is teleporting you.");
			}
			catch (StringIndexOutOfBoundsException e){}
		}
		else if (command.startsWith("admin_recall_party_menu"))
		{
			int x=activeChar.getX(), y = activeChar.getY(), z=activeChar.getZ();
			try
			{
				String targetName = command.substring(24);
				L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if(player == null)
				{
					activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
					return true;
				}
				if(!player.isInParty())
				{
					activeChar.sendAdmResultMessage("Player is not in party.");
					teleportCharacter(player,x,y,z,activeChar, "Admin is teleporting you.");
					return true;
				}
				for(L2PcInstance pm : player.getParty().getPartyMembers())
					teleportCharacter(pm, x, y, z, activeChar, "Your party is being teleported by an Admin.");
			}
			catch (Exception e){}
		}
		else if (command.startsWith("admin_recall_clan_menu"))
		{
			int x=activeChar.getX(), y = activeChar.getY(), z=activeChar.getZ();
			try
			{
				String targetName = command.substring(23);
				L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if(player == null)
				{
					activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
					return true;
				}
				L2Clan clan = player.getClan();
				if(clan==null)
				{
					activeChar.sendAdmResultMessage("Player is not in a clan.");
					teleportCharacter(player,x,y,z,activeChar, "Admin is teleporting you.");
					return true;
				}
				L2PcInstance[] members = clan.getOnlineMembers("");
				for(int i = 0; i < members.length; i++)
					teleportCharacter(members[i], x, y, z, activeChar, "Your clan is being teleported by an Admin.");
			}
			catch (Exception e){}
		}
		else if (command.startsWith("admin_goto_char_menu"))
		{
			try
			{
				String targetName = command.substring(21);
				targetName = targetName.replaceAll("&lt;", "<");
				targetName = targetName.replaceAll("&gt;", ">");
				targetName = targetName.replaceAll("&excl;", "!");
				targetName = targetName.replaceAll("&commat;", "@");
				targetName = targetName.replaceAll("&num;", "#");
				targetName = targetName.replaceAll("&dollar;", "$");
				targetName = targetName.replaceAll("&percnt;", "%");
				targetName = targetName.replaceAll("&Hat;", "^");
				targetName = targetName.replaceAll("&ast;", "*");
				targetName = targetName.replaceAll("&lpar;", "(");
				targetName = targetName.replaceAll("&rpar;", ")");
				targetName = targetName.replaceAll("&period;", ".");
				targetName = targetName.replaceAll("&comma;", ",");
				targetName = targetName.replaceAll("&semi;", ";");
				targetName = targetName.replaceAll("&sol;", "/");
				targetName = targetName.replaceAll("&verbar;", "|");
				//targetName = targetName.replaceAll("&bsol;", "\");
				targetName = targetName.replaceAll("&quest;", "?");
				targetName = targetName.replaceAll("&plus;", "+");
				targetName = targetName.replaceAll("&colon;", ":");
				targetName = targetName.replaceAll("&apos;", "'");
				targetName = targetName.replaceAll("&lowbar;", "_");
				L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				teleportToCharacter(activeChar, player);
			}
			catch (StringIndexOutOfBoundsException e){}
		}
		else if (command.equals("admin_kill_menu"))
		{
			handleKill(activeChar);
		}
		else if (command.startsWith("admin_kick_menu"))
		{
			StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				String player = st.nextToken();
				L2PcInstance plyr = L2World.getInstance().getPlayer(player);
				SystemMessage sm = SystemMessage.id(SystemMessageId.S1_S2);
				if (plyr != null)
				{
					plyr.logout();
					sm.addString("You kicked " + plyr.getName() + " from the game.");
				}
				else
					sm.addString("Player " + player + " was not found in the game.");
				activeChar.sendPacket(sm);
			}
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_ban_menu"))
		{
			StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				String player = st.nextToken();
				L2PcInstance plyr = L2World.getInstance().getPlayer(player);
				if (plyr != null)
				{
					plyr.logout();
				}
				setAccountAccessLevel(player, activeChar, -100);
			}
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_unban_menu"))
		{
			StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				String player = st.nextToken();
				setAccountAccessLevel(player, activeChar, 0);
			}
			showMainPage(activeChar);
		}
		return true;
	}
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	private boolean checkLevel(int level)
	{
		return (level >= REQUIRED_LEVEL);
	}
	private void handleKill(L2PcInstance activeChar)
	{
		handleKill(activeChar, null);
	}
	private void handleKill(L2PcInstance activeChar, String player) {
		L2Object obj = activeChar.getTarget();
		L2Character target = (L2Character)obj;
		String filename = "main_menu.htm";
		if (player != null)
		{
			L2PcInstance plyr = L2World.getInstance().getPlayer(player);
			if (plyr != null)
				target = plyr;
			activeChar.sendAdmResultMessage("You killed " + plyr.getName());
		}
		if (target != null)
		{
			if (target.isPlayer())
			{
				target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar);
				filename = "charmanage.htm";
			}
			else if (Config.L2JMOD_CHAMPION_ENABLE && target.isChampion())
				target.reduceCurrentHp(target.getMaxHp()*Config.L2JMOD_CHAMPION_HP + 1, activeChar);
			else
				target.reduceCurrentHp(target.getMaxHp() + 1, activeChar);
		}
		else
		{
			activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
		}
		AdminHelpPage.showHelpPage(activeChar, filename);
	}

	private void teleportCharacter(L2PcInstance player, int x, int y, int z, L2PcInstance activeChar, String message)
	{
		if (player != null)
		{
			player.sendMessage(message);
			player.teleToLocation(x, y, z, true);
		}
		showMainPage(activeChar);
	}
	private void teleportToCharacter(L2PcInstance activeChar, L2Object target)
	{
		L2PcInstance player = null;
		if (target != null && target.isPlayer())
			player = (L2PcInstance)target;
		else
		{
			activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		if (player.getObjectId() == activeChar.getObjectId())
			player.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_USE_ON_YOURSELF));
		else
		{
			activeChar.teleToLocation(player.getX(), player.getY(), player.getZ(), true);
			activeChar.sendAdmResultMessage("You're teleporting yourself to character " + player.getName());
		}
		showMainPage(activeChar);
	}
	/**
	 * @param activeChar
	 */
	private void showMainPage(L2PcInstance activeChar)
	{
		AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
	}

	private void setAccountAccessLevel(String player, L2PcInstance activeChar, int banLevel)
	{
		ru.agecold.mysql.Connect con = null;
		try
		{
			con = L2DatabaseFactory.get();
			String stmt = "SELECT account_name FROM characters WHERE char_name = ?";
			PreparedStatement statement = con.prepareStatement(stmt);
			statement.setString(1, player);
			ResultSet result = statement.executeQuery();
			if (result.next())
			{
				String acc_name = result.getString(1);
				SystemMessage sm = SystemMessage.id(SystemMessageId.S1_S2);
				if(acc_name.length() > 0)
				{
					LoginServerThread.getInstance().sendAccessLevel(acc_name, banLevel);
					sm.addString("Account Access Level for "+player+" set to "+banLevel+".");
				}
				else
					sm.addString("Couldn't find player: "+player+".");
				activeChar.sendPacket(sm);
			}
			else
				activeChar.sendAdmResultMessage("Specified player name didn't lead to a valid account.");
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not set accessLevel:"+e);
			if (Config.DEBUG)
				e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e) {}
		}
	}
}
