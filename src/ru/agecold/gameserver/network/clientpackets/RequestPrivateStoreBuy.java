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
package ru.agecold.gameserver.network.clientpackets;

import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.ItemRequest;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.TradeList;
import ru.agecold.gameserver.model.TradeList.TradeItem;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.ActionFailed;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.1.2.5 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPrivateStoreBuy extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPrivateStoreBuy.class.getName());

	private int _storePlayerId;
	private int _count;
	private ItemRequest[] _items;

	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		_count = readD();
		// count*12 is the size of a for iteration of each item
        if (_count < 0  || _count * 12 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
        {
            _count = 0;
        }
		_items = new ItemRequest[_count];


		for (int i = 0; i < _count ; i++)
		{
			int objectId = readD();
			long count   = readD();
			if (count > Integer.MAX_VALUE) count = Integer.MAX_VALUE;
			int price    = readD();

			_items[i] = new ItemRequest(objectId, (int)count, price);
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null) 
			return;
		
		if (player.isParalyzed())
			return;

		L2Object object = L2World.getInstance().findObject(_storePlayerId);
		if (object == null || !(object.isPlayer())) return;

		L2PcInstance storePlayer = object.getPlayer();
		if (!(storePlayer.getPrivateStoreType() == L2PcInstance.PS_SELL || storePlayer.getPrivateStoreType() == L2PcInstance.PS_PACKAGE_SELL)) return;

		TradeList storeList = storePlayer.getSellList();
		if (storeList == null) return;
		
		if (!player.isInsideRadius(storePlayer, 120, false, false))
			return;
		
        // FIXME: this check should be (and most probabliy is) done in the TradeList mechanics
		long priceTotal = 0;
        for(ItemRequest ir : _items)
        {
			if (ir.getCount() > Integer.MAX_VALUE || ir.getCount() < 0)
			{
	            //String msgErr = "[RequestPrivateStoreBuy] player "+getClient().getActiveChar().getName()+" tried an overflow exploit, ban this player!";
	            //Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
			    return;
			}
			TradeItem sellersItem = storeList.getItem(ir.getObjectId());
			if(sellersItem == null)
			{
	            //String msgErr = "[RequestPrivateStoreBuy] player "+getClient().getActiveChar().getName()+" tried to buy an item not sold in a private store (buy), ban this player!";
	            //Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
			    return;
			}
			if(ir.getPrice() != sellersItem.getPrice())
			{
	            //String msgErr = "[RequestPrivateStoreBuy] player "+getClient().getActiveChar().getName()+" tried to change the seller's price in a private store (buy), ban this player!";
	           // Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
			    return;
			}
			priceTotal += ir.getPrice() * ir.getCount();
        }

        // FIXME: this check should be (and most probabliy is) done in the TradeList mechanics
		if(priceTotal < 0 || priceTotal > Integer.MAX_VALUE)
        {
           // String msgErr = "[RequestPrivateStoreBuy] player "+getClient().getActiveChar().getName()+" tried an overflow exploit, ban this player!";
            //Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
            return;
        }

        if (player.getAdena() < priceTotal)
		{
			sendPacket(Static.YOU_NOT_ENOUGH_ADENA);
			player.sendActionFailed();
			return;
		}

        if (storePlayer.getPrivateStoreType() == L2PcInstance.PS_PACKAGE_SELL)
        {
        	if (storeList.getItemCount() > _count)
        	{
        		//String msgErr = "[RequestPrivateStoreBuy] player "+getClient().getActiveChar().getName()+" tried to buy less items then sold by package-sell, ban this player for bot-usage!";
        		//Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
        		return;
        	}
        }

        if (!storeList.PrivateStoreBuy(player, _items, (int) priceTotal))
        {
            player.sendActionFailed();
            _log.warning("PrivateStore buy has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
			storePlayer.logout();
            return;
        }
		
		player.sendChanges();
		storePlayer.saveTradeList();
		if (storeList.getItemCount() == 0)
		{
			if (storePlayer.isInOfflineMode())
			{
				storePlayer.kick();
				return;
			}
			storePlayer.setPrivateStoreType(L2PcInstance.PS_NONE);
			storePlayer.broadcastUserInfo();
		}
		storePlayer.sendChanges();
		player.sendActionFailed();
	}
}
