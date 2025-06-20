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
package scripts.items.itemhandlers;

import scripts.items.IItemHandler;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PlayableInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:30:07 $
 */

public class CompSpiritShotPacks implements IItemHandler
{
	private static final int[] ITEM_IDS = { 5140, 5141, 5142, 5143, 5144, 5145, 5256, 5257, 5258, 5259, 5260, 5261 };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable.isPlayer()))
			return;
		L2PcInstance activeChar = (L2PcInstance)playable;

	    int itemId = item.getItemId();
	    int itemToCreateId;
	    int amount;

	    if (itemId < 5200){ // Normal Compressed Package of SpiritShots
    		itemToCreateId = itemId - 2631; // Gives id of matching item for this pack
    		amount = 300;
	    }else{  // Greater Compressed Package of Spirithots
     		itemToCreateId = itemId - 2747; // Gives id of matching item for this pack
	    	amount = 1000;
	    }

		activeChar.getInventory().destroyItem("Extract", item, activeChar, null);
	    activeChar.getInventory().addItem("Extract", itemToCreateId, amount, activeChar, item);

	    SystemMessage sm = SystemMessage.id(SystemMessageId.EARNED_S2_S1_S);
	    sm.addItemName(itemToCreateId);
        sm.addNumber(amount);
	    activeChar.sendPacket(sm);

		activeChar.sendItems(false);
	}
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
