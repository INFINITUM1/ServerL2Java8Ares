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
package ru.agecold.gameserver.network.clientpackets;

import ru.agecold.Config;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestOustPartyMember extends L2GameClientPacket {

    private String _name;

    @Override
    protected void readImpl() {
        _name = readS();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.getCPD() < 400) {
            return;
        }

        if (player.isOutOfControl()) {
            return;
        }

        player.setCPD();

        if (player.isInParty() && player.getParty().isLeader(player)) {
            if (player.getParty().isInDimensionalRift() && !player.getParty().getDimensionalRift().getRevivedAtWaitingRoom().contains(player)) {
                player.sendMessage("You can't dismiss party member when you are in Dimensional Rift.");
            } else {
                L2PcInstance target = L2World.getInstance().getPlayer(_name);
                if (target == null || target.hasPartyExitPenalty()) {
                    return;
                }

                player.getParty().oustPartyMember(_name);
            }
        }
    }
}
