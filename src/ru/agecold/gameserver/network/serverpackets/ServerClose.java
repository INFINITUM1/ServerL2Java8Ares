package ru.agecold.gameserver.network.serverpackets;

/**
 *
 * @author  devScarlet & mrTJO
 */
public class ServerClose extends L2GameServerPacket
{
	/**
	 * @see ru.agecold.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0x26);
	}
}
