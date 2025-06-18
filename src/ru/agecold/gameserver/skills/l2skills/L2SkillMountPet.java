package ru.agecold.gameserver.skills.l2skills;

import javolution.util.FastList;
import ru.agecold.Config;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2Summon;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.Ride;
import ru.agecold.gameserver.templates.StatsSet;

public final class L2SkillMountPet extends L2Skill {

    public L2SkillMountPet(StatsSet set) {
        super(set);
    }

    @Override
    public void useSkill(L2Character caster, FastList<L2Object> targets) {
        if (caster.isAlikeDead()) {
            return;
        }
        if (!caster.isPlayer()) {
            return;
        }

        L2PcInstance player = caster.getPlayer();
        L2Summon pet = player.getPet();
        if (pet == null) {
            return;
        }

        player.stopSkillEffects(player.getActiveAug());
        player.broadcastUserInfo();
        Ride mount = new Ride(player.getObjectId(), Ride.ACTION_MOUNT, pet.getTemplate().npcId);
        player.broadcastPacket(mount);
        player.setMountType(mount.getMountType());
        player.setMountObjectID(pet.getControlItemId());
        pet.unSummon(player);
        player.startUnmountPet(Config.MOUNT_EXPIRE);
    }
}
