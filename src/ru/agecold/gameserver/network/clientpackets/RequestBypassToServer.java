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

import javolution.text.TextBuilder;
import ru.agecold.Config;
import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.datatables.CharTemplateTable;
import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.model.*;
import ru.agecold.gameserver.model.BypassManager.DecodedBypass;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.olympiad.OlympiadDiary;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.agecold.gameserver.util.AntiFarm;
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
    private String _command;

    /**
     * @param decrypt
     */
    @Override
    protected void readImpl() {
        _command = readS();
        /*if (_command != null) {
         _command = _command.trim();
         }*/
        //System.out.println("##1#" + _command);
        if (!_command.isEmpty() && isDirectBypass(_command)) {
            if (getClient().getActiveChar() != null) {
                //System.out.println("##" + _command);
                DecodedBypass bypass = getClient().getActiveChar().decodeBypass(_command);
                if (bypass == null) {
                    _command = null;
                } else {
                    _command = bypass.bypass;
                }
                //System.out.println("##" + _command);
            }
        }
    }

    private boolean isDirectBypass(String bps) {
        if (/*bps.startsWith("_bbs")
                 || */bps.startsWith("voice")
                || bps.startsWith("_maillist")
                || bps.startsWith("bbs_add_fav")
                || bps.startsWith("_bbsgetfav")
                || bps.startsWith("_bbsloc")
                || bps.startsWith("_bbsclan")
                || bps.startsWith("_bbsmemo")
                || bps.startsWith("_bbshome")
                || bps.startsWith("_maillist_")
                || bps.startsWith("_friendlist_")) {
            return false;
        }
        return true;
    }

    @Override
    protected void runImpl() {
        //System.out.println("##2#" + _command);
        if (_command == null) {
            return;
        }

        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
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
            if (_command.startsWith("npc_")) {
                if (player.isParalyzed()) {
                    return;
                }
                int endOfId = _command.indexOf('_', 5);
                String id;
                if (endOfId > 0) {
                    id = _command.substring(4, endOfId);
                } else {
                    id = _command.substring(4);
                }

                if (!id.matches("^[0-9]+$")) {
                    player.sendActionFailed();
                    return;
                }

                L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
                if (object == null || endOfId <= 0) {
                    player.sendActionFailed();
                    return;
                }

                if (player.isInsideRadius(object, L2NpcInstance.INTERACTION_DISTANCE, false, false)) {
                    object.onBypassFeedback(player, _command.substring(endOfId + 1));
                }
                player.sendActionFailed();
            } else if (_command.startsWith("Quest ")) {
                if (player.isParalyzed()) {
                    return;
                }

                String p = _command.substring(6).trim();
                int idx = p.indexOf(' ');
                if (idx < 0) {
                    player.processQuestEvent(p, "");
                } else {
                    player.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
                }
            } //	Draw a Symbol
            else if (_command.equals("menu_select?ask=-16&reply=1")) {
                L2Object object = player.getTarget();
                if (object.isL2Npc()) {
                    ((L2NpcInstance) object).onBypassFeedback(player, _command);
                }
            } else if (_command.equals("menu_select?ask=-16&reply=2")) {
                L2Object object = player.getTarget();
                if (object.isL2Npc()) {
                    ((L2NpcInstance) object).onBypassFeedback(player, _command);
                }
            } else if (_command.startsWith("menu_")) {
                IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler("menu_");
                if (vch != null) {
                    vch.useVoicedCommand(_command, player, null);
                }
            } else if (_command.startsWith("security_")) {
                IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler("security_");
                if (vch != null) {
                    vch.useVoicedCommand(_command, player, null);
                }
            } else if (_command.startsWith("vch_")) {
                String[] pars = _command.split("_");
                IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(pars[1] + "_");
                if (vch != null) {
                    _command = _command.replaceAll("vch_", "");
                    vch.useVoicedCommand(_command, player, null);
                }
            } else if (_command.startsWith("admin_") && player.getAccessLevel() >= 1) {
                //if (!AdminCommandAccessRights.getInstance().hasAccess(_command, player.getAccessLevel()))
                // {
                //     _log.info("<GM>" + player + " does not have sufficient privileges for command '" + _command + "'.");
                //     return;
                // }
                if (player.isParalyzed()) {
                    return;
                }

                String command = _command.split(" ")[0];
                IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);

                if (ach != null) {
                    ach.useAdminCommand(_command, player);
                } else {
                    _log.warning("No handler registered for bypass '" + _command + "'");
                }
            } // Navigate throught Manor windows
            else if (_command.startsWith("manor_menu_select?")) {
                L2Object object = player.getTarget();
                if (object.isL2Npc()) {
                    ((L2NpcInstance) object).onBypassFeedback(player, _command);
                }
            } else if (_command.startsWith("bbs_")) {
                CommunityBoard.getInstance().handleCommands(getClient(), _command);
            } else if (_command.startsWith("_bbs")) {
                CommunityBoard.getInstance().handleCommands(getClient(), _command);
            } else if (_command.equals("come_here") && player.getAccessLevel() >= Config.GM_ACCESSLEVEL) {
                comeHere(player);
            } else if (_command.startsWith("player_help ")) {
                playerHelp(player, _command.substring(12));
            } else if (_command.startsWith("olympiad_observ_")) {
                if (!player.inObserverMode()) {
                    return;
                }
                /*
                 * int gameId = Integer.parseInt(_command.substring(16)) + 1;
                 * if(player.getOlympiadGameId() == gameId) return;
                 * if(!L2GrandOlympiad.getInstance().canSpectator(gameId)) {
                 * player.sendMessage("This game not started"); return; }
                 * L2GrandOlympiad.getInstance().teleSpectator(player, gameId);
                 */
            } else if (_command.startsWith("ench_click")) {
                int pwd = 0;
                try {
                    pwd = Integer.parseInt(_command.substring(10).trim());
                } catch (Exception ignored) {
                    //
                }
                if (player.getEnchLesson() == pwd) {
                    player.showAntiClickOk();
                } else {
                    player.showAntiClickPWD();
                }
            } else if (_command.startsWith("four_choose")) {
                int id = 0;
                try {
                    id = Integer.parseInt(_command.substring(11).trim());
                } catch (Exception ignored) {
                    //
                }
                player.setFourSideSkill(id);

            } else if (_command.startsWith("_diary?class=")) // _diary?class=88&page=1
            {
                OlympiadDiary.show(player, _command.substring(13));
            } else if (_command.startsWith("gmpickup")) {
                int id = 0;
                try {
                    id = Integer.parseInt(_command.substring(9).trim());
                }
                catch (Exception e)
                {}
                L2Object obj = L2World.getInstance().findObject(id);
                if (obj == null) {
                    player.sendAdmResultMessage("Ошибка, objid не найден.");
                    return;
                }
                player.doPickupItemForce(obj);
                player.sendActionFailed();
            }
        } catch (Exception e) {
            _log.log(Level.WARNING, "Bad RequestBypassToServer: player " + player.getName() + "", e);
        }
    }

    /**
     * @param client
     */
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
