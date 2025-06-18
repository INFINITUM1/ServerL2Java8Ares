package net.sf.l2j.loginserver.gameserverpackets;

import net.sf.l2j.loginserver.clientpackets.ClientBasePacket;

public class SetNewPassword extends ClientBasePacket
{
	private String _account;
	private String _pwd;

	public SetNewPassword(byte[] decrypt)
	{
		super(decrypt);
		_account = readS();
		_pwd = readS();
	}

	public String getAccount()
	{
		return _account;
	}

	public String getNewPassword()
	{
		return _pwd;
	}
}