package scripts.ai;

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2MonsterInstance;
import ru.agecold.gameserver.templates.L2NpcTemplate;

public class SuperMob extends L2MonsterInstance {

    public SuperMob(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        return true;
    }

    @Override
    public void deleteMe() {
        super.deleteMe();
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        super.reduceCurrentHp(damage, attacker, awake);
    }
}
