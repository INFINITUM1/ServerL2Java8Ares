package ru.agecold.gameserver.network.smartguard.packet;

import ru.agecold.gameserver.network.serverpackets.*;

import java.nio.*;

public class RawPacket extends L2GameServerPacket
{
	byte[] data;

	public RawPacket(final ByteBuffer byteBuffer)
	{
		byteBuffer.get(data = new byte[byteBuffer.remaining()], 0, data.length);
	}

	@Override
	protected void writeImpl()
	{
		writeB(data);
	}
}
