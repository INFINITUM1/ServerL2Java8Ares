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
package ru.agecold.gameserver.skills.effects;

import ru.agecold.gameserver.model.L2Effect;
import ru.agecold.gameserver.skills.Env;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2Skill.SkillType;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.util.Rnd;
import javolution.util.FastTable;

public class EffectMalariaDebuff extends L2Effect
{
	public EffectMalariaDebuff(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return L2Effect.EffectType.MUTE;
	}

	@Override
	public void onStart() 
	{
		if (getEffected().isPlayer())
		{
			getEffected().startAbnormalEffect(0x02000);

			FastTable<L2Effect> effects = getEffected().getAllEffectsTable();
			for (int i = 0, n = effects.size(); i < n; i++) 
			{
				L2Effect e = effects.get(i);
				if (e == null)
					continue;
					
				if (e.getSkill().getId() == 4554 || e.getSkill().getId() == 4552)
					e.exit();
			}
		}	
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(0x02000);
	}
}
