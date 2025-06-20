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

import ru.agecold.gameserver.instancemanager.CastleManager;
import ru.agecold.gameserver.model.L2Attackable;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2SiegeClan;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.Castle;

/**
 * sample
 * 0b
 * 952a1048     objectId
 * 00000000 00000000 00000000 00000000 00000000 00000000

 * format  dddddd   rev 377
 * format  ddddddd   rev 417
 *
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/27 18:46:18 $
 */
public class Die extends L2GameServerPacket
{
    private int _charObjId;
    private boolean _fake;
    private boolean _sweepable;
    private int _access;
    private ru.agecold.gameserver.model.L2Clan _clan;
    private static final int REQUIRED_LEVEL = ru.agecold.Config.GM_FIXED;
    L2Character _activeChar;

    /**
     * @param _characters
     */
    public Die(L2Character cha)
    {
    	_activeChar = cha;
        if (cha.isPlayer()) {
            _access = cha.getPlayer().getAccessLevel() >= REQUIRED_LEVEL ? 1 : 0;
            _clan= cha.getClan();

        }
        _charObjId = cha.getObjectId();
        _fake = !cha.isDead();
        if (cha.isL2Attackable())
            _sweepable = ((L2Attackable)cha).isSweepActive();

        if (cha.isInFixedZone()) {
            _access = 1;
        }
    }

    @Override
	protected final void writeImpl()
    {
        if (_fake)
            return;

        writeC(0x06);

        writeD(_charObjId);
        // NOTE:
        // 6d 00 00 00 00 - to nearest village
        // 6d 01 00 00 00 - to hide away
        // 6d 02 00 00 00 - to castle
        // 6d 03 00 00 00 - to siege HQ
        // sweepable
        // 6d 04 00 00 00 - FIXED

        writeD(0x01);                                                   // 6d 00 00 00 00 - to nearest village
        if (_clan != null)
        {
            L2SiegeClan siegeClan = null;
            Boolean isInDefense = false;
            Castle castle = CastleManager.getInstance().getCastle(_activeChar);
            if (castle != null && castle.getSiege().getIsInProgress())
            {
            	//siege in progress
                siegeClan = castle.getSiege().getAttackerClan(_clan);
                if (siegeClan == null && castle.getSiege().checkIsDefender(_clan)){
                	isInDefense = true;
                }
            }

            writeD(_clan.getHasHideout() > 0 ? 0x01 : 0x00);            // 6d 01 00 00 00 - to hide away
            writeD(_clan.getHasCastle() > 0 ||
            	   isInDefense? 0x01 : 0x00);             				// 6d 02 00 00 00 - to castle
            writeD(siegeClan != null &&
            	   !isInDefense &&
                   siegeClan.getNumFlags() > 0 ? 0x01 : 0x00);       // 6d 03 00 00 00 - to siege HQ
        }
        else
        {
            writeD(0x00);                                               // 6d 01 00 00 00 - to hide away
            writeD(0x00);                                               // 6d 02 00 00 00 - to castle
            writeD(0x00);                                               // 6d 03 00 00 00 - to siege HQ
        }

        writeD(_sweepable ? 0x01 : 0x00);                               // sweepable  (blue glow)
        writeD(_access >= REQUIRED_LEVEL? 0x01: 0x00);                  // 6d 04 00 00 00 - to FIXED
    }
}
