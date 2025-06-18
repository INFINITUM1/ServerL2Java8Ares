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
package ru.agecold.gameserver.network.clientpackets;

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.ExConfirmVariationGemstone;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.L2Item;

/**
 * Format:(ch) dddd
 * @author  -Wooden-
 */
public final class RequestConfirmGemStone extends L2GameClientPacket
{
	private int _targetItemObjId;
	private int _refinerItemObjId;
	private int _gemstoneItemObjId;
	private int _gemstoneCount;


	/**
	 * @param buf
	 * @param client
	 */
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
		_gemstoneItemObjId = readD();
		_gemstoneCount= readD();
	}

	/**
	 * @see ru.agecold.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected
	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		L2ItemInstance targetItem = (L2ItemInstance)L2World.getInstance().findObject(_targetItemObjId);
		L2ItemInstance refinerItem = (L2ItemInstance)L2World.getInstance().findObject(_refinerItemObjId);
		L2ItemInstance gemstoneItem = (L2ItemInstance)L2World.getInstance().findObject(_gemstoneItemObjId);

		if (targetItem == null || refinerItem == null || gemstoneItem == null) return;

		// Make sure the item is a gemstone
		int gemstoneItemId = gemstoneItem.getItem().getItemId();
		if (gemstoneItemId != 2130 && gemstoneItemId != 2131)
		{
			player.sendPacket(Static.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		// Check if the gemstoneCount is sufficant
		int itemGrade = targetItem.getItem().getItemGrade();
		switch (itemGrade)
		{
			case L2Item.CRYSTAL_C:
				if (_gemstoneCount != 20 || gemstoneItemId != 2130)
				{
					player.sendPacket(Static.GEMSTONE_QUANTITY_IS_INCORRECT);
					return;
				}
				break;
			case L2Item.CRYSTAL_B:
				if (_gemstoneCount != 30 || gemstoneItemId != 2130)
				{
					player.sendPacket(Static.GEMSTONE_QUANTITY_IS_INCORRECT);
					return;
				}
				break;
			case L2Item.CRYSTAL_A:
				if (_gemstoneCount != 20 || gemstoneItemId != 2131)
				{
					player.sendPacket(Static.GEMSTONE_QUANTITY_IS_INCORRECT);
					return;
				}
				break;
			case L2Item.CRYSTAL_S:
				if (_gemstoneCount != 25 || gemstoneItemId != 2131)
				{
					player.sendPacket(Static.GEMSTONE_QUANTITY_IS_INCORRECT);
					return;
				}
				break;
		}

		player.sendPacket(new ExConfirmVariationGemstone(_gemstoneItemObjId, _gemstoneCount));
		player.sendPacket(Static.PRESS_THE_AUGMENT_BUTTON_TO_BEGIN);
	}
}
