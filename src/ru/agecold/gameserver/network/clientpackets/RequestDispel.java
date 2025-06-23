package ru.agecold.gameserver.network.clientpackets;

import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

public final class RequestDispel extends L2GameClientPacket
{
	private int skillId;
	private byte[] _data;

    @Override
	protected void readImpl()
    {
	    _data = new byte[1];
	    readB(_data);
	    skillId = readD();
    }

    @Override
	protected void runImpl()
    {
	    L2PcInstance player = getClient().getActiveChar();
	    if(player == null)
	    	return;

	    if(player.isInOlympiadMode())
	    {
		    player.sendActionFailed();
		    return;
	    }

	    L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);
	    if(skill == null)
	    {
		    player.sendActionFailed();
		    return;
	    }

	    if(skill.isSelfDispellable())
	    {
		    player.stopSkillEffects(skillId);
		    player.sendPacket(SystemMessage.id(SystemMessageId.EFFECT_S1_DISAPPEARED).addSkillName(skill.getId(), 1));
	    }
	    else
	    {
	    	player.sendActionFailed();
	    }
    }
}