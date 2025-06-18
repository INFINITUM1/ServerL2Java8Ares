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

import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.model.L2Effect;
import ru.agecold.gameserver.network.serverpackets.MyTargetSelected;
import ru.agecold.gameserver.skills.Env;

/**
 * 
 * @author -Nemesiss-
 */
public class EffectTargetMe extends L2Effect
{
	public EffectTargetMe(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#getEffectType()
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TARGET_ME;
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onStart()
	 */
	@Override
	public void onStart()
	{
		getEffected().setTarget(getEffector());
		getEffected().sendPacket(new MyTargetSelected(getEffector().getObjectId(), 0));
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getEffector());
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onExit()
	 */
	@Override
	public void onExit()
	{
		// nothing
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onActionTime()
	 */
	@Override
	public boolean onActionTime()
	{
		// nothing
		return false;
	}
}
