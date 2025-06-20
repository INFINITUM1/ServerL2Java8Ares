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

import javolution.util.FastList;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.TradeList;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/03/27 15:29:40 $
 */
public class PrivateStoreManageListBuy extends L2GameServerPacket
{
	private L2PcInstance _activeChar;
	private int _playerAdena;
	private L2ItemInstance[] _itemList;
	private FastList<TradeList.TradeItem> _buyList;

	public PrivateStoreManageListBuy(L2PcInstance player)
	{
		_activeChar = player;
		_playerAdena = _activeChar.getAdena();
		_itemList = _activeChar.getInventory().getUniqueItems(false,true);
		_buyList = _activeChar.getBuyList().getItems();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xb7);
		//section 1
		writeD(_activeChar.getObjectId());
		writeD(_playerAdena);

		//section2
		writeD(_itemList.length); // inventory items for potential buy
		for (L2ItemInstance item : _itemList)
		{
			if (item.isAugmented())
				continue;
		
			writeD(item.getItem().getItemId());
			writeH(item.getEnchantLevel()); //show enchant lvl as 0, as you can't buy enchanted weapons
			writeD(item.getCount());
			writeD(item.getReferencePrice());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getItem().getType2());
		}

		//section 3
		writeD(_buyList.size()); //count for all items already added for buy
		for (FastList.Node<TradeList.TradeItem> n = _buyList.head(), end = _buyList.tail(); (n = n.getNext()) != end;) 
		{
			TradeList.TradeItem item = n.getValue();
			
			writeD(item.getItem().getItemId());
			writeH(item.getEnchant());
			writeD(item.getCount());
			writeD(item.getItem().getReferencePrice());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getItem().getType2());
			writeD(item.getPrice());//your price
			writeD(item.getItem().getReferencePrice());//fixed store price
		}
	}

	/* (non-Javadoc)
	 * @see ru.agecold.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "S.PrivateSellListBuy";
	}
}
