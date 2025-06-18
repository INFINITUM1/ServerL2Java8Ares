package ru.agecold.gameserver.skills.targets;

import javolution.util.FastList;

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;

public class TargetMobCorpseArea extends TargetList
{
	public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill)
	{
        if (target == null || !target.isMonster() || !target.isDead())
        {
            activeChar.sendPacket(Static.TARGET_IS_INCORRECT);
            return targets;
        }

        if (onlyFirst)
		{
            targets.add(target);
            return targets;
		}
		targets.add(target);

		FastList<L2Character> objs = activeChar.getKnownList().getKnownCharactersInRadius(skill.getSkillRadius());
		if (objs == null || objs.isEmpty())
			return targets;

		L2Character cha = null;
		for (FastList.Node<L2Character> n = objs.head(), end = objs.tail(); (n = n.getNext()) != end;)
		{
			cha = n.getValue();
			if (cha == null)
				continue;

            if (!cha.isMonster())
				continue;

            if (activeChar.equals(cha) || cha.isDead())
				continue;

			if (!activeChar.canSeeTarget(cha))
				continue;

            targets.add(cha);
		}
		return targets;
	}
}
