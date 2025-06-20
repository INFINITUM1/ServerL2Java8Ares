/*
 * $HeadURL: $
 *
 * $Author: $
 * $Date: $
 * $Revision: $
 *
 *
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
package ru.agecold.gameserver.taskmanager;

import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.network.serverpackets.AutoAttackStop;
import ru.agecold.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: $ $Date: $
 * @author  Luca Baldi
 */
public class AttackStanceTaskManager
{
    protected static final Logger _log = AbstractLogger.getLogger(AttackStanceTaskManager.class.getName());

    protected Map<L2Character,Long> _attackStanceTasks = new FastMap<L2Character,Long>().shared("AttackStanceTaskManager._attackStanceTasks");

    private static AttackStanceTaskManager _instance;

    public AttackStanceTaskManager()
    {
    }

    public static AttackStanceTaskManager getInstance()
    {
        return _instance;
    }
	
	public static void init()
	{
		_instance = new AttackStanceTaskManager();
		_instance.load();
	}
	
	private void load()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FightModeScheduler(), 0, 1000);
	}

    public void addAttackStanceTask(L2Character actor)
    {
        _attackStanceTasks.put(actor, System.currentTimeMillis());
    }

    public void removeAttackStanceTask(L2Character actor)
    {
        _attackStanceTasks.remove(actor);
    }

    public boolean getAttackStanceTask(L2Character actor)
    {
        return _attackStanceTasks.containsKey(actor);
    }

    private class FightModeScheduler implements Runnable
    {
    	protected FightModeScheduler()
    	{
    		// Do nothing
    	}

        public void run()
        {
            Long current = System.currentTimeMillis();
            try
            {
            	if (_attackStanceTasks != null)
            		synchronized (this) {
            			for(L2Character actor : _attackStanceTasks.keySet())
            			{
            				if((current - _attackStanceTasks.get(actor)) > 15000)
            				{
            					actor.broadcastPacket(new AutoAttackStop(actor.getObjectId()));
            					actor.getAI().setAutoAttacking(false);
            					_attackStanceTasks.remove(actor);
            				}
            			}
            		}
            } catch (Throwable e) {
            	// TODO: Find out the reason for exception. Unless caught here, players remain in attack positions.
            	_log.warning(e.toString());
            }
        }
    }
}
