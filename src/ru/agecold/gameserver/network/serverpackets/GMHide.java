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
 * @author Kerberos
 */
public class GMHide extends L2GameServerPacket {
    // cd

    private int _mode;

    /**
     * @param _mode (0 = display windows, 1 = hide windows)
     */
    public GMHide(int mode) {
        _mode = mode;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x8d);
        writeD(_mode);
    }
}
