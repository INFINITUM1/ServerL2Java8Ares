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

import net.sf.l2j.loginserver.serverpackets.ServerBasePacket;

/**
 * @author -Wooden-
 *
 */
public class PlayerAuthResponse extends ServerBasePacket
{
	public PlayerAuthResponse(String account, boolean response, String hwid, boolean email, int serverId, String phone)
	{
		writeC(0x03);
		writeS(account);
		writeC(response ? 1 : 0);
		writeS(hwid);
		writeC(email ? 1 : 0);
		writeC(serverId);
		writeS(phone);
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