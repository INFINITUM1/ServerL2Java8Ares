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
package ru.agecold.gameserver.model.actor.instance;

import ru.agecold.Config;
import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.ai.L2CharacterAI;
import ru.agecold.gameserver.ai.L2ControllableMobAI;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.templates.L2NpcTemplate;

/**
 * @author littlecrow
 *
 */
public class L2ControllableMobInstance extends L2MonsterInstance
{
	private boolean _isInvul;
	private L2ControllableMobAI _aiBackup;	// to save ai, avoiding beeing detached

	protected class ControllableAIAcessor extends AIAccessor
    {
		@Override
		public void detachAI()
        {
			// do nothing, AI of controllable mobs can't be detached automatically
		}
	}


	@Override
	public boolean isAggressive()
    {
		return true;
	}

	@Override
	public int getAggroRange()
    {
		// force mobs to be aggro
		return 500;
	}

	public L2ControllableMobInstance(int objectId, L2NpcTemplate template)
    {
		super(objectId, template);
	}

	@Override
	public L2CharacterAI getAI()
    {
		//synchronized(this)
		//{
			if (_ai == null && _aiBackup == null)
            {
				_ai = new L2ControllableMobAI(new ControllableAIAcessor());
				_aiBackup = (L2ControllableMobAI)_ai;
			}
			else
			{
				_ai = _aiBackup;
            }
		//}
		return _ai;
	}

	@Override
	public boolean isInvul()
    {
		return _isInvul;
	}

	public void setInvul(boolean isInvul)
    {
		_isInvul = isInvul;
	}

	@Override
	public void reduceCurrentHp(double i, L2Character attacker, boolean awake)
    {
		if (isInvul() || isDead())
			return;

		if (awake)
			stopSleeping(null);

		i = getCurrentHp() - i;

		if (i < 0)
			i = 0;

		setCurrentHp(i);

		if (isDead())
		{
			// first die (and calculate rewards), if currentHp < 0,
			// then overhit may be calculated
			//if (Config.DEBUG) _log.fine("char is dead.");

			stopMove(null);

			// Start the doDie process
			doDie(attacker);

			// now reset currentHp to zero
			setCurrentHp(0);
		}
	}

	@Override
	public boolean doDie(L2Character killer)
    {
		if (!super.doDie(killer))
			return false;

		removeAI();
		return true;
	}

	@Override
	public void deleteMe()
    {
		removeAI();
		super.deleteMe();
	}

	/**
	 * Definitively remove AI
	 */
	protected void removeAI()
    {
		//synchronized (this)
        //{
			if (_aiBackup != null)
            {
				_aiBackup.setIntention(CtrlIntention.AI_INTENTION_IDLE);
				_aiBackup = null;
				_ai = null;
			}
		//}
	}
}