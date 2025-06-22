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

import ru.agecold.Config;
import ru.agecold.gameserver.network.serverpackets.ServerClose;
import ru.agecold.gameserver.network.smartguard.SmartGuard;
import ru.agecold.gameserver.network.smartguard.packet.VersionCheck;

/**
 * This class ...
 *
 * @version $Revision: 1.5.2.8.2.8 $ $Date: 2005/04/02 10:43:04 $
 */
public final class ProtocolVersion extends L2GameClientPacket {

    private static final short BasePacketSize = 260;
    private int protocol;
    private boolean hasExtraData = false;

    @Override
    protected void readImpl() {
        protocol = readD();
        if(SmartGuard.isActive() && _buf.remaining() >= 262)
        {
            _buf.position(_buf.position() + BasePacketSize);
            int dataLen = readH();
            if(_buf.remaining() >= dataLen)
            {
                hasExtraData = true;
            }
        }
    }

    @Override
    protected void runImpl() {
        //System.out.println("###" + (_client == null));
        // this packet is never encrypted
        if (protocol == -2) {
            //  if (Config.DEBUG) _log.info("Ping received");
            // this is just a ping attempt from the new C2 client
            getClient().close(ServerClose.STATIC);
            return;
        }

        if (protocol < Config.MIN_PROTOCOL_REVISION || protocol > Config.MAX_PROTOCOL_REVISION) {
            // _log.info("Client: "+getClient().toString()+" -> Protocol Revision: " + protocol + " is invalid. Minimum is "+Config.MIN_PROTOCOL_REVISION+" and Maximum is "+Config.MAX_PROTOCOL_REVISION+" are supported. Closing connection.");
            // _log.warning("Wrong Protocol Version "+protocol);
            //if (Config.SHOW_PROTOCOLprotocolS)
            //System.out.println("#####L2TOP? "+protocol+" FROM"+getClient().toString());
            //System.out.println("##### " + protocol + " FROM" + getClient().toString());

            getClient().close(ServerClose.STATIC);
            return;
            //getClient().closeNow();
            //return;
        }

        if(SmartGuard.isActive() && !hasExtraData)
        {
            getClient().close(ServerClose.STATIC);
            return;
        }

        //System.out.println("##### " + protocol + " FROM" + getClient().toString());
        getClient().setRevision(protocol);
        sendPacket(new VersionCheck(getClient().enableCrypt()));
        return;
    }
}
