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
package ru.agecold.gameserver.network.clientpackets;

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.Duel;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

/**
 * Format:(ch) ddd
 * @author  -Wooden-
 */
public final class RequestDuelAnswerStart extends L2GameClientPacket
{
	private int _partyDuel;
	@SuppressWarnings("unused")
	private int _unk1;
	private int _response;

	@Override
	protected void readImpl()
	{
		_partyDuel = readD();
		_unk1 = readD();
		_response = readD();
	}

	/**
	 * @see ru.agecold.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null) 
			return;
			
		L2PcInstance requestor = player.getTransactionRequester();
		
		if (requestor == null) 
			return;

		if (_response == 1)
		{
			SystemMessage msg1 = null, msg2 = null;
			if(!Duel.checkIfCanDuel(player, player, true) || !Duel.checkIfCanDuel(player, requestor, true))
				return;

			if (_partyDuel == 1)
			{
				msg1 = SystemMessage.id(SystemMessageId.YOU_HAVE_ACCEPTED_S1S_CHALLENGE_TO_A_PARTY_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addString(requestor.getName());
				msg2 = SystemMessage.id(SystemMessageId.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_DUEL_AGAINST_THEIR_PARTY_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addString(player.getName());
			}
			else
			{
				msg1 = SystemMessage.id(SystemMessageId.YOU_HAVE_ACCEPTED_S1S_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addString(requestor.getName());
				msg2 = SystemMessage.id(SystemMessageId.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addString(player.getName());
			}

			player.sendPacket(msg1);
			requestor.sendPacket(msg2);
			Duel.createDuel(requestor, player, _partyDuel);
			msg1 = null;
			msg2 = null;
		}
		else
		{
			SystemMessage msg = null;
			if (_partyDuel == 1) 
				msg = Static.THE_OPPOSING_PARTY_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL;
			else
				msg = SystemMessage.id(SystemMessageId.S1_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL).addString(player.getName());
    		requestor.sendPacket(msg);
			msg = null;
		}
		requestor.setTransactionRequester(null);
		player.setTransactionRequester(null);
	}
}
