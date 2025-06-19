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

import ru.agecold.gameserver.network.L2GameClient;

/**
 * This class ...
 *
 * @version $Revision: 1.5.2.1.2.5 $ $Date: 2005/03/27 15:29:30 $
 */
public class CharacterSelected extends L2GameClientPacket {

    private static Logger _log = Logger.getLogger(CharacterSelected.class.getName());
    // cd
    private int _charSlot;
    @SuppressWarnings("unused")
    private int _unk1; 	// new in C4
    @SuppressWarnings("unused")
    private int _unk2;	// new in C4
    @SuppressWarnings("unused")
    private int _unk3;	// new in C4
    @SuppressWarnings("unused")
    private int _unk4;	// new in C4

    @Override
    protected void readImpl() {
        _charSlot = readD();
        _unk1 = readH();
        _unk2 = readD();
        _unk3 = readD();
        _unk4 = readD();
    }

    @Override
    protected void runImpl() {
        L2GameClient client = getClient();
        if(client == null)
            return;
        // if there is a playback.dat file in the current directory, it will
        // be sent to the client instead of any regular packets
        // to make this work, the first packet in the playback.dat has to
        // be a  [S]0x21 packet
        // after playback is done, the client will not work correct and need to exit
        //playLogFile(getConnection()); // try to play log file

        // we should always be abble to acquire the lock
        // but if we cant lock then nothing should be done (ie repeated packet)
        // System.out.println("###1#");
        client.playerSelected(_charSlot);
        //System.out.println("###99#");
    }
}
