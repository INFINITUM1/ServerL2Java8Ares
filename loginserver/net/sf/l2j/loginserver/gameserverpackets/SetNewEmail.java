package net.sf.l2j.loginserver.gameserverpackets;

import net.sf.l2j.loginserver.clientpackets.ClientBasePacket;

public class SetNewEmail extends ClientBasePacket
{
	private String _account;
	private String _email;

	public SetNewEmail(byte[] decrypt)
	{
		super(decrypt);
		_account = readS();
		_email = readS();
	}

	public String getAccount()
	{
		return _account;
	}

	public String getNewEmail()
	{
		return _email;
	}
}