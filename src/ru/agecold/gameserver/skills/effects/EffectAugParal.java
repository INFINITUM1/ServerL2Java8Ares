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

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2SiegeFlagInstance;
import ru.agecold.gameserver.model.actor.instance.L2BossInstance;
import ru.agecold.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.agecold.gameserver.model.actor.instance.L2MinionInstance;
import ru.agecold.gameserver.model.L2Effect;
import ru.agecold.gameserver.skills.Env;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.network.serverpackets.MagicSkillUser;
import ru.agecold.util.Rnd;

final class EffectAugParal extends L2Effect {

	public EffectAugParal(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.PARALYZE;
	}

	@Override
	public void onStart()
	{
		getEffector().broadcastPacket(new MagicSkillUser(getEffector(), getEffected(), 5144, 1, 0, 0));
		getEffected().startAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_1);
		getEffected().setIsParalyzed(true);
	}

	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_1);
		getEffected().setIsParalyzed(false);
	}

    @Override
	public boolean onActionTime()
    {
		// Simply stop the effect
		getEffected().setIsInvul(false);
		return false;
    }
}
