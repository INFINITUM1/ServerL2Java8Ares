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
package net.sf.l2j.loginserver.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.loginserver.L2LoginClient;

import org.mmocore.network.ReceivablePacket;

/**
 *
 * @author  KenM
 */
public abstract class L2LoginClientPacket extends ReceivablePacket<L2LoginClient>
{
	private static Logger _log = Logger.getLogger(L2LoginClientPacket.class.getName());
	
	/**
	 * @see org.mmocore.network.ReceivablePacket#read()
	 */
	@Override
	protected final boolean read()
	{
		try
		{
			getClient().can_runImpl = true;
			return readImpl();
		}
		catch (Exception e)
		{
			System.out.println(getClient().toString() + "ERROR READING: " + getClass().getSimpleName());
			e.printStackTrace();
			return false;
		}
	}

	protected abstract boolean readImpl();
}
