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

import java.util.List;

/**
 * Format: (ch) d[d]
 *
 * @author  -Wooden-
 */
public class ExCursedWeaponList extends L2GameServerPacket
{
	private List<Integer> _cursedWeaponIds;

	public ExCursedWeaponList(List<Integer> cursedWeaponIds)
	{
		_cursedWeaponIds = cursedWeaponIds;
	}

	/**
	 * @see ru.agecold.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x45);

		writeD(_cursedWeaponIds.size());
		for(Integer i : _cursedWeaponIds)
		{
			writeD(i.intValue());
		}
		//_cursedWeaponIds.clear();
	}
	
	@Override
	public void gcb()
	{
		_cursedWeaponIds.clear();
		//_cursedWeaponIds = null;
	}
}