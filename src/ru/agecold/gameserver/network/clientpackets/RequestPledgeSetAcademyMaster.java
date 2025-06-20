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
import ru.agecold.gameserver.model.L2Clan;
import ru.agecold.gameserver.model.L2ClanMember;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) dSS
 * @author  -Wooden-
 *
 */
public final class RequestPledgeSetAcademyMaster extends L2GameClientPacket
{
    private String _currPlayerName;
    private int _set; // 1 set, 0 delete
    private String _targetPlayerName;

    @Override
	protected void readImpl()
    {
        _set = readD();
        _currPlayerName = readS();
        _targetPlayerName = readS();
    }

    /**
     * @see ru.agecold.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
	protected void runImpl()
    {
    	L2PcInstance player = getClient().getActiveChar();
        L2Clan clan = player.getClan();
        if (clan == null) return;

        if((player.getClanPrivileges() & L2Clan.CP_CL_MASTER_RIGHTS) != L2Clan.CP_CL_MASTER_RIGHTS)
        {
        	player.sendPacket(Static.YOU_DO_NOT_HAVE_THE_RIGHT_TO_DISMISS_AN_APPRENTICE);
        	return;
        }

        L2ClanMember currentMember = clan.getClanMember(_currPlayerName);
        L2ClanMember targetMember = clan.getClanMember(_targetPlayerName);
        if (currentMember == null || targetMember == null) return;

        L2ClanMember apprenticeMember, sponsorMember;
        if (currentMember.getPledgeType() == L2Clan.SUBUNIT_ACADEMY)
        {
        	apprenticeMember = currentMember;
        	sponsorMember = targetMember;
        }
        else
        {
        	apprenticeMember = targetMember;
        	sponsorMember = currentMember;
        }

        L2PcInstance apprentice = apprenticeMember.getPlayerInstance();
        L2PcInstance sponsor = sponsorMember.getPlayerInstance();

        SystemMessage sm = null;
        if(_set == 0)
        {
        	// test: do we get the current sponsor & apprentice from this packet or no?
        	if (apprentice != null) apprentice.setSponsor(0);
        	else // offline
        		apprenticeMember.initApprenticeAndSponsor(0, 0);

        	if (sponsor != null) sponsor.setApprentice(0);
        	else // offline
        		sponsorMember.initApprenticeAndSponsor(0, 0);

        	apprenticeMember.saveApprenticeAndSponsor(0, 0);
        	sponsorMember.saveApprenticeAndSponsor(0, 0);

        	sm = SystemMessage.id(SystemMessageId.S2_CLAN_MEMBER_S1_S_APPRENTICE_HAS_BEEN_REMOVED);
        }
        else
        {
        	if (apprenticeMember.getSponsor() != 0 || sponsorMember.getApprentice() != 0
        			|| apprenticeMember.getApprentice() != 0 || sponsorMember.getSponsor() != 0)
        	{
        		player.sendMessage("Remove previous connections first.");
        		return;
        	}
        	if (apprentice != null)
        		apprentice.setSponsor(sponsorMember.getObjectId());
        	else // offline
        		apprenticeMember.initApprenticeAndSponsor(0, sponsorMember.getObjectId());

        	if (sponsor != null)
        		sponsor.setApprentice(apprenticeMember.getObjectId());
        	else // offline
        		sponsorMember.initApprenticeAndSponsor(apprenticeMember.getObjectId(), 0);

        	// saving to database even if online, since both must match
        	apprenticeMember.saveApprenticeAndSponsor(0, sponsorMember.getObjectId());
        	sponsorMember.saveApprenticeAndSponsor(apprenticeMember.getObjectId(), 0);

        	sm = SystemMessage.id(SystemMessageId.S2_HAS_BEEN_DESIGNATED_AS_APPRENTICE_OF_CLAN_MEMBER_S1);
        }
        sm.addString(sponsorMember.getName()).addString(apprenticeMember.getName());
    	if (sponsor != player && sponsor != apprentice) player.sendPacket(sm);
    	if (sponsor != null) sponsor.sendPacket(sm);
    	if (apprentice != null) apprentice.sendPacket(sm);
		sm = null;
    }

    /**
     * @see ru.agecold.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return "C.PledgeSetAcademyMaster";
    }


}