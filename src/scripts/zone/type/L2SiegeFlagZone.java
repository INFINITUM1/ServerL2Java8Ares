
package scripts.zone.type;

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.L2ZoneType;

public class L2SiegeFlagZone extends L2ZoneType
{
	public L2SiegeFlagZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		character.setInSiegeFlagArea(true);
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		character.setInSiegeFlagArea(false);
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
	
}
