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
package ru.agecold.gameserver.network.serverpackets;

import ru.agecold.gameserver.model.L2Object;

/**
 * sample
 * 0000: 0c  9b da 12 40                                     ....@
 *
 * format  d
 *
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:40 $
 */
public class Revive extends L2GameServerPacket
{
	private int _objectId;

	public Revive(L2Object obj)
	{
		_objectId = obj.getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x07);
		writeD(_objectId);
	}

	/* (non-Javadoc)
	 * @see ru.agecold.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "S.Revive";
	}

}
