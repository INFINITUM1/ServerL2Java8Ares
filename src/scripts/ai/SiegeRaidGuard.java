package scripts.ai;

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2RaidBossInstance;
import ru.agecold.gameserver.templates.L2NpcTemplate;

public final class SiegeRaidGuard extends L2RaidBossInstance {

    public SiegeRaidGuard(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public boolean isRaid() {
        return true;
    }

    @Override
    public boolean isSiegeRaidGuard() {
        return false;
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
         return false;
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        super.reduceCurrentHp(damage, attacker, awake);
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
}
