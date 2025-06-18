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
package ru.agecold.gameserver.network.clientpackets;

import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.*;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.2.2.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestGMCommand extends L2GameClientPacket
{
	static Logger _log = Logger.getLogger(RequestGMCommand.class.getName());

	private String _targetName;
	private int _command;


	@Override
	protected void readImpl()
	{
		_targetName = readS();
		_command    = readD();
		//_unknown  = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		    return;
		// prevent non gm or low level GMs from vieweing player stuff
		if (!player.isGM() || player.getAccessLevel() < Config.GM_ALTG_MIN_LEVEL)
			return;
		
		if (player.isParalyzed())
			return;

		L2PcInstance target = L2World.getInstance().getPlayer(_targetName);

		// target name was incorrect?
		if (target == null)
			return;

		switch(_command)
		{
			case 1: // target status
				player.sendPacket(new GMViewCharacterInfo(target));
				player.sendPacket(new GMHennaInfo(target));
				break;
			case 2: // target clan
				if (target.getClan() != null)
					player.sendPacket(new GMViewPledgeInfo(target.getClan(),target));
				break;
			case 3: // target skills
				player.sendPacket(new GMViewSkillInfo(target));
				break;
			case 4: // target quests
				player.sendPacket(new GMViewQuestList(target));
				break;
			case 5: // target inventory
				player.sendPacket(new GMViewItemList(target));
				player.sendPacket(new GMHennaInfo(target));
				break;
			case 6: // target warehouse
				player.sendPacket(new GMViewWarehouseWithdrawList(target));
				break;
		}
	}
}
