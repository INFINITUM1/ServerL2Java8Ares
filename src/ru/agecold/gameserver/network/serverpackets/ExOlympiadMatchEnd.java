/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.agecold.gameserver.network.serverpackets;

/**
 *
 * @author GodKratos
 */
public class ExOlympiadMatchEnd extends L2GameServerPacket {

    public ExOlympiadMatchEnd() {

    }

    @Override
    protected final void writeImpl() {
        writeC(0xFE);
        writeH(0x2D);
    }
}
