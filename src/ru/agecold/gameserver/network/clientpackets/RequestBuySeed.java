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

import ru.agecold.Config;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.ItemTable;
import ru.agecold.gameserver.instancemanager.CastleManager;
import ru.agecold.gameserver.instancemanager.CastleManorManager;
import ru.agecold.gameserver.instancemanager.CastleManorManager.SeedProduction;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.actor.instance.L2ManorManagerInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.Castle;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.ActionFailed;
import ru.agecold.gameserver.network.serverpackets.InventoryUpdate;
import ru.agecold.gameserver.network.serverpackets.StatusUpdate;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.L2Item;
import ru.agecold.gameserver.util.Util;

/**
 * Format: cdd[dd]
 * c    // id (0xC4)
 *
 * d    // manor id
 * d    // seeds to buy
 * [
 * d    // seed id
 * d    // count
 * ]
 * @param decrypt
 * @author l3x
 */



public class RequestBuySeed extends L2GameClientPacket
{
	private int _count;
	private int _manorId;
	private int[] _items; // size _count * 2

    @Override
	protected void readImpl()
	{
		_manorId = readD();
		_count = readD();

		if (_count > 500 || _count * 8 < _buf.remaining()) // check values
		{
			_count = 0;
			return;
		}

		_items = new int[_count * 2];

		for (int i = 0; i < _count; i++)
		{
			int itemId = readD();
			_items[i * 2 + 0] = itemId;
			long cnt = readD();
			if (cnt > Integer.MAX_VALUE || cnt < 1)
			{
				_count = 0;
				_items = null;
				return;
			}
			_items[i * 2 + 1] = (int) cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		long totalPrice = 0;
		int slots = 0;
		int totalWeight = 0;

		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if (_count < 1)
		{
			player.sendActionFailed();
			return;
		}

		L2Object target = player.getTarget();

		if (!(target instanceof L2ManorManagerInstance))
			target = player.getLastFolkNPC();

		if (!(target instanceof L2ManorManagerInstance))
			return;

		Castle castle = CastleManager.getInstance().getCastleById(_manorId);

		for (int i = 0; i < _count; i++)
		{
			int seedId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];
			int price = 0;
			int residual = 0;

			SeedProduction seed = castle.getSeed(seedId,CastleManorManager.PERIOD_CURRENT);
			price = seed.getPrice();
			residual = seed.getCanProduce();

			if (price <= 0)
				return;

			if (residual < count)
				return;

			totalPrice += count * price;

			L2Item template = ItemTable.getInstance().getTemplate(seedId);
			totalWeight += count * template.getWeight();
			if (!template.isStackable())
				slots += count;
			else if (player.getInventory().getItemByItemId(seedId) == null)
				slots++;
		}

		if (totalPrice > Integer.MAX_VALUE)
		{
			//Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.",Config.DEFAULT_PUNISH);
			return;
		}

		if (!player.getInventory().validateWeight(totalWeight))
		{
			player.sendPacket(Static.WEIGHT_LIMIT_EXCEEDED);
			return;
		}

		if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(Static.SLOTS_FULL);
			return;
		}

		// Charge buyer
		if ((totalPrice < 0) || !player.reduceAdena("Buy", (int) totalPrice, target, false))
		{
			player.sendPacket(Static.YOU_NOT_ENOUGH_ADENA);
			return;
		}

		// Adding to treasury for Manor Castle
		castle.addToTreasuryNoTax((int) totalPrice);

		// Proceed the purchase
		InventoryUpdate playerIU = new InventoryUpdate();
		for (int i = 0; i < _count; i++)
		{
			int seedId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];
			if (count < 0)
				count = 0;

			// Update Castle Seeds Amount
			SeedProduction seed = castle.getSeed(seedId,
					CastleManorManager.PERIOD_CURRENT);
			seed.setCanProduce(seed.getCanProduce() - count);
			if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				CastleManager.getInstance().getCastleById(_manorId).updateSeed(
						seed.getId(), seed.getCanProduce(),
						CastleManorManager.PERIOD_CURRENT);

			// Add item to Inventory and adjust update packet
			L2ItemInstance item = player.getInventory().addItem("Buy", seedId,
					count, player, target);

			if (item.getCount() > count)
				playerIU.addModifiedItem(item);
			else
				playerIU.addNewItem(item);

			// Send Char Buy Messages
			player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(seedId).addNumber(count));
		}
		// Send update packets
		player.sendPacket(playerIU);

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}
