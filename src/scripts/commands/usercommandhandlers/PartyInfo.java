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
package scripts.commands.usercommandhandlers;

import scripts.commands.IUserCommandHandler;
import ru.agecold.gameserver.model.L2Party;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

/**
 * Support for /partyinfo command
 * Added by Tempy - 28 Jul 05
 */
public class PartyInfo implements IUserCommandHandler
{
    private static final int[] COMMAND_IDS = { 81 };

    /* (non-Javadoc)
     * @see ru.agecold.gameserver.handler.IUserCommandHandler#useUserCommand(int, ru.agecold.gameserver.model.L2PcInstance)
     */
    public boolean useUserCommand(int id, L2PcInstance activeChar)
    {
        if (id != COMMAND_IDS[0]) return false;

    	if (!activeChar.isInParty())
    	{
    		activeChar.sendMessage("You are not in a party.");
    		return false;
    	}

        L2Party playerParty = activeChar.getParty();
    	int memberCount = playerParty.getMemberCount();
    	int lootDistribution = playerParty.getLootDistribution();
    	String partyLeader = playerParty.getPartyMembers().get(0).getName();

        activeChar.sendPacket(SystemMessage.id(SystemMessageId.PARTY_INFORMATION));

    		switch (lootDistribution) {
    		case L2Party.ITEM_LOOTER:
    			activeChar.sendPacket(SystemMessage.id(SystemMessageId.LOOTING_FINDERS_KEEPERS));
    			break;
    		case L2Party.ITEM_ORDER:
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.LOOTING_BY_TURN));
    			break;
    		case L2Party.ITEM_ORDER_SPOIL:
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.LOOTING_BY_TURN_INCLUDE_SPOIL));
    			break;
    		case L2Party.ITEM_RANDOM:
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.LOOTING_RANDOM));
    			break;
    		case L2Party.ITEM_RANDOM_SPOIL:
                activeChar.sendPacket(SystemMessage.id(SystemMessageId.LOOTING_RANDOM_INCLUDE_SPOIL));
    			break;
    		}

    	activeChar.sendPacket(SystemMessage.id(SystemMessageId.PARTY_LEADER_S1).addString(partyLeader));

    	/*sm = SystemMessage.id(SystemMessageId.S1_S2);
    	sm.addString("Members: " + memberCount + "/9");*/
/*
    	activeChar.sendPacket(SystemMessage.id(SystemMessageId.WAR_LIST));*/
    	return true;
    }

    /* (non-Javadoc)
     * @see ru.agecold.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    public int[] getUserCommandList()
    {
        return COMMAND_IDS;
    }
}
