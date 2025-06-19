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

import ru.agecold.Config;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import scripts.commands.IAdminCommandHandler;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

import java.io.File;

/**
 * This class handles following admin commands:
 * - announce text = announces text to all players
 * - list_announcements = show menu
 * - reload_announcements = reloads announcements from txt file
 * - announce_announcements = announce all stored announcements to all players
 * - add_announcement text = adds text to startup announcements
 * - del_announcement id = deletes announcement with respective id
 *
 * @version $Revision: 1.4.4.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminBanMenu implements IAdminCommandHandler {

	private static final String[] ADMIN_COMMANDS = {

			};
	private static final int REQUIRED_LEVEL = Config.GM_BAN;

	public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN)
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM()))
            	return false;

		if (command.equals("admin_guard"))
		{
			sendMainPage(activeChar);
		}


		return true;
	}

	private void sendMainPage(L2PcInstance player)
	{
		//sendHTML(player, buildPage(SmartTemplate.PageMain.load()));
	}

	private void sendHTML(L2PcInstance player, String text)
	{
		if (player != null) {
			sendHtml(player, text);
		}
	}

	private void sendHtml(L2PcInstance player, String text)
	{
		player.sendPacket(new NpcHtmlMessage(0).setHtml(text));
	}

	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private boolean checkLevel(int level) {
		return (level >= REQUIRED_LEVEL);
	}

}
