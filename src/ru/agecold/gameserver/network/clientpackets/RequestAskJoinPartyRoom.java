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

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.instancemanager.PartyWaitingRoomManager;
import ru.agecold.gameserver.instancemanager.PartyWaitingRoomManager.WaitingRoom;
import ru.agecold.gameserver.model.L2Party;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance.TransactionType;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.ExAskJoinPartyRoom;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

public class RequestAskJoinPartyRoom extends L2GameClientPacket
{
	private String _name;

    @Override
	protected void readImpl()
    {
        _name = readS();
    }

    @Override
	protected void runImpl()
    {
        L2PcInstance requestor = getClient().getActiveChar();
        L2PcInstance target = L2World.getInstance().getPlayer(_name);

		if (requestor == null)
		    return;
			
		if(System.currentTimeMillis() - requestor.gCPR() < 500)
			return;

		requestor.sCPR();

        if (target == null || target == requestor)
        {
			requestor.sendPacket(Static.TARGET_IS_INCORRECT);
			return;
		}
		
		WaitingRoom rRoom = requestor.getPartyRoom();
		if (rRoom == null)
			return;
		
		if (!rRoom.owner.equals(requestor))
			return;
		
		/*WaitingRoom tRoom = requestor.getPartyRoom();
		if (tRoom != null && tRoom.id != rRoom.id)
			return;*/
		
		if (target.isAlone() || target.isInEncounterEvent())
		{
			requestor.sendPacket(Static.LEAVE_ALONR);
			requestor.sendActionFailed();
			return;
		}

		if (target.isInParty())
        {
			requestor.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_ALREADY_IN_PARTY).addString(target.getName()));
			return;
		}

		if (target.equals(requestor))
        {
			requestor.sendPacket(Static.INCORRECT_TARGET);
			return;
		}

		if (target.isCursedWeaponEquiped() || requestor.isCursedWeaponEquiped())
        {
			requestor.sendPacket(Static.INCORRECT_TARGET);
			return;
		}

		if (target.isGM() && target.getMessageRefusal())
        {
			requestor.sendPacket(SystemMessage.id(SystemMessageId.S1_IS_ALREADY_IN_PARTY).addString(target.getName()));
			return;
		}
		
		if (target.isInJail() || requestor.isInJail())
        {
			requestor.sendPacket(Static.INCORRECT_TARGET);
			return;
		}

        if (target.isInOlympiadMode() || requestor.isInOlympiadMode())
            return;

        if (target.isInDuel() || requestor.isInDuel())
            return;
			
		if(requestor.isTransactionInProgress())
		{
			requestor.sendPacket(Static.WAITING_FOR_ANOTHER_REPLY);
			return;
		}

		target.setTransactionRequester(requestor, System.currentTimeMillis() + 10000);
		target.setTransactionType(TransactionType.ROOM);
		requestor.setTransactionRequester(target, System.currentTimeMillis() + 10000);
		requestor.setTransactionType(TransactionType.ROOM);

		target.sendPacket(new ExAskJoinPartyRoom(requestor.getName()));
    }
}