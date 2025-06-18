package ru.agecold.gameserver.skills.l2skills;

import javolution.util.FastList;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.NpcTable;
import ru.agecold.gameserver.datatables.SummonItemsData;
import ru.agecold.gameserver.model.*;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PetInstance;
import ru.agecold.gameserver.network.serverpackets.MagicSkillUser;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.gameserver.templates.StatsSet;

public final class L2SkillUnsummonPet extends L2Skill {

    public L2SkillUnsummonPet(StatsSet set) {
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
        if (pet == null || player.isBetrayed()) {
            return;
        }
        //returns pet to control item
        if (pet.isDead()) {
            player.sendPacket(Static.DEAD_PET_CANNOT_BE_RETURNED);
        } else if (pet.isAttackingNow() || pet.isRooted()) {
            player.sendPacket(Static.PET_CANNOT_SENT_BACK_DURING_BATTLE);
        } else {
            // if it is a pet and not a summon
            if (pet.isPet()) {
                L2PetInstance petInst = (L2PetInstance) pet;
                // if the pet is more than 40% fed
                if (petInst.getCurrentFed() > (petInst.getMaxFed() * 0.40)) {
                    pet.unSummon(player);
                } else {
                    player.sendPacket(Static.YOU_CANNOT_RESTORE_HUNGRY_PETS);
                }
            }
        }
    }

    static class PetSummonFeedWait implements Runnable {

        private L2PcInstance _activeChar;
        private L2PetInstance _petSummon;

        PetSummonFeedWait(L2PcInstance activeChar, L2PetInstance petSummon) {
            _activeChar = activeChar;
            _petSummon = petSummon;
        }

        public void run() {
            try {
                if (_petSummon.getCurrentFed() <= 0) {
                    _petSummon.unSummon(_activeChar);
                } else {
                    _petSummon.startFeed(false);
                }
            } catch (Throwable e) {
            }
        }
    }
}
