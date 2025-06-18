package scripts.commands.voicedcommandhandlers;

import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.GMViewCharacterInfo;
import scripts.commands.IVoicedCommandHandler;

public class ModSpecial implements IVoicedCommandHandler {

    private static final String[] VOICED_COMMANDS = {"showstat"};

    @Override
    public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target) {
        if (activeChar.isModerator() && activeChar.getModerRank() <= 2) {
            String[] cmdParams = command.split(" ");

            String name = cmdParams[1];

            L2PcInstance targetPlayer = L2World.getInstance().getPlayer(name);

            if (targetPlayer == null) {
                activeChar.sendModerResultMessage("Игрок не найден или ошибка при наборе ника");
                return false;
            }

            if (command.startsWith("showstat")) {
                activeChar.sendModerResultMessage("Просмотр статов " + targetPlayer.getName());
                activeChar.sendPacket(new GMViewCharacterInfo(targetPlayer));
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String[] getVoicedCommandList() {
        return VOICED_COMMANDS;
    }
}