/*
 * This program is free software; you can redistribute it and/or modify
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
package ru.agecold.gameserver.network.serverpackets;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import ru.agecold.Config;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.actor.instance.L2MerchantInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.3.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class SellList extends L2GameServerPacket
{
	private static Logger _log = Logger.getLogger(SellList.class.getName());
	private final L2PcInstance _activeChar;
	private final L2MerchantInstance _lease;
	private int _money;
	private List<L2ItemInstance> _selllist = new FastList<L2ItemInstance>();

	public SellList(L2PcInstance player)
	{
		_activeChar = player;
		_lease = null;
		_money = _activeChar.getAdena();
		doLease();
	}

	public SellList(L2PcInstance player, L2MerchantInstance lease)
	{
		_activeChar = player;
		_lease = lease;
		_money = _activeChar.getAdena();
		doLease();
	}

	private void doLease()
	{
		if (_lease == null)
		{
			for (L2ItemInstance item : _activeChar.getInventory().getItems())
			{
				if (!item.isEquipped() &&                                                      // Not equipped
                        item.getItem().isSellable() &&                                         // Item is sellable
                        (_activeChar.getPet() == null ||                                             // Pet not summoned or
                                item.getObjectId() != _activeChar.getPet().getControlItemId()))      // Pet is summoned and not the item that summoned the pet
				{
					_selllist.add(item);
					//if (Config.DEBUG)
					//	_log.fine("item added to selllist: " + item.getItem().getName());
				}
			}
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x10);
		writeD(_money);
		writeD(_lease == null ? 0x00 : 1000000 + _lease.getTemplate().npcId);

		writeH(_selllist.size());

		for (L2ItemInstance item : _selllist)
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(0x00);
			writeH(0x00);

			if (_lease == null)
				writeD(item.getItem().getReferencePrice()/2); // wtf??? there is no conditional part in SellList!! this d should allways be here 0.o! fortunately the lease stuff are never ever use so the if allways exectues
		}
	}
	
	@Override
	public void gc()
	{
		_selllist.clear();
		_selllist = null;
	}

	/* (non-Javadoc)
	 * @see ru.agecold.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "S.SellList";
	}
}
