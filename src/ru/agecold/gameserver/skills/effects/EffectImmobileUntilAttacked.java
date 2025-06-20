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

import java.util.logging.Logger;

import ru.agecold.gameserver.model.L2Effect;
import ru.agecold.gameserver.skills.Env;
import ru.agecold.util.log.AbstractLogger;


/**
 * @author Ahmed
 * 
 */
public class EffectImmobileUntilAttacked extends L2Effect
{
	static final Logger _log = AbstractLogger.getLogger(EffectImmobileUntilAttacked.class.getName());
	
	public EffectImmobileUntilAttacked(Env env, EffectTemplate template)
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
		return EffectType.IMMOBILEUNTILATTACKED;
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onStart()
	 */
	@Override
	public void onStart()
	{
		getEffector().startImmobileUntilAttacked();
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onExit()
	 */
	@Override
	public void onExit()
	{
		getEffected().stopImmobileUntilAttacked(this);
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onActionTime()
	 */
	@Override
	public boolean onActionTime()
	{
		// just stop this effect
		return false;
	}
}