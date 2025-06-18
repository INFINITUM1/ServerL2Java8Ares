package ru.agecold.gameserver.network.clientpackets;

import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.SkillList;

public class RequestMagicSkillList extends L2GameClientPacket
{
	/**
	 * packet type id 0x38
	 * format:		c
	 * @param rawPacket
	 */

	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

		player.sendSkillList();
	}
}