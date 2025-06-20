/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.agecold.gameserver.skills.effects;

import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.model.L2Effect;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.skills.Env;

final class EffectChanceSkill extends L2Effect
{
	public EffectChanceSkill(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.CHANCE_SKILL;
	}

	@Override
	public void onStart()
	{
		L2Skill skill = SkillTable.getInstance().getInfo(getSkill().getChanceTriggeredId(), getSkill().getChanceTriggeredLevel());
		if (skill !=null)
			getEffected().addChanceSkill(skill);
	}

	@Override
	public void onExit()
	{
		if (getEffected().getChanceSkills() != null)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(getSkill().getChanceTriggeredId(), getSkill().getChanceTriggeredLevel());
			getEffected().removeChanceSkill(skill);
		}
	}

    @Override
	public boolean onActionTime()
    {
    	return false;
    }
}
