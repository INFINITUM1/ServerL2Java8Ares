/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.agecold.gameserver.network.serverpackets;

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;

/**
 *
 * @author  devScarlet & mrTJO
 */
public final class ServerObjectInfo extends L2GameServerPacket
{
	private L2NpcInstance _activeChar;
	private int _x, _y, _z, _heading;
	private int _idTemplate;
	private boolean _isAttackable;
	private int _collisionHeight, _collisionRadius;

	public ServerObjectInfo(L2NpcInstance activeChar, L2Character actor)
	{
		_activeChar = activeChar;
		_idTemplate = _activeChar.getTemplate().idTemplate;
		_isAttackable = _activeChar.isAutoAttackable(actor);
		_collisionHeight = _activeChar.getCollisionHeight();
        _collisionRadius = _activeChar.getCollisionRadius();
        _x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_heading = _activeChar.getHeading();
	}
	
	/**
	 * @see ru.agecold.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected final void writeImpl()
	{
		writeC(0x8c);
		writeD(_activeChar.getObjectId());
		writeD(_idTemplate+1000000);
		writeS(""); // name
		writeD(_isAttackable ? 1 : 0);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
		writeF(1.0); // movement multiplier
		writeF(1.0); // attack speed multiplier
		writeF(_collisionRadius);
		writeF(_collisionHeight);
		writeD((int) (_isAttackable ? _activeChar.getCurrentHp() : 0));
		writeD(_isAttackable ? _activeChar.getMaxHp() : 0);
		writeD(0x01); // object type
		writeD(0x00); // special effects
	}

	/**
	 * @see ru.agecold.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return "S.ServerObjectInfo";
	}
}
