package ru.agecold.gameserver.skills.l2skills;

import javolution.util.FastList;
import ru.agecold.Config;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.NpcTable;
import ru.agecold.gameserver.datatables.SummonItemsData;
import ru.agecold.gameserver.model.*;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PetInstance;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.gameserver.templates.StatsSet;

public final class L2SkillSummonPet extends L2Skill {

    public L2SkillSummonPet(StatsSet set) {
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

        L2PcInstance activeChar = caster.getPlayer();
        L2ItemInstance item = activeChar.getPetSummon();
        if (item == null) {
            return;
        }
        L2SummonItem sitem = SummonItemsData.getInstance().getSummonItem(item.getItemId());

        L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(sitem.getNpcId());
        if (npcTemplate == null) {
            return;
        }

        L2PetInstance petSummon = L2PetInstance.spawnPet(npcTemplate, activeChar, item);
        if (petSummon == null) {
            return;
        }

        petSummon.setTitle(activeChar.getName());

        if (!petSummon.isRespawned()) {
            petSummon.setCurrentHp(petSummon.getMaxHp());
            petSummon.setCurrentMp(petSummon.getMaxMp());
            petSummon.getStat().setExp(petSummon.getExpForThisLevel());
            petSummon.setCurrentFed(petSummon.getMaxFed());
        }

        petSummon.setRunning();

        if (!petSummon.isRespawned()) {
            petSummon.store();
        }

        activeChar.setPet(petSummon);
        activeChar.sendPacket(Static.SUMMON_A_PET);
        L2World.getInstance().storeObject(petSummon);
        petSummon.spawnMe(activeChar.getX() + 50, activeChar.getY() + 100, activeChar.getZ());
        petSummon.startFeed(false);
        petSummon.setFollowStatus(true);
        petSummon.setShowSummonAnimation(false);
        item.setEnchantLevel(petSummon.getLevel());

        if (petSummon.getCurrentFed() <= 0) {
            ThreadPoolManager.getInstance().scheduleAi(new L2SkillSummonPet.PetSummonFeedWait(activeChar, petSummon), 60000, true);
        } else {
            petSummon.startFeed(false);
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
