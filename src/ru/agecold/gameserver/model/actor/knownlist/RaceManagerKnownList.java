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

import ru.agecold.gameserver.MonsterRace;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2RaceManagerInstance;
import ru.agecold.gameserver.network.serverpackets.DeleteObject;

public class RaceManagerKnownList extends NpcKnownList {
    // =========================================================
    // Data Field

    // =========================================================
    // Constructor
    public RaceManagerKnownList(L2RaceManagerInstance activeChar) {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    @Override
    public boolean addKnownObject(L2Object object) {
        return addKnownObject(object, null);
    }

    @Override
    public boolean addKnownObject(L2Object object, L2Character dropper) {
        if (!super.addKnownObject(object, dropper)) {
            return false;
        }

        /* DONT KNOW WHY WE NEED THIS WHEN RACE MANAGER HAS A METHOD THAT BROADCAST TO ITS KNOW PLAYERS
        if (object.isPlayer()) {
        if (packet != null)
        ((L2PcInstance) object).sendPacket(packet);
        }
         */

        return true;
    }

    @Override
    public boolean removeKnownObject(L2Object object) {
        if (!super.removeKnownObject(object)) {
            return false;
        }

        if (object.isPlayer()) {
            //System.out.println("Sending delete monsrac info.");
            DeleteObject obj = null;
            for (int i = 0; i < 8; i++) {
                obj = new DeleteObject(MonsterRace.getInstance().getMonsters()[i]);
                object.sendPacket(obj);
            }
        }

        return true;
    }

    // =========================================================
    // Method - Private
    // =========================================================
    // Property - Public
    @Override
    public L2RaceManagerInstance getActiveChar() {
        return (L2RaceManagerInstance) super.getActiveChar();
    }
}
