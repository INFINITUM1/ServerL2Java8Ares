package scripts.commands.voicedcommandhandlers;

import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import scripts.commands.IVoicedCommandHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ServerTime implements IVoicedCommandHandler
{
	private static final String[] _commandList = { "servertime", "serverdate" };
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if(command.equals("servertime"))
		{
			activeChar.sendMessage(DATE_FORMAT.format(new Date(System.currentTimeMillis())));
			return true;
		}
		else if(command.equals("serverdate"))
		{
			activeChar.sendMessage(DATE_FORMAT.format(new Date(Calendar.getInstance().getTimeInMillis())));
			return true;
		}
		return false;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}