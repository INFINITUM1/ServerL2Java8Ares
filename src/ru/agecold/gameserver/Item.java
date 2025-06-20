/*
 * $Header: Item.java, 2/08/2005 00:49:12 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 2/08/2005 00:49:12 $
 * $Revision: 1 $
 * $Log: Item.java,v $
 * Revision 1  2/08/2005 00:49:12  luisantonioa
 * Added copyright notice
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
package ru.agecold.gameserver;

import ru.agecold.gameserver.templates.L2Item;
import ru.agecold.gameserver.templates.StatsSet;

/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public class Item
{
    public int      id;
    @SuppressWarnings("unchecked")
	public Enum     type;
    public String   name;
    public StatsSet set;
    public int      currentLevel;
    public L2Item   item;
}
