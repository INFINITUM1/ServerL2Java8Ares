package scripts.commands.admincommandhandlers;

import ru.agecold.Config;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.Hero;
import ru.agecold.gameserver.model.entity.olympiad.Olympiad;
import ru.agecold.gameserver.model.entity.olympiad.OlympiadDatabase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.agecold.gameserver.model.entity.olympiad.ValidationTask;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import scripts.commands.IAdminCommandHandler;

public class AdminHero implements IAdminCommandHandler {

    private static String[] _adminCommands = {"admin_sethero", "admin_manualhero"};
    private final static Log _log = LogFactory.getLog(AdminHero.class.getName());
    private static final int REQUIRED_LEVEL = Config.GM_MENU;

    public boolean useAdminCommand(String command, L2PcInstance admin) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (!(checkLevel(admin.getAccessLevel()) && admin.isGM())) {
                return false;
            }
        }
        if (command.startsWith("admin_sethero")) {
            L2Object obj = admin.getTarget();
            if (!obj.isPlayer()) {
                return false;
            }

            L2PcInstance target = (L2PcInstance) obj;
            if (target.isHero()) {
                target.setHero(false);
                target.setHeroExpire(0);
                target.broadcastUserInfo();
                admin.sendAdmResultMessage("Забрали стутас героя у игрока " + target.getName());
                /*
                 * Connect con = null; PreparedStatement st = null; try { con =
                 * L2DatabaseFactory.getConnection(); st =
                 * con.prepareStatement("UPDATE characters SET hero=0 WHERE
                 * obj_id=?"); st.setInt(1, target.getObjectId()); st.execute();
                 * } catch (SQLException e) { _log.warn("could not unset Hero
                 * stats of char:", e); } finally { Close.CS(con, st); }
                 */
            } else {
                int days = 30;
                try {
                    days = Integer.parseInt(command.substring(14).trim());
                } catch (Exception e) {
                    days = 30;
                }
                admin.sendAdmResultMessage("Выдан стутас героя игроку " + target.getName() + " на " + days + " дней.");
                target.setHero(days);
            }
        } else if (command.startsWith("admin_manualhero")) {
            Announcements.getInstance().announceToAll(SystemMessage.id(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED).addNumber(Olympiad._currentCycle));
            Announcements.getInstance().announceToAll("Olympiad Validation Period has began");

            Olympiad._isOlympiadEnd = true;
            if (Olympiad._scheduledManagerTask != null) {
                Olympiad._scheduledManagerTask.cancel(true);
            }
            if (Olympiad._scheduledWeeklyTask != null) {
                Olympiad._scheduledWeeklyTask.cancel(true);
            }
            Olympiad._validationEnd = Olympiad._olympiadEnd + Config.ALT_OLY_VPERIOD;

            OlympiadDatabase.saveNobleData();

            Olympiad._period = 1;
            Hero.getInstance().clearHeroes();
            try {
                OlympiadDatabase.save();
            }
            catch (Exception e) {
                Olympiad._log.warning("Olympiad System: Failed to save Olympiad configuration: " + e);
            }
            _log.warn("Olympiad System: Starting Validation period. Time to end validation: 1000ms");
            if (Olympiad._scheduledValdationTask != null) {
                Olympiad._scheduledValdationTask.cancel(true);
            }
            Olympiad._scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationTask(), 1000);
        }
        return false;
    }

    public String[] getAdminCommandList() {
        return _adminCommands;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }
}