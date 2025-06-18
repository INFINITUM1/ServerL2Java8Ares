package net.sf.l2j.loginserver.gameserverpackets;

import net.sf.l2j.loginserver.clientpackets.ClientBasePacket;

public class SetHwid extends ClientBasePacket
{
	private String _account;
	private String _hwid;

	public SetHwid(byte[] decrypt)
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