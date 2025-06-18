package scripts.zone.type;

import ru.agecold.gameserver.model.L2Character;
import scripts.zone.L2ZoneType;

public class L2DropPenaltyZone extends L2ZoneType {
    public L2DropPenaltyZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character) {
        character.setInDropPenaltyZone(true);
    }

    @Override
    protected void onExit(L2Character character) {
        character.setInDropPenaltyZone(false);
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }
}