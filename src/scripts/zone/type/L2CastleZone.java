/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package scripts.zone.type;

import javolution.util.FastList;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.MapRegionTable;
import ru.agecold.gameserver.instancemanager.CastleManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2SiegeSummonInstance;
import ru.agecold.gameserver.model.entity.Castle;
import scripts.zone.L2ZoneType;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

/**
 * A castle zone
 *
 * @author  durgus
 */
public class L2CastleZone extends L2ZoneType
{
	private int _castleId;
	private Castle _castle;
	private int[] _spawnLoc;

	public L2CastleZone(int id)
	{
		super(id);

		_spawnLoc = new int[3];
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);

			// Register self to the correct castle
			_castle = CastleManager.getInstance().getCastleById(_castleId);
			_castle.setZone(this);
		}
		else if (name.equals("spawnX"))
		{
			_spawnLoc[0] = Integer.parseInt(value);
		}
		else if (name.equals("spawnY"))
		{
			_spawnLoc[1] = Integer.parseInt(value);
		}
		else if (name.equals("spawnZ"))
		{
			_spawnLoc[2] = Integer.parseInt(value);
		}
		else super.setParameter(name, value);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		character.setInCastleZone(true);
		if (_castle.getSiege().getIsInProgress())
		{
			character.setInsideZone(L2Character.ZONE_PVP, true);
			character.setInsideZone(L2Character.ZONE_SIEGE, true);
			character.sendPacket(Static.ENTERED_COMBAT_ZONE);
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInCastleZone(false);
		if (_castle.getSiege().getIsInProgress())
		{
			character.setInsideZone(L2Character.ZONE_PVP, false);
			character.setInsideZone(L2Character.ZONE_SIEGE, false);
			character.sendPacket(Static.LEFT_COMBAT_ZONE);

			if (character.getPvpFlag() == 0)
				character.startPvPFlag();
		}

		if (character instanceof L2SiegeSummonInstance)
			((L2SiegeSummonInstance)character).unSummon(((L2SiegeSummonInstance)character).getOwner());
	}

	@Override
	protected void onDieInside(L2Character character) {}

	@Override
	protected void onReviveInside(L2Character character) {}

	public void updateZoneStatusForCharactersInside()
	{
		if (_castle.getSiege().getIsInProgress())
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					onEnter(character);
				}
				catch (NullPointerException e) {}
			}
		}
		else
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					onExit(character);
				}
				catch (NullPointerException e) {}
			}
		}
	}

	/**
	 * Removes all foreigners from the castle
	 * @param owningClanId
	 */
	public void banishForeigners(int owningClanId)
	{
		for (L2Character temp : _characterList.values())
		{
			if(!(temp.isPlayer())) 
				continue;
			
			if (temp.getClanId() == owningClanId) 
				continue;

			temp.teleToLocation(MapRegionTable.TeleportWhereType.Town);
		}
	}

	/**
	 * Sends a message to all players in this zone
	 * @param message
	 */
	public void announceToPlayers(String message)
	{
		for (L2Character temp : _characterList.values())
		{
			temp.sendMessage(message);
		}
	}
	
	//системные сообщения
	public void announceSmToPlayers(SystemMessage sm)
	{
		for (L2Character temp : _characterList.values())
		{
			temp.sendPacket(sm);
		}
	}

	/**
	 * Returns all players within this zone
	 * @return
	 */
	public FastList<L2PcInstance> getAllPlayers()
	{
		FastList<L2PcInstance> players = new FastList<L2PcInstance>();

		for (L2Character temp : _characterList.values())
		{
			if (temp.isPlayer())
				players.add((L2PcInstance)temp);
		}

		return players;
	}
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	private final Castle getCastle()
	{
		if (_castle == null)
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		return _castle;
	}

	/**
	 * Get the castles defender spawn
	 * @return
	 */
	public int[] getSpawn()
	{
		return _spawnLoc;
	}
}
