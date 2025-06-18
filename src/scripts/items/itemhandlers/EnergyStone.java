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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package scripts.items.itemhandlers;


import ru.agecold.gameserver.datatables.SkillTable;
import scripts.items.IItemHandler;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PetInstance;
import ru.agecold.gameserver.model.actor.instance.L2PlayableInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.EtcStatusUpdate;
import ru.agecold.gameserver.network.serverpackets.MagicSkillUser;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.skills.effects.EffectCharge;
import ru.agecold.gameserver.skills.l2skills.L2SkillCharge;

public class EnergyStone implements IItemHandler
{
    private static final int[] ITEM_IDS = { 5589 };
    private EffectCharge _effect;
    private L2SkillCharge _skill;

    public void useItem(L2PlayableInstance playable, L2ItemInstance item)
    {
    	L2PcInstance activeChar = null;
        if (playable.isPlayer())
        	activeChar = (L2PcInstance)playable;
        else if (playable.isPet())
        	activeChar = ((L2PetInstance)playable).getOwner();

		if (activeChar == null)
			return;

        if (item.getItemId() != 5589) 
			return;
		
		switch(activeChar.getClassId().getId())
		{
			case 2:
			case 48:
			case 88:
			case 114:
				if (activeChar.isAllSkillsDisabled())
				{
					activeChar.sendActionFailed();
					return;
				}

				if (activeChar.isSitting())
				{
					activeChar.sendPacket(Static.CANT_MOVE_SITTING);
					return;
				}

				_skill = getChargeSkill(activeChar);
				if (_skill == null)
				{
					activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_CANNOT_BE_USED).addItemName(5589));
					return;
				}

				if (activeChar.getCharges() < 2)
				{
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.broadcastPacket(new MagicSkillUser(playable, activeChar, _skill.getId(), 1, 1, 0));
					activeChar.increaseCharges();
				}
				else
					activeChar.sendPacket(Static.FORCE_MAXLEVEL_REACHED);
				break;
			default:
				activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_CANNOT_BE_USED).addItemName(5589));
				break;
		}
    }

    private L2SkillCharge getChargeSkill(L2PcInstance activeChar)
    {
		L2Skill[] skills = activeChar.getAllSkills();
		for (L2Skill s : skills) 
		{
			if (s == null)
				continue;

			if (s.getId() == 50 || s.getId() == 8) 
				return (L2SkillCharge)s;
		}
		return null;
    }

    public int[] getItemIds()
    {
        return ITEM_IDS;
    }
}