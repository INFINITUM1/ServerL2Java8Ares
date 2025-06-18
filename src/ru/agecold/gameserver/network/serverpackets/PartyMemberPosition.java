package ru.agecold.gameserver.network.serverpackets;

import javolution.util.FastMap;

import ru.agecold.gameserver.model.L2Party;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.L2GameClient;
import ru.agecold.util.Location;

public class PartyMemberPosition extends L2GameServerPacket
{
	private FastMap<Integer, Location> _positions = new FastMap<Integer, Location>();
	public PartyMemberPosition(FastMap<Integer, Location> positions)
	{
		if (positions != null)
			_positions = positions;
	}

	@Override
	protected void writeImpl()
	{
		L2GameClient client = getClient();
		if(client == null || _positions.isEmpty())
			return;

		L2PcInstance player = client.getActiveChar();
		if(player == null)
			return;

		int objId = player.getObjectId();
		int sz = _positions.containsKey(objId) ? _positions.size() - 1 : _positions.size();
		if(sz < 1)
			return;

		writeC(0xa7);
		writeD(sz);

		for (FastMap.Entry<Integer, Location> e = _positions.head(), end = _positions.tail(); (e = e.getNext()) != end;) 
		{
			Integer id = e.getKey(); // No typecast necessary.
			Location loc = e.getValue(); // No typecast necessary.
			if (id == null || loc == null)
				continue;

			if (id == objId)
				continue;

			writeD(id);
			writeD(loc.x);
			writeD(loc.y);
			writeD(loc.z);
		}
	}
}
