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
package scripts.commands.usercommandhandlers;

import ru.agecold.Config;
import ru.agecold.gameserver.datatables.MapRegionTable;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import scripts.commands.IUserCommandHandler;

/**
 *
 *
 */
public class Loc implements IUserCommandHandler {

    private static final int[] COMMAND_IDS = {0};

    /*
     * (non-Javadoc) @see
     * ru.agecold.gameserver.handler.IUserCommandHandler#useUserCommand(int,
     * ru.agecold.gameserver.model.L2PcInstance)
     */
    public boolean useUserCommand(int id, L2PcInstance activeChar) {
        if (!Config.ENABLE_LOC_COMMAND)
        {
            return true;
        }
        int _nearestTown = MapRegionTable.getInstance().getClosestTownNumber(activeChar);
        SystemMessageId msg;
        switch (_nearestTown) {
            case 0:
                msg = SystemMessageId.LOC_TI_S1_S2_S3;
                break;
            case 1:
                msg = SystemMessageId.LOC_ELVEN_S1_S2_S3;
                break;
            case 2:
                msg = SystemMessageId.LOC_DARK_ELVEN_S1_S2_S3;
                break;
            case 3:
                msg = SystemMessageId.LOC_ORC_S1_S2_S3;
                break;
            case 4:
                msg = SystemMessageId.LOC_DWARVEN_S1_S2_S3;
                break;
            case 5:
                msg = SystemMessageId.LOC_GLUDIO_S1_S2_S3;
                break;
            case 6:
                msg = SystemMessageId.LOC_GLUDIN_S1_S2_S3;
                break;
            case 7:
                msg = SystemMessageId.LOC_DION_S1_S2_S3;
                break;
            case 8:
                msg = SystemMessageId.LOC_GIRAN_S1_S2_S3;
                break;
            case 9:
                msg = SystemMessageId.LOC_OREN_S1_S2_S3;
                break;
            case 10:
                msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
                break;
            case 11:
                msg = SystemMessageId.LOC_HUNTER_S1_S2_S3;
                break;
            case 12:
                msg = SystemMessageId.LOC_GIRAN_HARBOR_S1_S2_S3;
                break;
            case 13:
                msg = SystemMessageId.LOC_HEINE_S1_S2_S3;
                break;
            case 14:
                msg = SystemMessageId.LOC_RUNE_S1_S2_S3;
                break;
            case 15:
                msg = SystemMessageId.LOC_GODDARD_S1_S2_S3;
                break;
            case 16:
                msg = SystemMessageId.LOC_SCHUTTGART_S1_S2_S3;
                break;
            case 17:
                msg = SystemMessageId.LOC_FLORAN_S1_S2_S3;
                break;
            case 18:
                msg = SystemMessageId.LOC_PRIMEVAL_ISLE_S1_S2_S3;
                break;
            default:
                msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
        }
        activeChar.sendPacket(SystemMessage.id(msg).addNumber(activeChar.getX()).addNumber(activeChar.getY()).addNumber(activeChar.getZ()));
        return true;
    }

    /*
     * (non-Javadoc) @see
     * ru.agecold.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}
