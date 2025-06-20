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
import ru.agecold.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.agecold.gameserver.model.L2Manor;

/**
 * Format: ch cddd[ddddcdcdcd]
 * c - id (0xFE)
 * h - sub id (0x1D)
 *
 * c
 * d - manor id
 * d
 * d - size
 * [
 * d - crop id
 * d - residual buy
 * d - start buy
 * d - buy price
 * c - reward type
 * d - seed level
 * c - reward 1 items
 * d - reward 1 item id
 * c - reward 2 items
 * d - reward 2 item id
 * ]
 *
 * @author l3x
 */
public class ExShowCropInfo extends L2GameServerPacket {

    private FastList<CropProcure> _crops = new FastList<CropProcure>();
    private int _manorId;

    public ExShowCropInfo(int manorId, FastList<CropProcure> crops) {
        _manorId = manorId;
        if (crops == null || crops.isEmpty()) {
            return;
        }
        _crops.addAll(crops);
    }

    @Override
    protected void writeImpl() {
        writeC(0xFE);     // Id
        writeH(0x1D);     // SubId
        writeC(0);
        writeD(_manorId); // Manor ID
        writeD(0);
        writeD(_crops.size());
        for (CropProcure crop : _crops) {
            writeD(crop.getId());          // Crop id
            writeD(crop.getAmount());      // Buy residual
            writeD(crop.getStartAmount()); // Buy
            writeD(crop.getPrice());       // Buy price
            writeC(crop.getReward());      // Reward
            writeD(L2Manor.getInstance().getSeedLevelByCrop(crop.getId())); // Seed Level
            writeC(1); // rewrad 1 Type
            writeD(L2Manor.getInstance().getRewardItem(crop.getId(), 1));    // Rewrad 1 Type Item Id
            writeC(1); // rewrad 2 Type
            writeD(L2Manor.getInstance().getRewardItem(crop.getId(), 2));    // Rewrad 2 Type Item Id
        }
    }

    @Override
    public void gc() {
        _crops.clear();
        _crops = null;
    }
}
