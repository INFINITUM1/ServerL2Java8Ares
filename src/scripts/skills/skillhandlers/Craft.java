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
package scripts.skills.skillhandlers;

import ru.agecold.gameserver.RecipeController;
import scripts.skills.ISkillHandler;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2Skill.SkillType;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import javolution.util.FastList;
import javolution.util.FastList.Node;
/**
 * This class ...
 *
 * @version $Revision: 1.1.2.2.2.4 $ $Date: 2005/04/06 16:13:48 $
 */

public class Craft implements ISkillHandler
{
	/* (non-Javadoc)
	 * @see ru.agecold.gameserver.handler.IItemHandler#useItem(ru.agecold.gameserver.model.L2PcInstance, ru.agecold.gameserver.model.L2ItemInstance)
	 */
	private static final SkillType[] SKILL_IDS = {SkillType.COMMON_CRAFT, SkillType.DWARVEN_CRAFT};

	/* (non-Javadoc)
	 * @see ru.agecold.gameserver.handler.IItemHandler#useItem(ru.agecold.gameserver.model.L2PcInstance, ru.agecold.gameserver.model.L2ItemInstance)
	 */
	public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
	{
		if (activeChar == null || !(activeChar.isPlayer())) return;

		L2PcInstance player = (L2PcInstance)activeChar;

		if (player.getPrivateStoreType() != 0)
		{
			player.sendPacket(Static.CANNOT_CREATED_WHILE_ENGAGED_IN_TRADING);
			return;
		}
		RecipeController.getInstance().requestBookOpen(player,(skill.getSkillType() == SkillType.DWARVEN_CRAFT) ? true : false);
		//targets.clear();
	}


	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
