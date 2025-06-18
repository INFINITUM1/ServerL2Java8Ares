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


import ru.agecold.gameserver.datatables.HennaTable;
import ru.agecold.gameserver.model.L2HennaInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.HennaItemInfo;
import ru.agecold.gameserver.templates.L2Henna;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public final class RequestHennaItemInfo extends L2GameClientPacket
{
	private int _symbolId;
	// format  cd


	@Override
	protected void readImpl()
	{
		_symbolId  = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		    return;
			
		L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);
        if(template == null)
            return;
			
    	L2HennaInstance temp = new L2HennaInstance(template);

		player.sendPacket(new HennaItemInfo(temp,player));
	}
}
