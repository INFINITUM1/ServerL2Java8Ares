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
package ru.agecold.gameserver;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import ru.agecold.Config;
import ru.agecold.gameserver.instancemanager.ItemsOnGroundManager;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.templates.L2EtcItemType;
import ru.agecold.util.log.AbstractLogger;

public class ItemsAutoDestroy
{
	protected static final Logger _log = AbstractLogger.getLogger("ItemsAutoDestroy");
	private static ItemsAutoDestroy _instance;
	protected List<L2ItemInstance> _items = null;
	protected static long _sleep;

    private ItemsAutoDestroy()
    {
        _items = new FastList<L2ItemInstance>();
        _sleep	= Config.AUTODESTROY_ITEM_AFTER * 1000;
        if(_sleep == 0) // it should not happend as it is not called when AUTODESTROY_ITEM_AFTER = 0 but we never know..
        	_sleep = 3600000;
        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckItemsForDestroy(),5000,5000);
		_log.info("ItemsAutoDestroy: Loaded.");
    }

    public static ItemsAutoDestroy getInstance()
    {
        return _instance;
    }
	
    public static void init()
    {
        _instance = new ItemsAutoDestroy();
    }

    public synchronized void addItem (L2ItemInstance item)
    {
        item.setDropTime(System.currentTimeMillis());
        _items.add(item);
    }

    public synchronized void removeItems()
    {
    	//if (Config.DEBUG)
    		//_log.info("[ItemsAutoDestroy] : "+_items.size()+" items to check.");

    	if (_items.isEmpty()) return;

        long curtime = System.currentTimeMillis();
        for (L2ItemInstance item : _items)
        {
            if (item == null || item.getDropTime()==0 || item.getLocation() != L2ItemInstance.ItemLocation.VOID)
                _items.remove(item);
            else
            {
            	if(item.getItemType() == L2EtcItemType.HERB )
            	{
            		if((curtime - item.getDropTime()) > Config.HERB_AUTO_DESTROY_TIME)
            		{
            			L2World.getInstance().removeVisibleObject(item,item.getWorldRegion());
            			L2World.getInstance().removeObject(item);
            			_items.remove(item);
            			if (Config.SAVE_DROPPED_ITEM)
                			ItemsOnGroundManager.getInstance().removeObject(item);
            		}
            	}
            	else if ( (curtime - item.getDropTime()) > _sleep)
                {
                    L2World.getInstance().removeVisibleObject(item,item.getWorldRegion());
                    L2World.getInstance().removeObject(item);
                    _items.remove(item);
                    if (Config.SAVE_DROPPED_ITEM)
                    	ItemsOnGroundManager.getInstance().removeObject(item);
                }
            }
        }

    	//if (Config.DEBUG)
    		//_log.info("[ItemsAutoDestroy] : "+_items.size()+" items remaining.");
    }

    protected class CheckItemsForDestroy extends Thread
    {
        @Override
		public void run()
        {
        	removeItems();
        }
    }
}