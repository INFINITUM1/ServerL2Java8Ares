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
package ru.agecold.gameserver.skills.conditions;

import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.skills.Env;


/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConditionPlayerState extends Condition {

	public enum CheckPlayerState { RESTING, MOVING, RUNNING, FLYING, BEHIND, FRONT }

	private final CheckPlayerState _check;
	private final boolean _required;

	public ConditionPlayerState(CheckPlayerState check, boolean required)
	{
		_check = check;
		_required = required;
	}

	@Override
	public boolean testImpl(Env env) {
		switch (_check)
		{
		case RESTING:
			if (env.cha.isPlayer()) {
				return env.cha.getPlayer().isSitting() == _required;
			}
			return !_required;
		case MOVING:
			return env.cha.isMoving() == _required;
		case RUNNING:
			return env.cha.isMoving() == _required && env.cha.isRunning() == _required;
		case FLYING:
			return env.cha.isFlying() == _required;
        case BEHIND:
            return env.cha.isBehindTarget() == _required;
        case FRONT:
            return env.cha.isFrontTarget() == _required;
		}
		return !_required;
	}
}

