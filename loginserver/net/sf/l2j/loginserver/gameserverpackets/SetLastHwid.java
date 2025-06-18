package net.sf.l2j.loginserver.gameserverpackets;

import net.sf.l2j.loginserver.clientpackets.ClientBasePacket;

public class SetLastHwid extends ClientBasePacket
{
	private String _account;
	private String _hwid;

	public SetLastHwid(byte[] decrypt)
	{
		super(decrypt);
		_account = readS();
		_hwid = readS();
	}

	public String getAccount()
	{
		return _account;
	}

	public String getHwid()
	{
		return _hwid;
	}
}