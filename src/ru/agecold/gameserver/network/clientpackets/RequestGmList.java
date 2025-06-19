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

import ru.agecold.gameserver.GmListTable;
import ru.agecold.gameserver.network.smartguard.SmartGuard;
import ru.agecold.gameserver.network.smartguard.integration.SmartClient;
import smartguard.api.ISmartGuardService;
import smartguard.core.properties.GuardProperties;
import smartguard.spi.SmartGuardSPI;

/**
 * This class handles RequestGmLista packet triggered by /gmlist command
 *
 * @version $Revision: 1.1.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestGmList extends L2GameClientPacket
{
	private byte[] data = null;

	@Override
	protected void readImpl()
	{
		if(SmartGuard.isActive() && _buf.remaining() > 2)
		{
			final int dataLen = readH();
			if(_buf.remaining() >= dataLen)
			{
				readB(data = new byte[dataLen]);
			}
		}
	}

	@Override
	protected void runImpl()
	{
		if(SmartGuard.isActive() && data != null)
		{
			ISmartGuardService svc = SmartGuardSPI.getSmartGuardService();
			svc.getSmartGuardBus().onClientData(new SmartClient(getClient()), data);
			return;
		}
		if(getClient().getActiveChar() != null)
		{
			GmListTable.getInstance().sendListToPlayer(getClient().getActiveChar());
		}
	}
}
