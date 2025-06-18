/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.agecold.gameserver.network.gameserverpackets;

import java.io.IOException;
/**
 *
 * @author Администратор
 */
public class SetNewEmail extends GameServerBasePacket {
    public SetNewEmail(String player, String email) {
        writeC(0xbf);
        writeS(player);
        writeS(email);
    }

    /* (non-Javadoc)
     * @see ru.agecold.gameserver.gameserverpackets.GameServerBasePacket#getContent()
     */
    @Override
    public byte[] getContent() throws IOException {
        return getBytes();
    }
}
