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
package ru.agecold.gameserver.network.serverpackets;

public class TutorialShowQuestionMark extends L2GameServerPacket
{
	private int _blink;

	public TutorialShowQuestionMark(int blink)
	{
		_blink = blink; //this influences the blinking frequancy :S
	}

	/* (non-Javadoc)
	 * @see ru.agecold.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xa1);
		writeD(_blink);

	}

	/* (non-Javadoc)
	 * @see ru.agecold.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "S.TutorialShowQuestionMark";
	}

}