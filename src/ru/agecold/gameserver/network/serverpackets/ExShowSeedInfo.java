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
package ru.agecold.gameserver.network.serverpackets;

import javolution.util.FastList;
import ru.agecold.gameserver.instancemanager.CastleManorManager.SeedProduction;
import ru.agecold.gameserver.model.L2Manor;

/**
 * format(packet 0xFE)
 * ch ddd [dddddcdcd]
 * c  - id
 * h  - sub id
 *
 * d  - manor id
 * d
 * d  - size
 *
 * [
 * d  - seed id
 * d  - left to buy
 * d  - started amount
 * d  - sell price
 * d  - seed level
 * c
 * d  - reward 1 id
 * c
 * d  - reward 2 id
 * ]
 *
 * @author l3x
 */
public class ExShowSeedInfo extends L2GameServerPacket {

    private FastList<SeedProduction> _seeds = new FastList<SeedProduction>();
    private int _manorId;

    public ExShowSeedInfo(int manorId, FastList<SeedProduction> seeds) {
        _manorId = manorId;
        if (seeds == null || seeds.isEmpty()) {
            return;
        }
        _seeds.addAll(seeds);
    }

    @Override
    protected void writeImpl() {
        writeC(0xFE); // Id
        writeH(0x1C); // SubId
        writeC(0);
        writeD(_manorId); // Manor ID
        writeD(0);
        writeD(_seeds.size());
        for (SeedProduction seed : _seeds) {
            writeD(seed.getId());       // Seed id
            writeD(seed.getCanProduce());   // Left to buy
            writeD(seed.getStartProduce()); // Started amount
            writeD(seed.getPrice());        // Sell Price
            writeD(L2Manor.getInstance().getSeedLevel(seed.getId())); // Seed Level
            writeC(1); // reward 1 Type
            writeD(L2Manor.getInstance().getRewardItemBySeed(seed.getId(), 1)); // Reward 1 Type Item Id
            writeC(1); // reward 2 Type
            writeD(L2Manor.getInstance().getRewardItemBySeed(seed.getId(), 2)); // Reward 2 Type Item Id
        }
        _seeds.clear();
        _seeds = null;
    }
}
