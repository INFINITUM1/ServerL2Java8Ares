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
package ru.agecold.gameserver.network.serverpackets;

/**
 * Format: ch d
 * @author  KenM
 */
public class ExSetCompassZoneCode extends L2GameServerPacket
{
	public static final int SIEGEWARZONE1 = 0x0A;
	public static final int SIEGEWARZONE2 = 0x0B;
	public static final int PEACEZONE = 0x0C;
	public static final int SEVENSIGNSZONE = 0x0D;
	public static final int PVPZONE = 0x0E;
	public static final int GENERALZONE = 0x0F;

	private int _zoneType;

	public ExSetCompassZoneCode(int val)
	{
		_zoneType = val;
	}

	/**
	 * @see ru.agecold.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x32);
		writeD(_zoneType);
	}
}
