package ru.agecold.gameserver.network.serverpackets;

import ru.agecold.gameserver.GameTimeController;

public class ClientSetTime extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xec);
		writeD(GameTimeController.getInstance().getGameTime()); // time in client minutes
		writeD(6); //constant to match the server time( this determines the speed of the client clock)
	}
}