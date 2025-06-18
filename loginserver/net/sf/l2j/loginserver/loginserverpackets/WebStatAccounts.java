package net.sf.l2j.loginserver.loginserverpackets;

import java.io.IOException;

import net.sf.l2j.loginserver.serverpackets.ServerBasePacket;

public class WebStatAccounts extends ServerBasePacket
{
	public WebStatAccounts(int count)
	{
		writeC(0xbe);
		writeD(count);
	}

	@Override
	public byte[] getContent() throws IOException
	{
		return getBytes();
	}
}