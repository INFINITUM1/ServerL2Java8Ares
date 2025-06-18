package ru.agecold.gameserver.network.loginserverpackets;

import ru.agecold.Config;
import ru.agecold.gameserver.util.WebStat;

public class AcceptPlayer extends LoginServerBasePacket
{
	private String _ip;
	public AcceptPlayer(byte[] decrypt)
	{
		super(decrypt);
		_ip = readS();

		if (Config.WEBSTAT_ENABLE)
			WebStat.getInstance().addLogin(_ip);
	}

	public String getIp()
	{
		return _ip;
	}
}