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

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.lib.Log;
import ru.agecold.gameserver.model.*;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.olympiad.OlympiadDiary;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.agecold.gameserver.util.BypassStorage.ValidBypass;
import scripts.commands.AdminCommandHandler;
import scripts.commands.IAdminCommandHandler;
import scripts.commands.IVoicedCommandHandler;
import scripts.commands.VoicedCommandHandler;
import scripts.communitybbs.CommunityBoard;

/**
 * This class ...
 *
 * @version $Revision: 1.12.4.5 $ $Date: 2005/04/11 10:06:11 $
 */
public final class RequestBypassToServer extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(RequestBypassToServer.class.getName());
    // S
    private String _bypass = null;

    @Override
    protected void readImpl() {
        _bypass = readS();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if(player == null || _bypass.isEmpty())
            return;
        ValidBypass bp = player.getBypassStorage().validate(_bypass);
        if(bp == null)
        {
            player.sendActionFailed();
            if(!_bypass.startsWith("bbs_add_fav"))
            {
                L2Object nc = player.getTarget();
                Log.add("Direct access to bypass: " + _bypass + "| Player: " + player + (nc != null && nc.isL2Npc() ? " target: " + nc.getNpcId() + "[" + nc.getObjectId() + "] " + nc.getX() + "," + nc.getY() + "," + nc.getZ() : ""), "bypass");
            }
            return;
        }
        if (System.currentTimeMillis() - player.getCPA() < 100) {
            return;
        }
        player.setCPA();

        if (player.getActiveTradeList() != null) {
            player.cancelActiveTrade();
        }

        try {
            if (bp.bypass.startsWith("npc_")) {
                if (player.isParalyzed()) {
                    return;
                }
                int endOfId = bp.bypass.indexOf('_', 5);
                String id;
                if (endOfId > 0) {
                    id = bp.bypass.substring(4, endOfId);
                } else {
                    id = bp.bypass.substring(4);
                }

                L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
                if (object == null || endOfId <= 0) {
                    player.sendActionFailed();
                    return;
                }

                if (player.isInsideRadius(object, L2NpcInstance.INTERACTION_DISTANCE, false, false)) {
                    ((L2NpcInstance) object).onBypassFeedback(player, _bypass.substring(endOfId + 1));
                }
                player.sendActionFailed();
            } else if (bp.bypass.startsWith("Quest ")) {
                if (player.isParalyzed()) {
                    return;
                }

                String p = bp.bypass.substring(6).trim();
                int idx = p.indexOf(' ');
                if (idx < 0) {
                    player.processQuestEvent(p, "");
                } else {
                    player.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
                }
            } //	Draw a Symbol
            else if (bp.bypass.equals("menu_select?ask=-16&reply=1")) {
                L2Object object = player.getTarget();
                if(object != null && object.isL2Npc()) {
                    ((L2NpcInstance) object).onBypassFeedback(player, bp.bypass);
                }
            } else if (bp.bypass.equals("menu_select?ask=-16&reply=2")) {
                L2Object object = player.getTarget();
                if(object != null && object.isL2Npc()) {
                    ((L2NpcInstance) object).onBypassFeedback(player, bp.bypass);
                }
            } else if (bp.bypass.startsWith("menu_")) {
                IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler("menu_");
                if (vch != null) {
                    vch.useVoicedCommand(bp.bypass, player, null);
                }
            } else if (bp.bypass.startsWith("security_")) {
                IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler("security_");
                if (vch != null) {
                    vch.useVoicedCommand(bp.bypass, player, null);
                }
            } else if (bp.bypass.startsWith("vch_")) {
                String[] pars = bp.bypass.split("_");
                IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(pars[1] + "_");
                if (vch != null) {
                    bp.bypass = bp.bypass.replaceAll("vch_", "");
                    vch.useVoicedCommand(bp.bypass, player, null);
                }
            } else if (bp.bypass.startsWith("admin_") && player.getAccessLevel() >= 1) {
                //if (!AdminCommandAccessRights.getInstance().hasAccess(bp.bypass, player.getAccessLevel()))
                // {
                //     _log.info("<GM>" + player + " does not have sufficient privileges for command '" + bp.bypass + "'.");
                //     return;
                // }
                if (player.isParalyzed()) {
                    return;
                }

                IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(bp.bypass);

                if (ach != null) {
                    try
                    {
                        ach.useAdminCommand(_bypass, player);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                } else {
                    _log.warning("No handler registered for bypass '" + bp.bypass + "'");
                }
            } // Navigate throught Manor windows
            else if (bp.bypass.startsWith("manor_menu_select?")) {
                L2Object object = player.getTarget();
                if(object != null && object.isL2Npc()) {
                    ((L2NpcInstance) object).onBypassFeedback(player, bp.bypass);
                }
            } else if (bp.bypass.equals("come_here") && player.getAccessLevel() >= Config.GM_ACCESSLEVEL) {
                comeHere(player);
            } else if (bp.bypass.startsWith("player_help ")) {
                playerHelp(player, bp.bypass.substring(12));
            } else if (bp.bypass.startsWith("olympiad_observ_")) {
                if (!player.inObserverMode()) {
                    return;
                }
                /*
                 * int gameId = Integer.parseInt(bp.bypass.substring(16)) + 1;
                 * if(player.getOlympiadGameId() == gameId) return;
                 * if(!L2GrandOlympiad.getInstance().canSpectator(gameId)) {
                 * player.sendMessage("This game not started"); return; }
                 * L2GrandOlympiad.getInstance().teleSpectator(player, gameId);
                 */
            } else if (bp.bypass.startsWith("ench_click")) {
                int pwd = 0;
                try {
                    pwd = Integer.parseInt(bp.bypass.substring(10).trim());
                } catch (Exception ignored) {
                    //
                }
                if (player.getEnchLesson() == pwd) {
                    player.showAntiClickOk();
                } else {
                    player.showAntiClickPWD();
                }
            } else if (bp.bypass.startsWith("four_choose")) {
                int id = 0;
                try {
                    id = Integer.parseInt(bp.bypass.substring(11).trim());
                } catch (Exception ignored) {
                    //
                }
                player.setFourSideSkill(id);

            } else if (bp.bypass.startsWith("_diary?class=")) // _diary?class=88&page=1
            {
                OlympiadDiary.show(player, bp.bypass.substring(13));
            } else if (bp.bypass.startsWith("gmpickup")) {
                int id = 0;
                try {
                    id = Integer.parseInt(bp.bypass.substring(9).trim());
                }
                catch (Exception e)
                {}
                L2Object obj = L2World.getInstance().findObject(id);
                if (obj == null) {
                    player.sendAdmResultMessage("Error, objId not found.");
                    return;
                }
                player.doPickupItemForce(obj);
                player.sendActionFailed();
            } else if(bp.bbs) {
                CommunityBoard.getInstance().handleCommands(getClient(), bp.bypass);
            }
        } catch (Exception e) {
            _log.log(Level.WARNING, "Bad RequestBypassToServer: player " + player.getName() + "", e);
        }
    }

    private void comeHere(L2PcInstance player) {
        L2Object obj = player.getTarget();
        if (obj == null) {
            return;
        }
        if (obj.isL2Npc()) {
            L2NpcInstance temp = (L2NpcInstance) obj;
            temp.setTarget(player);
            temp.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,
                    new L2CharPosition(player.getX(), player.getY(), player.getZ(), 0));
//			temp.moveTo(player.getX(),player.getY(), player.getZ(), 0 );
        }

    }

    private void playerHelp(L2PcInstance player, String path) {
        if (path.indexOf("..") != -1) {
            return;
        }

        String filename = "data/html/help/" + path;
        NpcHtmlMessage html = NpcHtmlMessage.id(1);
        html.setFile(filename);
        player.sendPacket(html);
    }
}
