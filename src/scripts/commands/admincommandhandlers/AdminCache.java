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

import java.io.File;

import ru.agecold.Config;
import ru.agecold.gameserver.cache.CrestCache;
import ru.agecold.gameserver.cache.HtmCache;
import scripts.commands.IAdminCommandHandler;
import ru.agecold.gameserver.model.GMAudit;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Layanere
 *
 */
public class AdminCache implements IAdminCommandHandler
{
	private static final int   REQUIRED_LEVEL  = Config.GM_CACHE;
	private static final String[] ADMIN_COMMANDS  =
		{
		"admin_cache_htm_rebuild",
		"admin_cache_htm_reload",
		"admin_cache_reload_path",
		"admin_cache_reload_file",
		"admin_cache_crest_rebuild",
		"admin_cache_crest_reload",
		"admin_cache_crest_fix"
		};

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (!Config.ALT_PRIVILEGES_ADMIN)
			if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) return false;

		if (command.startsWith("admin_cache_htm_rebuild") || command.equals("admin_cache_htm_reload"))
		{
			HtmCache.getInstance().reload(Config.DATAPACK_ROOT);
			activeChar.sendAdmResultMessage("Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage()  + " MB on " + HtmCache.getInstance().getLoadedFiles() + " file(s) loaded.");
		}
		else if (command.startsWith("admin_cache_reload_path "))
		{
			try
			{
				String path = command.split(" ")[1];
				HtmCache.getInstance().reloadPath(new File(Config.DATAPACK_ROOT, path));
				activeChar.sendAdmResultMessage("Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage()  + " MB in " + HtmCache.getInstance().getLoadedFiles() + " file(s) loaded.");
			}
			catch (Exception e)
			{
				activeChar.sendAdmResultMessage("Usage: //cache_reload_path <path>");
			}
		}
		else if (command.startsWith("admin_cache_reload_file "))
		{
			try
			{
				String path = command.split(" ")[1];
				if (HtmCache.getInstance().loadFile(new File(Config.DATAPACK_ROOT,path)) != null)
				{
					activeChar.sendAdmResultMessage("Cache[HTML]: file was loaded");
				}
				else
				{
					activeChar.sendAdmResultMessage("Cache[HTML]: file can't be loaded");
				}
			}
			catch (Exception e)
			{
				activeChar.sendAdmResultMessage("Usage: //cache_reload_file <relative_path/file>");
			}
		}
		else if (command.startsWith("admin_cache_crest_rebuild") || command.startsWith("admin_cache_crest_reload"))
		{
			CrestCache.getInstance().load();
			activeChar.sendAdmResultMessage("Cache[Crest]: " + String.format("%.3f",CrestCache.getInstance().getMemoryUsage())  + " megabytes on " + CrestCache.getInstance().getLoadedFiles() + " files loaded");
		}
		else if (command.startsWith("admin_cache_crest_fix"))
		{
			CrestCache.getInstance().check();
			activeChar.sendAdmResultMessage("Cache[Crest]: crests fixed");
		}
		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");
		return true;
	}

	private boolean checkLevel(int level)
	{
		return (level >= REQUIRED_LEVEL);
	}

}
