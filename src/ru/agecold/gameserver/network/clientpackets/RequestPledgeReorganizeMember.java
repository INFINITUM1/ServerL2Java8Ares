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

import ru.agecold.gameserver.model.L2Clan;
import ru.agecold.gameserver.model.L2ClanMember;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.PledgeShowMemberListUpdate;


/**
 * Format: (ch) dSdS
 * @author  -Wooden-
 */
public final class RequestPledgeReorganizeMember extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _unk1;
	private String _memberName;
	private int _newPledgeType;
	@SuppressWarnings("unused")
	private String _unk2;

	@Override
	protected void readImpl()
	{
		_unk1 = readD();
		_memberName = readS();
		_newPledgeType = readD();
		_unk2 = readS();
	}

	/**
	 * @see ru.agecold.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
	      if(player == null)
		       	return;
		     //do we need powers to do that??
		  L2Clan clan = player.getClan();
		      if(clan == null)
		       	return;
		  L2ClanMember member = clan.getClanMember(_memberName);
		      if(member == null)
		       	return;
		  member.setPledgeType(_newPledgeType);
		  clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(member));
	}

	/**
	 * @see ru.agecold.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "C.PledgeReorganizeMember";
	}

}
