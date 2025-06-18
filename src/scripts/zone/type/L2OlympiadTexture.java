
package scripts.zone.type;

import ru.agecold.gameserver.instancemanager.EventManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
//import ru.agecold.gameserver.model.olympiad.Olympiad;
import scripts.zone.L2ZoneType;

public class L2OlympiadTexture extends L2ZoneType
{
	private int _stadiumId;

	public L2OlympiadTexture(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		/*character.setInsideZone(L2Character.ZONE_PVP, false);
		if (character.isPlayer())
		{	
			L2PcInstance player = (L2PcInstance)character;
			player.sendMessage("ololo!!");
			
			EventManager.getInstance().onTexture(player);
			// handle removal from olympiad game
			if (Olympiad.getInstance().isRegistered(player) || player.getOlympiadGameId() != -1) 
				Olympiad.getInstance().removeDisconnectedCompetitor(player);
		}*/
	}

	@Override
	protected void onExit(L2Character character)
	{
		//character.setInsideZone(L2Character.ZONE_PVP, false);
	}
	
	@Override
	protected void onDieInside(L2Character character) {}

	@Override
	protected void onReviveInside(L2Character character) {}

	/**
	 * Returns this zones stadium id (if any)
	 * @return
	 */
	public int getStadiumId()
	{
		return _stadiumId;
	}
}
