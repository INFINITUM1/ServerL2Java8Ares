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
package scripts.commands.admincommandhandlers;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import ru.agecold.Config;
import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.datatables.NpcTable;
import ru.agecold.gameserver.datatables.SpawnTable;
import scripts.commands.IAdminCommandHandler;
import ru.agecold.gameserver.model.GMAudit;
import ru.agecold.gameserver.model.L2CharPosition;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Spawn;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.L2NpcTemplate;

/**
 * This class handles following admin commands:
 * - show_moves
 * - show_teleport
 * - teleport_to_character
 * - move_to
 * - teleport_character
 *
 * @version $Revision: 1.3.2.6.2.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminTeleport implements IAdminCommandHandler {

    private static final Logger _log = Logger.getLogger(AdminTeleport.class.getName());
    private static final String[] ADMIN_COMMANDS = {
        "admin_show_moves",
        "admin_show_moves_other",
        "admin_show_teleport",
        "admin_teleport_to_character",
        "admin_teleportto",
        "admin_move_to",
        "admin_teleport_character",
        "admin_recall",
        "admin_walk",
        "admin_explore",
        "teleportto",
        "recall",
        "admin_recall_npc",
        "admin_gonorth",
        "admin_gosouth",
        "admin_goeast",
        "admin_gowest",
        "admin_goup",
        "admin_godown",
        "admin_tele",
        "admin_tptoNpc",
        "admin_teleto",};
    private static final int REQUIRED_LEVEL = Config.GM_TELEPORT;
    private static final int REQUIRED_LEVEL2 = Config.GM_TELEPORT_OTHER;

    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) {
                return false;
            }
        }

        String target = (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

        if (command.equals("admin_teleto")) {
            activeChar.setTeleMode(1);
        }
        if (command.equals("admin_teleto r")) {
            activeChar.setTeleMode(2);
        }
        if (command.equals("admin_teleto end")) {
            activeChar.setTeleMode(0);
        }
        if (command.equals("admin_show_moves")) {
            AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
        }
        if (command.equals("admin_show_moves_other")) {
            AdminHelpPage.showHelpPage(activeChar, "tele/other.html");
        } else if (command.equals("admin_show_teleport")) {
            showTeleportCharWindow(activeChar);
        } else if (command.equals("admin_recall_npc")) {
            recallNPC(activeChar);
        } else if (command.equals("admin_teleport_to_character")) {
            teleportToCharacter(activeChar, activeChar.getTarget());
        } else if (command.equals("admin_explore") && Config.ACTIVATE_POSITION_RECORDER) {
            activeChar._exploring = !activeChar._exploring;
            activeChar.explore();
        } else if (command.startsWith("admin_walk")) {
            try {
                String val = command.substring(11);
                StringTokenizer st = new StringTokenizer(val);
                String x1 = st.nextToken();
                int x = Integer.parseInt(x1);
                String y1 = st.nextToken();
                int y = Integer.parseInt(y1);
                String z1 = st.nextToken();
                int z = Integer.parseInt(z1);
                L2CharPosition pos = new L2CharPosition(x, y, z, 0);
                activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
            } catch (Exception e) {
                if (Config.DEBUG) {
                    _log.info("admin_walk: " + e);
                }
            }
        } else if (command.startsWith("admin_move_to")) {
            try {
                String val = command.substring(14);
                teleportTo(activeChar, val);
            } catch (StringIndexOutOfBoundsException e) {
                //Case of empty or missing coordinates
                AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
            }
        } else if (command.startsWith("admin_teleport_character")) {
            try {
                String val = command.substring(25);

                if (activeChar.getAccessLevel() >= REQUIRED_LEVEL2) {
                    teleportCharacter(activeChar, val);
                }
            } catch (StringIndexOutOfBoundsException e) {
                //Case of empty coordinates
                activeChar.sendAdmResultMessage("Wrong or no Coordinates given.");
                showTeleportCharWindow(activeChar); //back to character teleport
            }
        } else if (command.startsWith("admin_teleportto ")) {
            try {
                String targetName = command.substring(17);
                //Util.nameSlashes(targetName);
                targetName = targetName.replaceAll("&lt;", "<");
                targetName = targetName.replaceAll("&gt;", ">");
                targetName = targetName.replaceAll("&excl;", "!");
                targetName = targetName.replaceAll("&commat;", "@");
                targetName = targetName.replaceAll("&num;", "#");
                targetName = targetName.replaceAll("&dollar;", "$");
                targetName = targetName.replaceAll("&percnt;", "%");
                targetName = targetName.replaceAll("&Hat;", "^");
                targetName = targetName.replaceAll("&ast;", "*");
                targetName = targetName.replaceAll("&lpar;", "(");
                targetName = targetName.replaceAll("&rpar;", ")");
                targetName = targetName.replaceAll("&period;", ".");
                targetName = targetName.replaceAll("&comma;", ",");
                targetName = targetName.replaceAll("&semi;", ";");
                targetName = targetName.replaceAll("&sol;", "/");
                targetName = targetName.replaceAll("&verbar;", "|");
                //targetName = targetName.replaceAll("&bsol;", "\");
                targetName = targetName.replaceAll("&quest;", "?");
                targetName = targetName.replaceAll("&plus;", "+");
                targetName = targetName.replaceAll("&colon;", ":");
                targetName = targetName.replaceAll("&apos;", "'");
                targetName = targetName.replaceAll("&lowbar;", "_");
                L2PcInstance player = L2World.getInstance().getPlayer(targetName);
                teleportToCharacter(activeChar, player);
            } catch (StringIndexOutOfBoundsException e) {
            }
        } else if (command.startsWith("admin_recall ")) {
            try {
                String targetName = command.substring(13);
                targetName = targetName.replaceAll("&lt;", "<");
                targetName = targetName.replaceAll("&gt;", ">");
                targetName = targetName.replaceAll("&excl;", "!");
                targetName = targetName.replaceAll("&commat;", "@");
                targetName = targetName.replaceAll("&num;", "#");
                targetName = targetName.replaceAll("&dollar;", "$");
                targetName = targetName.replaceAll("&percnt;", "%");
                targetName = targetName.replaceAll("&Hat;", "^");
                targetName = targetName.replaceAll("&ast;", "*");
                targetName = targetName.replaceAll("&lpar;", "(");
                targetName = targetName.replaceAll("&rpar;", ")");
                targetName = targetName.replaceAll("&period;", ".");
                targetName = targetName.replaceAll("&comma;", ",");
                targetName = targetName.replaceAll("&semi;", ";");
                targetName = targetName.replaceAll("&sol;", "/");
                targetName = targetName.replaceAll("&verbar;", "|");
                //targetName = targetName.replaceAll("&bsol;", "\");
                targetName = targetName.replaceAll("&quest;", "?");
                targetName = targetName.replaceAll("&plus;", "+");
                targetName = targetName.replaceAll("&colon;", ":");
                targetName = targetName.replaceAll("&apos;", "'");
                targetName = targetName.replaceAll("&lowbar;", "_");
                L2PcInstance player = L2World.getInstance().getPlayer(targetName);
                if (activeChar.getAccessLevel() >= REQUIRED_LEVEL2) {
                    teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ());
                }
            } catch (StringIndexOutOfBoundsException e) {
            }
        } else if (command.startsWith("admin_tptoNpc ")) {
            try {
                Integer oID = Integer.parseInt(command.substring(14));
                L2Object npc = L2World.getInstance().findObject(oID);
                if (npc == null) {
                    activeChar.sendMessage("No NPC founded.");
                    return false;
                }
                teleportToNpc(npc, activeChar);
            } catch (StringIndexOutOfBoundsException e) {
            }
        } else if (command.equals("admin_tele")) {
            showTeleportWindow(activeChar);
        } else if (command.startsWith("admin_go")) {
            int intVal = 150;
            int x = activeChar.getX(), y = activeChar.getY(), z = activeChar.getZ();
            try {
                String val = command.substring(8);
                StringTokenizer st = new StringTokenizer(val);
                String dir = st.nextToken();
                if (st.hasMoreTokens()) {
                    intVal = Integer.parseInt(st.nextToken());
                }
                if (dir.equals("east")) {
                    x += intVal;
                } else if (dir.equals("west")) {
                    x -= intVal;
                } else if (dir.equals("north")) {
                    y -= intVal;
                } else if (dir.equals("south")) {
                    y += intVal;
                } else if (dir.equals("up")) {
                    z += intVal;
                } else if (dir.equals("down")) {
                    z -= intVal;
                }
                activeChar.teleToLocation(x, y, z, false);
                showTeleportWindow(activeChar);
            } catch (Exception e) {
                activeChar.sendAdmResultMessage("Usage: //go<north|south|east|west|up|down> [offset] (default 150)");
            }
        }

        return true;
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }

    private void teleportTo(L2PcInstance activeChar, String Cords) {
        try {
            StringTokenizer st = new StringTokenizer(Cords);
            String x1 = st.nextToken();
            int x = Integer.parseInt(x1);
            String y1 = st.nextToken();
            int y = Integer.parseInt(y1);
            String z1 = st.nextToken();
            int z = Integer.parseInt(z1);

            activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            activeChar.teleToLocation(x, y, z, false);

            SystemMessage sm = SystemMessage.id(SystemMessageId.S1_S2);
            sm.addString("You have been teleported to " + Cords);
            activeChar.sendPacket(sm);
        } catch (NoSuchElementException nsee) {
            activeChar.sendAdmResultMessage("Wrong or no Coordinates given.");
        }
    }

    private void showTeleportWindow(L2PcInstance activeChar) {
        AdminHelpPage.showHelpPage(activeChar, "move.htm");
    }

    private void showTeleportCharWindow(L2PcInstance activeChar) {
        L2Object target = activeChar.getTarget();
        L2PcInstance player = null;
        if (target.isPlayer()) {
            player = (L2PcInstance) target;
        } else {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
            return;
        }
        NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);

        TextBuilder replyMSG = new TextBuilder("<html><title>Teleport Character</title>");
        replyMSG.append("<body>");
        replyMSG.append("The character you will teleport is " + player.getName() + ".");
        replyMSG.append("<br>");

        replyMSG.append("Co-ordinate x");
        replyMSG.append("<edit var=\"char_cord_x\" width=110>");
        replyMSG.append("Co-ordinate y");
        replyMSG.append("<edit var=\"char_cord_y\" width=110>");
        replyMSG.append("Co-ordinate z");
        replyMSG.append("<edit var=\"char_cord_z\" width=110>");
        replyMSG.append("<button value=\"Teleport\" action=\"bypass -h admin_teleport_character $char_cord_x $char_cord_y $char_cord_z\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
        replyMSG.append("<button value=\"Teleport near you\" action=\"bypass -h admin_teleport_character " + activeChar.getX() + " " + activeChar.getY() + " " + activeChar.getZ() + "\" width=115 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
        replyMSG.append("<center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
        replyMSG.append("</body></html>");

        adminReply.setHtml(replyMSG.toString());
        activeChar.sendPacket(adminReply);
    }

    private void teleportCharacter(L2PcInstance activeChar, String Cords) {
        L2Object target = activeChar.getTarget();
        L2PcInstance player = null;
        if (target.isPlayer()) {
            player = (L2PcInstance) target;
        } else {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
            return;
        }

        if (player.getObjectId() == activeChar.getObjectId()) {
            player.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_USE_ON_YOURSELF));
        } else {
            try {
                StringTokenizer st = new StringTokenizer(Cords);
                String x1 = st.nextToken();
                int x = Integer.parseInt(x1);
                String y1 = st.nextToken();
                int y = Integer.parseInt(y1);
                String z1 = st.nextToken();
                int z = Integer.parseInt(z1);
                teleportCharacter(player, x, y, z);
            } catch (NoSuchElementException nsee) {
            }
        }
    }

    /**
     * @param player
     * @param x
     * @param y
     * @param z
     */
    private void teleportCharacter(L2PcInstance player, int x, int y, int z) {
        if (player != null) {
            //Common character information
            player.sendAdmResultMessage("Admin is teleporting you.");

            player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            player.teleToLocation(x, y, z, true);
        }
    }

    private void teleportToCharacter(L2PcInstance activeChar, L2Object target) {
        L2PcInstance player = null;
        if (target != null && target.isPlayer()) {
            player = (L2PcInstance) target;
        } else {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
            return;
        }

        if (player.getObjectId() == activeChar.getObjectId()) {
            player.sendPacket(SystemMessage.id(SystemMessageId.CANNOT_USE_ON_YOURSELF));
        } else {
            int x = player.getX();
            int y = player.getY();
            int z = player.getZ();

            activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            activeChar.teleToLocation(x, y, z, true);

            activeChar.sendAdmResultMessage("You have teleported to character " + player.getName() + ".");
        }
    }

    private void recallNPC(L2PcInstance activeChar) {
        L2Object obj = activeChar.getTarget();
        if ((obj != null) && (obj.isL2Npc())) {
            L2NpcInstance target = (L2NpcInstance) obj;

            int monsterTemplate = target.getTemplate().npcId;
            L2NpcTemplate template1 = NpcTable.getInstance().getTemplate(monsterTemplate);
            if (template1 == null) {
                activeChar.sendAdmResultMessage("Incorrect monster template.");
                _log.warning("ERROR: NPC " + target.getObjectId() + " has a 'null' template.");
                return;
            }

            L2Spawn spawn = target.getSpawn();
            if (spawn == null) {
                activeChar.sendAdmResultMessage("Incorrect monster spawn.");
                _log.warning("ERROR: NPC " + target.getObjectId() + " has a 'null' spawn.");
                return;
            }
            int respawnTime = spawn.getRespawnDelay();

            target.deleteMe();
            spawn.stopRespawn();
            SpawnTable.getInstance().deleteSpawn(spawn, true);

            try {
                //L2MonsterInstance mob = new L2MonsterInstance(monsterTemplate, template1);

                spawn = new L2Spawn(template1);
                spawn.setLocx(activeChar.getX());
                spawn.setLocy(activeChar.getY());
                spawn.setLocz(activeChar.getZ());
                spawn.setAmount(1);
                spawn.setHeading(activeChar.getHeading());
                spawn.setRespawnDelay(respawnTime);
                SpawnTable.getInstance().addNewSpawn(spawn, true);
                spawn.init();

                SystemMessage sm = SystemMessage.id(SystemMessageId.S1_S2);
                sm.addString("Created " + template1.name + " on " + target.getObjectId() + ".");
                activeChar.sendPacket(sm);

                if (Config.DEBUG) {
                    _log.fine("Spawn at X=" + spawn.getLocx() + " Y=" + spawn.getLocy() + " Z=" + spawn.getLocz());
                    _log.warning("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") moved NPC " + target.getObjectId());
                }
            } catch (Exception e) {
                activeChar.sendAdmResultMessage("Target is not in game.");
            }

        } else {
            activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
        }
    }

    private void teleportToNpc(L2Object npc, L2PcInstance player) {
        player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        player.teleToLocation(npc.getX(), npc.getY(), npc.getZ(), true);
    }
}
