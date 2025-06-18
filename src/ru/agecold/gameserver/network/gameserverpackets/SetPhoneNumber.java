package ru.agecold.gameserver.network.gameserverpackets;

import java.io.IOException;

public class SetPhoneNumber extends GameServerBasePacket
{
    public SetPhoneNumber(String account, String phone)
    {
        writeC(0xCA);
        writeS(account);
        writeS(phone);
    }

    @Override
    public byte[] getContent() throws IOException
    {
        return getBytes();
    }
}