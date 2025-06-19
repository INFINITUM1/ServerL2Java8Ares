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

import ru.agecold.gameserver.lib.Log;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.util.BypassStorage.ValidBypass;
import scripts.communitybbs.CommunityBoard;
/**
 * Format SSSSSS
 * @author -Wooden-
 *
 */
public class RequestBBSwrite extends L2GameClientPacket
{
	private String _url;
	private String _arg1;
	private String _arg2;
	private String _arg3;
	private String _arg4;
	private String _arg5;

	@Override
	protected void readImpl()
	{
		_url = readS();
		_arg1 = readS();
		_arg2 = readS();
		_arg3 = readS();
		_arg4 = readS();
		_arg5 = readS();
	}

	/* (non-Javadoc)
	 * @see ru.agecold.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected
	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

		ValidBypass bp = player.getBypassStorage().validate(_url);
		if(bp == null)
		{
			player.sendActionFailed();
			Log.add("BBSwrite direct access to bypass: " + _url + " / Player: " + player, "bypass");
			return;
		}
		CommunityBoard.getInstance().handleWriteCommands(getClient(),_url,_arg1,_arg2, _arg3, _arg4, _arg5);
	}
}