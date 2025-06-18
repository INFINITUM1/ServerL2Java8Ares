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
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PlayableInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.MagicSkillUser;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.L2Item;
import ru.agecold.gameserver.templates.L2Weapon;
import ru.agecold.gameserver.templates.L2WeaponType;
import ru.agecold.gameserver.util.Broadcast;

/**
 * @author -Nemesiss-
 *
 */
public class FishShots implements IItemHandler {
    // All the item IDs that this handler knows.

    private static final int[] ITEM_IDS = {6535, 6536, 6537, 6538, 6539, 6540};
    private static final int[] SKILL_IDS = {2181, 2182, 2183, 2184, 2185, 2186};

    /* (non-Javadoc)
     * @see ru.agecold.gameserver.handler.IItemHandler#useItem(ru.agecold.gameserver.model.L2PcInstance, ru.agecold.gameserver.model.L2ItemInstance)
     */
    public void useItem(L2PlayableInstance playable, L2ItemInstance item) {
        if (!(playable.isPlayer())) {
            return;
        }

        L2PcInstance activeChar = (L2PcInstance) playable;
        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
        L2Weapon weaponItem = activeChar.getActiveWeaponItem();

        if (weaponInst == null || weaponItem.getItemType() != L2WeaponType.ROD) {
            return;
        }

        if (weaponInst.getChargedFishshot()) {
            // spiritshot is already active
            return;
        }

        int FishshotId = item.getItemId();
        int grade = weaponItem.getCrystalType();
        int count = item.getCount();

        if ((grade == L2Item.CRYSTAL_NONE && FishshotId != 6535)
                || (grade == L2Item.CRYSTAL_D && FishshotId != 6536)
                || (grade == L2Item.CRYSTAL_C && FishshotId != 6537)
                || (grade == L2Item.CRYSTAL_B && FishshotId != 6538)
                || (grade == L2Item.CRYSTAL_A && FishshotId != 6539)
                || (grade == L2Item.CRYSTAL_S && FishshotId != 6540)) {
            //1479 - This fishing shot is not fit for the fishing pole crystal.
            activeChar.sendPacket(Static.WRONG_FISHINGSHOT_GRADE);
            return;
        }

        if (count < 1) {
            return;
        }

        weaponInst.setChargedFishshot(true);
        activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), 1, null, false);
        L2Object oldTarget = activeChar.getTarget();
        activeChar.setTarget(activeChar);

        //activeChar.sendPacket(SystemMessage.id(SystemMessage.ENABLED_SPIRITSHOT));
        activeChar.broadcastPacket(new MagicSkillUser(activeChar, SKILL_IDS[grade], 1, 0, 0));
        activeChar.setTarget(oldTarget);
    }

    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
