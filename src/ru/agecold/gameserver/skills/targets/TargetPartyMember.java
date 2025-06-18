package ru.agecold.gameserver.skills.targets;

import javolution.util.FastList;

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

public class TargetPartyMember extends TargetList
{
	public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill)
	{
		if (target == null || !target.isPlayer())
			return targets;

		if (target.isDead())
			return targets;

		if (target.equals(activeChar) || (activeChar.getParty() != null && activeChar.getParty().getPartyMembers().contains(target)))
			targets.add(target);

        if (targets.size() == 0)
            activeChar.sendPacket(Static.TARGET_IS_INCORRECT);

		return targets;
	}
}
