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
package ru.agecold.gameserver.skills.l2skills;

import ru.agecold.gameserver.idfactory.IdFactory;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.StatsSet;
import ru.agecold.util.Rnd;
import javolution.util.FastList;

/**
 * @author Nemesiss
 *
 */
public class L2SkillCreateItem extends L2Skill
{
    private final int[] _createItemId;
    private final int _createItemCount;
    private final int _randomCount;

    public L2SkillCreateItem(StatsSet set)
    {
        super(set);
        _createItemId = set.getIntegerArray("create_item_id");
        _createItemCount = set.getInteger("create_item_count", 0);
        _randomCount = set.getInteger("random_count", 1);
    }

    /**
     * @see ru.agecold.gameserver.model.L2Skill#useSkill(ru.agecold.gameserver.model.L2Character, ru.agecold.gameserver.model.L2Object[])
     */
    @Override
	public void useSkill(L2Character activeChar, FastList<L2Object> targets)
    {
        if (activeChar.isAlikeDead()) return;
        if (_createItemId == null || _createItemCount == 0)
        {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.SKILL_NOT_AVAILABLE));
            return;
        }

        if (activeChar.isPlayer())
        {
            int rnd = Rnd.nextInt(_randomCount) + 1;
            int count = _createItemCount * rnd;
            int rndid = Rnd.nextInt(_createItemId.length);
            giveItems(activeChar.getPlayer(), _createItemId[rndid], count);
        }
    }

    /**
     * @param activeChar
     * @param itemId
     * @param count
     */
    public void giveItems(L2PcInstance activeChar, int itemId, int count)
    {
        L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
        if (item == null) return;
        item.setCount(count);
        activeChar.getInventory().addItem("Skill", item, activeChar, activeChar);

        if (count > 1)
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.EARNED_S2_S1_S).addItemName(item.getItemId()).addNumber(count));
        else
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(item.getItemId()));
        activeChar.sendItems(false);
    }
}
