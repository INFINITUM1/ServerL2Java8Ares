package ru.agecold.gameserver.skills.targets;

import javolution.util.FastList;

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;

public class TargetPartyOther extends TargetList {

    public final FastList<L2Object> getTargetList(FastList<L2Object> targets, L2Character activeChar, boolean onlyFirst, L2Character target, L2Skill skill) {
        if (target == null) {
            return targets;
        }

        if (activeChar.equals(target) || target.isDead()) {
            return targets;
        }

        if (activeChar.getParty() != null && activeChar.getParty().getPartyMembers().contains(target)) {
            if (target.isPlayer()) {
                switch (skill.getId()) {
                    case 426:
                        if (!target.isMageClass()) {
                            targets.add(target);
                        }
                        break;
                    case 427:
                        if (target.isMageClass()) {
                            targets.add(target);
                        }
                        break;
                    default:
                        targets.add(target);
                        break;
                }
            } else {
                targets.add(target);
            }
        }

        if (targets.size() == 0) {
            activeChar.sendPacket(Static.TARGET_IS_INCORRECT);
        }

        return targets;
    }
}
