package scripts.commands.voicedcommandhandlers;

import ru.agecold.Config;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import scripts.commands.IVoicedCommandHandler;

public class Acp implements IVoicedCommandHandler
{
    private static final String[] VOICED_COMMANDS = { "acp", "hp_", "mp_", "cp_", "hps_", "mps_", "cps_" };

    @Override
    public boolean useVoicedCommand(String command, L2PcInstance player, String target) {
        if (!Config.ACP_ENGINE) {
            return false;
        }
        if (Config.ACP_ENGINE_PREMIUM && !player.isPremium()) {
            player.sendMessage("Команда доступна только для премиумов.");
            return false;
        }
        if (Config.ACP_EVENT_FORB && player.isInEvent()) {
            player.sendMessage("Команда запрещена для использования на ивентах.");
            return false;
        }
        if (command.startsWith("hp_")) {
            player.setAutoHp(!player.hasAutoHp());
        } else if (command.startsWith("mp_")) {
            player.setAutoMp(!player.hasAutoMp());
        } else if (command.startsWith("cp_")) {
            player.setAutoCp(!player.hasAutoCp());
        } else if (command.startsWith("hps_")) {
            int pc = 0;
            try {
                pc = Integer.parseInt(command.substring(5));
            } catch (Exception e) {
                pc = Config.ACP_HP_PC;
            }
            player.setAutoHpPc(pc);
        }
        else if (command.startsWith("mps_")) {
            int pc = 0;
            try {
                pc = Integer.parseInt(command.substring(5));
            } catch (Exception e) {
                pc = Config.ACP_MP_PC;
            }
            player.setAutoMpPc(pc);
        } else if (command.startsWith("cps_")) {
            int pc = 0;
            try {
                pc = Integer.parseInt(command.substring(5));
            } catch (Exception e) {
                pc = Config.ACP_CP_PC;
            }
            player.setAutoCpPc(pc);
        }
        showHome(player);
        return true;
    }

    private void showHome(L2PcInstance player) {
        NpcHtmlMessage htm = NpcHtmlMessage.id(0);
        htm.setFile("data/html/acp_service.htm");

        htm.replace("%STATUS_HP%", player.hasAutoHp() ? "Вкл" : "Выкл");
        htm.replace("%STATUS_MP%", player.hasAutoMp() ? "Вкл" : "Выкл");
        htm.replace("%STATUS_CP%", player.hasAutoCp() ? "Вкл" : "Выкл");

        htm.replace("%HP_PC%", player.getAutoHpPc() + "");
        htm.replace("%MP_PC%", player.getAutoMpPc() + "");
        htm.replace("%CP_PC%", player.getAutoCpPc() + "");

        player.sendPacket(htm);
    }

    @Override
    public String[] getVoicedCommandList() {
        return VOICED_COMMANDS;
    }
}