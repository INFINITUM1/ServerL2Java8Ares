
package scripts.zone.type;

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.base.Race;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import scripts.zone.L2ZoneType;


public class L2ElfTreeZone extends L2ZoneType
{
	public L2ElfTreeZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (character.isPlayer())
		{
			L2PcInstance player = (L2PcInstance)character;

			if (player.getRace() != Race.elf) 
				return;

			player.setInsideZone(L2Character.ZONE_MOTHERTREE, true);
			player.setInElfTree(true);
			player.sendPacket(SystemMessage.id(SystemMessageId.ENTER_SHADOW_MOTHER_TREE));
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if (character.isPlayer() && character.isInsideZone(L2Character.ZONE_MOTHERTREE))
		{
			L2PcInstance player = (L2PcInstance)character;
			
			character.setInsideZone(L2Character.ZONE_MOTHERTREE, false);
			player.setInElfTree(false);
			player.sendPacket(SystemMessage.id(SystemMessageId.EXIT_SHADOW_MOTHER_TREE));
		}
	}

	@Override
	protected void onDieInside(L2Character character) {}

	@Override
	protected void onReviveInside(L2Character character) {}

}
