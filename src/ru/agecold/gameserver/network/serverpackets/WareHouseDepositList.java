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

import java.util.logging.Logger;

import javolution.util.FastTable;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

/**
 * 0x53 WareHouseDepositList  dh (h dddhh dhhh d)
 *
 * @version $Revision: 1.4.2.1.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class WareHouseDepositList extends L2GameServerPacket {

    public static final int PRIVATE = 1;
    public static final int CLAN = 2;
    public static final int CASTLE = 3; //not sure
    public static final int FREIGHT = 4; //not sure
    private static Logger _log = Logger.getLogger(WareHouseDepositList.class.getName());
    private L2PcInstance _activeChar;
    private int _playerAdena;
    private FastTable<L2ItemInstance> _items;
    private int _whType;

    public WareHouseDepositList(L2PcInstance player, int type) {
        _activeChar = player;
        _whType = type;
        _playerAdena = _activeChar.getAdena();
        _items = new FastTable<L2ItemInstance>();

        for (L2ItemInstance item : player.getInventory().getAllItems()) {
            if (item != null && item.canBeStored(player, _whType == 1)) {
                _items.add(item);
            }
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0x41);
        /* 0x01-Private Warehouse
         * 0x02-Clan Warehouse
         * 0x03-Castle Warehouse
         * 0x04-Warehouse */
        writeH(_whType);
        writeD(_playerAdena);
        //if (Config.DEBUG) _log.fine("count:"+count);
        writeH(_items.size());

        for (int i = (_items.size() - 1); i > -1; i--) {
            L2ItemInstance item = _items.get(i);
            if (item == null || item.getItem() == null) {
                continue;
            }

            writeH(item.getItem().getType1()); // item type1 //unconfirmed, works
            writeD(item.getObjectId()); //unconfirmed, works
            writeD(item.getItemId()); //unconfirmed, works
            writeD(item.getCount()); //unconfirmed, works
            writeH(item.getItem().getType2());	// item type2 //unconfirmed, works
            writeH(0x00);	// ? 100
            writeD(item.getItem().getBodyPart());	// ?
            writeH(item.getEnchantLevel());	// enchant level -confirmed
            writeH(0x00);	// ? 300
            writeH(0x00);	// ? 200
            writeD(item.getObjectId()); // item id - confimed
            if (item.isAugmented()) {
                writeD(0x0000FFFF & item.getAugmentation().getAugmentationId());
                writeD(item.getAugmentation().getAugmentationId() >> 16);
            } else {
                writeQ(0x00);
            }
        }
    }

    @Override
    public void gc() {
        _items.clear();
        _items = null;
    }

    /* (non-Javadoc)
     * @see ru.agecold.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
    public String getType() {
        return "S.WareHouseDepositList";
    }
}
