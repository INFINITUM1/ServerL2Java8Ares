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
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.ClanTable;
import ru.agecold.gameserver.model.L2Clan;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

public final class AllyDismiss extends L2GameClientPacket
{
    private String _clanName;

    @Override
	protected void readImpl()
    {
    	_clanName = readS();
    }

    @Override
	protected void runImpl()
    {
        if (_clanName == null)
        {
            return;
        }
        L2PcInstance player = getClient().getActiveChar();
        if (player == null)
        {
            return;
        }
		if (player.getClan() == null)
        {
			player.sendPacket(Static.YOU_ARE_NOT_A_CLAN_MEMBER);
            return;
        }
        L2Clan leaderClan = player.getClan();
		if (leaderClan.getAllyId() == 0)
		{
			player.sendPacket(Static.NO_CURRENT_ALLIANCES);
			return;
		}
		if (!player.isClanLeader() || leaderClan.getClanId() != leaderClan.getAllyId())
		{
			player.sendPacket(Static.FEATURE_ONLY_FOR_ALLIANCE_LEADER);
			return;
		}
		L2Clan clan = ClanTable.getInstance().getClanByName(_clanName);
        if (clan == null)
        {
			player.sendPacket(Static.CLAN_DOESNT_EXISTS);
			return;
        }
        if (clan.getClanId() == leaderClan.getClanId())
        {
			player.sendPacket(Static.ALLIANCE_LEADER_CANT_WITHDRAW);
			return;
        }
        if (clan.getAllyId() != leaderClan.getAllyId())
        {
			player.sendPacket(Static.DIFFERANT_ALLIANCE);
			return;
        }

		long currentTime = System.currentTimeMillis();
        leaderClan.setAllyPenaltyExpiryTime(
        		currentTime + Config.ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED * 86400000L,
        		L2Clan.PENALTY_TYPE_DISMISS_CLAN); //24*60*60*1000 = 86400000
		leaderClan.updateClanInDB();

        clan.setAllyId(0);
        clan.setAllyName(null);
        clan.setAllyPenaltyExpiryTime(
        		currentTime + Config.ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED * 86400000L,
        		L2Clan.PENALTY_TYPE_CLAN_DISMISSED); //24*60*60*1000 = 86400000
        clan.updateClanInDB();

        player.sendPacket(Static.YOU_HAVE_EXPELED_A_CLAN);
    }
}