package net.sf.l2j.loginserver.gameserverpackets;

import net.sf.l2j.loginserver.clientpackets.ClientBasePacket;

public class SetPhoneNumber extends ClientBasePacket
{
	private String _account;
	private String _phone;

	public SetPhoneNumber(byte[] decrypt)
	{
		super(decrypt);
		_account = readS();
		_phone = readS();
	}

	public String getAccount()
	{
		return _account;
	}

	public String getPhone()
	{
		return _phone;
	}
}