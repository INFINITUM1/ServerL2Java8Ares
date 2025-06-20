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
package net.sf.l2j.loginserver.loginserverpackets;

import java.io.IOException;

import net.sf.l2j.loginserver.GameServerTable;
import net.sf.l2j.loginserver.serverpackets.ServerBasePacket;

/**
 * @author -Wooden-
 *
 */
public class AuthResponse extends ServerBasePacket
{

	/**
	 * @param serverId
	 */
	public AuthResponse(int serverId)
	{
		writeC(0x06);
		writeC(0x7B);
		writeC(0xD5);
		writeC(serverId);
		writeS(GameServerTable.getInstance().getServerNameById(serverId));
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.loginserver.serverpackets.ServerBasePacket#getContent()
	 */
	@Override
	public byte[] getContent() throws IOException
	{
		return getBytes();
	}

}
