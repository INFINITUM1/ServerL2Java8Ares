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

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.StatsSet;
import javolution.util.FastList;

public class L2SkillDefault extends L2Skill 
{

	public L2SkillDefault(StatsSet set) {
		super(set);
	}

	@Override
	public void useSkill(L2Character caster, FastList<L2Object> targets) 
	{
		caster.sendActionFailed();
		caster.sendPacket(SystemMessage.id(SystemMessageId.S1_S2).addString("Skill not implemented.  Skill ID: " + getId() + " " + getSkillType()));
	}

}
