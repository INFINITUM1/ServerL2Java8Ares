package ru.agecold.gameserver.skills.targets;

import javolution.util.FastList;

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;

public class TargetUndead extends TargetList
{
	public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill)
	{
        if (target != null
				&& !target.isDead()
				&& (target.isL2Npc() || target.isSummon())
				&& target.isUndead())
            targets.add(target);
        else
			activeChar.sendPacket(Static.TARGET_IS_INCORRECT);

		return targets;
	}
}
