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
package ru.agecold.gameserver.model.actor.knownlist;

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.actor.instance.L2CabaleBufferInstance;
import ru.agecold.gameserver.model.actor.instance.L2FestivalGuideInstance;
import ru.agecold.gameserver.model.actor.instance.L2FolkInstance;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PlayableInstance;

public class NpcKnownList extends CharKnownList
{
    // =========================================================
    // Data Field

    // =========================================================
    // Constructor
    public NpcKnownList(L2NpcInstance activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    @Override
	public L2NpcInstance getActiveChar() { return (L2NpcInstance)super.getActiveChar(); }

    @Override
	public int getDistanceToForgetObject(L2Object object) { return 2 * getDistanceToWatchObject(object); }

    @Override
	public int getDistanceToWatchObject(L2Object object)
    {
        if (object instanceof L2FestivalGuideInstance)
            return 10000;

        if (object.isL2Folk() || !(object.isL2Character()))
            return 0;

        if (object instanceof L2CabaleBufferInstance)
            return 900;

        if (object.isL2Playable())
            return 1500;

        return 500;
    }
}
