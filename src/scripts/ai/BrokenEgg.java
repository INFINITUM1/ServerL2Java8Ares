package scripts.ai;

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import scripts.autoevents.brokeneggs.BrokenEggs;

public class BrokenEgg extends L2GrandBossInstance {

    public BrokenEgg(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        if (!BrokenEggs.getEvent().canAttackEgg()) {
            attacker.sendActionFailed();
            return;
        }
        super.reduceCurrentHp(damage, attacker, awake);
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        BrokenEggs.getEvent().notifyEggDie(killer.getPlayer());
        return true;
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
    }
}
