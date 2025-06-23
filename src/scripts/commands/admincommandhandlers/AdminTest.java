/*
 * $Header: AdminTest.java, 25/07/2005 17:15:21 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 25/07/2005 17:15:21 $
 * $Revision: 1 $
 * $Log: AdminTest.java,v $
 * Revision 1  25/07/2005 17:15:21  luisantonioa
 * Added copyright notice
 *
 *
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
import ru.agecold.Config;
import ru.agecold.gameserver.LoginServerThread;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.datatables.HwidSpamTable;
import ru.agecold.gameserver.instancemanager.bosses.FrintezzaManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.olympiad.Olympiad;
import ru.agecold.gameserver.network.serverpackets.MagicSkillUser;
import ru.agecold.gameserver.network.serverpackets.WareHouseDepositList;
import scripts.autoevents.lasthero.LastHero;
import scripts.commands.IAdminCommandHandler;

/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public class AdminTest implements IAdminCommandHandler {

    private static final int REQUIRED_LEVEL = Config.GM_TEST;
    private static final String[] ADMIN_COMMANDS = {
        "admin_test", "admin_stats", "admin_skill_test",
        "admin_st", "admin_mp", "admin_known", "admin_reconls", "admin_frinta", "admin_cnend", "admin_bubu",
        "admin_mpt", "admin_mpt2", "admin_lsd", "admin_mhs", "admin_bhs", "admin_dhs", "admin_ihs", "admin_epc", "admin_epc2", "admin_epc3"
    };

    /*
     * (non-Javadoc) @see
     * ru.agecold.gameserver.handler.IAdminCommandHandler#useAdminCommand(java.lang.String,
     * ru.agecold.gameserver.model.L2PcInstance)
     */
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (activeChar.getAccessLevel() < REQUIRED_LEVEL) {
                return false;
            }
        }

        if (command.equals("admin_stats")) {
            for (String line : ThreadPoolManager.getInstance().getStats()) {
                activeChar.sendMessage(line);
            }
        } else if (command.equals("admin_frinta")) {
            FrintezzaManager.getInstance().spawnBoss();
        } else if (command.startsWith("admin_skill_test") || command.startsWith("admin_st")) {
            try {
                StringTokenizer st = new StringTokenizer(command);
                st.nextToken();
                int id = Integer.parseInt(st.nextToken());
                adminTestSkill(activeChar, id);
            } catch (NumberFormatException e) {
                activeChar.sendAdmResultMessage("Command format is //skill_test <ID>");
            } catch (NoSuchElementException nsee) {
                activeChar.sendAdmResultMessage("Command format is //skill_test <ID>");
            }
        } else if (command.startsWith("admin_test uni flush")) {
            //Universe.getInstance().flush();
            activeChar.sendAdmResultMessage("Universe Map Saved.");
        } else if (command.startsWith("admin_test uni")) {
            //activeChar.sendAdmResultMessage("Universe Map Size is: "+Universe.getInstance().size());
        } else if (command.equals("admin_mp on")) {
            //.startPacketMonitor();
            activeChar.sendAdmResultMessage("command not working");
        } else if (command.equals("admin_mp off")) {
            //.stopPacketMonitor();
            activeChar.sendAdmResultMessage("command not working");
        } else if (command.equals("admin_mp dump")) {
            //.dumpPacketHistory();
            activeChar.sendAdmResultMessage("command not working");
        } else if (command.equals("admin_known on")) {
            Config.CHECK_KNOWN = true;
        } else if (command.equals("admin_known off")) {
            Config.CHECK_KNOWN = false;
        } else if (command.equals("admin_reconls")) {
            LoginServerThread.getInstance().reConnect();
        } else if (command.startsWith("admin_bubu")) {
            int type = Integer.parseInt(command.substring(11));
            activeChar.sendPacket(new WareHouseDepositList(activeChar, type));
        } else if (command.equals("admin_lsd")) {
            LoginServerThread.getInstance().testDown();
            activeChar.sendAdmResultMessage("Ok.");
        } else if (command.equals("admin_mpt")) {
            return false;
        } else if (command.equals("admin_mhs")) {
            if (activeChar.getTarget() == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            L2PcInstance target = activeChar.getTarget().getPlayer();
            if (target == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            target.setHwidSpamer();
            activeChar.sendAdmResultMessage("Игрок " + target.getName() + " помечен спамером.");
        }
        else if (command.equals("admin_bhs")) {
            if (activeChar.getTarget() == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            L2PcInstance target = activeChar.getTarget().getPlayer();
            if (target == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            HwidSpamTable.banPlayer(target, target.getHWID());
            activeChar.sendAdmResultMessage("Игрок " + target.getName() + " помечен спамером и добавлен в базу.");
        }
        else if (command.equals("admin_dhs")) {
            if (activeChar.getTarget() == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            L2PcInstance target = activeChar.getTarget().getPlayer();
            if (target == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            HwidSpamTable.deleteHwid(target.getHWID());
            activeChar.sendAdmResultMessage("Игрок " + target.getName() + " удален из спамеров и базы.");
        }
        else if (command.equals("admin_ihs")) {
            if (activeChar.getTarget() == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            L2PcInstance target = activeChar.getTarget().getPlayer();
            if (target == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            HwidSpamTable.ignorePlayer(target, target.getHWID());
            activeChar.sendAdmResultMessage("Игрок " + target.getName() + " добавлен в белый список.");
        }
        else if (command.equals("admin_epc")) {
            if (activeChar.getTarget() == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            L2PcInstance target = activeChar.getTarget().getPlayer();
            if (target == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            String htm = "Игрок: " + target.getName() + "<br>";
            htm += "Олимп: <br1>";
            htm += "Запущен: " + Olympiad.inCompPeriod() + "<br1>";
            htm += "Зарегистрирован: " + Olympiad.isRegisteredInComp(target) + "<br><br>";
            htm += "Ласт Хиро: <br1>";
            htm += "Запущен: " + LastHero.getEvent().isStarted() + "<br1>";
            htm += "Зарегистрирован: " + LastHero.getEvent().isRegged(target) + "<br>";
            activeChar.sendHtmlMessage(htm);
        }
        else if (command.equals("admin_epc2")) {
            if (activeChar.getTarget() == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            L2PcInstance target = activeChar.getTarget().getPlayer();
            if (target == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            LastHero.getEvent().delPlayer(target);
            activeChar.sendAdmResultMessage("Игрок " + target.getName() + " удален с ласт хиро.");
        } else if (command.equals("admin_epc3")) {
            if (activeChar.getTarget() == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            L2PcInstance target = activeChar.getTarget().getPlayer();
            if (target == null) {
                activeChar.sendAdmResultMessage("Неправильная цель.");
                return false;
            }
            Olympiad.unRegisterNoble(target);
            activeChar.sendAdmResultMessage("Игрок " + target.getName() + " удален с олимпиады.");
        }
        return true;
    }

    /**
     * @param activeChar
     * @param id
     */
    private void adminTestSkill(L2PcInstance activeChar, int id) {
        L2Character player;
        L2Object target = activeChar.getTarget();
        if (target == null || !(target.isL2Character())) {
            player = activeChar;
        } else {
            player = (L2Character) target;
        }
        player.broadcastPacket(new MagicSkillUser(activeChar, player, id, 1, 1, 1));

    }
    /*
     * (non-Javadoc) @see
     * ru.agecold.gameserver.handler.IAdminCommandHandler#getAdminCommandList()
     */

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}
