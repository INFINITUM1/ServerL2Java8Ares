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

import ru.agecold.gameserver.model.actor.instance.L2PcInstance;


/**
 *
 * 0000: 76  7a 07 80 49  ea 01 00 00  c1 37 fe    uz..Ic'.J.....7. <p>
 * 0010: ff 9e c3 03 00 8f f3 ff ff                         .........<p>
 * <p>
 *
 * format   dddddd      (player id, target id, distance, startx, starty, startz)<p>
 *
 *
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class MoveOnVehicle extends L2GameServerPacket
{
    private int _id;
    private int _x, _y, _z;
    private L2PcInstance _activeChar;

    public MoveOnVehicle(int vehicleID, L2PcInstance player, int x, int y, int z)
    {
        _id = vehicleID;
        _activeChar = player;
        _x = x;
        _y = y;
        _z = z;
    }

    @Override
	protected final void writeImpl()
    {
        writeC(0x71);

        writeD(_activeChar.getObjectId());
        writeD(_id);
        writeD(_x);
        writeD(_y);
        writeD(_z);
        writeD(_activeChar.getX());
        writeD(_activeChar.getY());
        writeD(_activeChar.getZ());
    }
}