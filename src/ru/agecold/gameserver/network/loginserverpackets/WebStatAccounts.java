package ru.agecold.gameserver.network.loginserverpackets;

import ru.agecold.Config;
import ru.agecold.gameserver.util.WebStat;

public class WebStatAccounts extends LoginServerBasePacket
{
	private int _count;
	public WebStatAccounts(byte[] decrypt)
	{
		super(decrypt);
		_count = readD();
	}

	public int getCount()
	{
		return _count;
	}
}