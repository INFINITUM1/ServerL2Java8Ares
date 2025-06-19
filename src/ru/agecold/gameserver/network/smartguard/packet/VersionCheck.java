package ru.agecold.gameserver.network.smartguard.packet;

import ru.agecold.gameserver.network.serverpackets.*;
import ru.agecold.gameserver.network.smartguard.SmartGuard;
import smartguard.core.properties.GuardProperties;
import smartguard.spi.*;

public class VersionCheck extends L2GameServerPacket
{
	private byte[] _key;

	public VersionCheck(final byte[] key)
	{
		_key = key;
		if(_key != null && SmartGuard.isActive())
			SmartGuardSPI.getSmartGuardService().getLicenseManager().cryptInternalData(_key);
	}

	@Override
	public void writeImpl()
	{
		if(SmartGuard.isActive())
		{
			writeC(0);
			if(_key == null || _key.length == 0)
			{
				writeC(0);
				return;
			}
			writeC(1);
			for(int i = 0; i < 8; ++i)
			{
				writeC(_key[i]);
			}
			writeD(1);
			writeC(0);
		}
		else
		{
			writeC(0x00);
			writeC(0x01);
			writeB(_key);
			writeD(0x01);
			writeD(0x01);
		}
	}
}
