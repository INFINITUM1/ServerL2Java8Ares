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
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.EtcStatusUpdate;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.skills.Env;


public class EffectCharge extends L2Effect
{
	public int numCharges;

	public EffectCharge(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.CHARGE;
	}

	@Override
	public boolean onActionTime()
    {
    	// ignore
    	return true;
    }

	@Override
	public int getLevel() { return numCharges; }

	public void addNumCharges(int i) { numCharges = numCharges + i; }
}
