package scripts.commands.voicedcommandhandlers;

import ru.agecold.gameserver.autofarm.AutofarmManager;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import scripts.commands.IVoicedCommandHandler;

public class Autofarm implements IVoicedCommandHandler
{

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target) {
		switch (command) {
			case "farm":
				System.out.println("Command#farm");
				AutofarmManager.getInstance().toggleFarm(activeChar);
				break;
			case "farmon":
				System.out.println("Command#farmon");
				AutofarmManager.getInstance().startFarm(activeChar);
				break;
			case "farmoff":
				System.out.println("Command#farmoff");
				AutofarmManager.getInstance().stopFarm(activeChar);
				break;
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList() {
		return new String[]{ "farm", "farmon", "farmoff"};
	}
}