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
import ru.agecold.gameserver.ai.CtrlEvent;
import ru.agecold.gameserver.model.L2CharPosition;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class CannotMoveAnymore extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(CannotMoveAnymore.class.getName());

	private int _x;
	private int _y;
	private int _z;
	private int _heading;

	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_heading = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Character player = getClient().getActiveChar();
		if (player == null)
			return;

		//if (Config.DEBUG)
		//	_log.fine("client: x:" + _x + " y:" + _y + " z:" + _z
		//			+ " server x:" + player.getX() + " y:" + player.getY()
		//			+ " z:" + player.getZ());
		if (player.getAI() != null)
			player.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED_BLOCKED, new L2CharPosition(_x, _y, _z, _heading));

		// player.stopMove();
		//
		// if (Config.DEBUG)
		// _log.fine("client: x:"+_x+" y:"+_y+" z:"+_z+
		// " server x:"+player.getX()+" y:"+player.getZ()+" z:"+player.getZ());
		// StopMove smwl = new StopMove(player);
		// getClient().getActiveChar().sendPacket(smwl);
		// getClient().getActiveChar().broadcastPacket(smwl);
		//
		// StopRotation sr = new StopRotation(getClient().getActiveChar(),
		// _heading);
		// getClient().getActiveChar().sendPacket(sr);
		// getClient().getActiveChar().broadcastPacket(sr);
	}
}
