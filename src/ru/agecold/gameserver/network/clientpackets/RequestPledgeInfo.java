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

import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.datatables.ClanTable;
import ru.agecold.gameserver.model.L2Clan;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.PledgeInfo;

/**
 * This class ...
 *
 * @version $Revision: 1.5.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPledgeInfo extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPledgeInfo.class.getName());

	private int _clanId;

	@Override
	protected void readImpl()
	{
		_clanId = readD();
	}

	@Override
	protected void runImpl()
	{
		//if (Config.DEBUG)
		//	_log.fine("infos for clan " + _clanId + " requested");

		L2PcInstance player = getClient().getActiveChar();
		L2Clan clan = ClanTable.getInstance().getClan(_clanId);
		if (clan == null)
		{
			//_log.warning("Clan data for clanId " + _clanId + " is missing");
			return; // we have no clan data ?!? should not happen
		}

		if (player != null)
			player.sendPacket(new PledgeInfo(clan));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ru.agecold.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[C] PledgeInfo";
	}
}
