package scripts.commands.usercommandhandlers;

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2CommandChannel;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import scripts.commands.IUserCommandHandler;

public class ChannelCreate implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS = { 92 };

    public boolean useUserCommand(int id, L2PcInstance activeChar)
    {
        if (id != COMMAND_IDS[0]) 
			return false;			
		
		// CC могут создавать только лидеры партий, состоящие в клане ранком не ниже барона, так же не состоящие еще в СС
		if(activeChar.getClan() == null || !activeChar.isInParty() || !activeChar.getParty().isLeader(activeChar) || activeChar.getParty().isInCommandChannel())
			return false;

		new L2CommandChannel(activeChar); // Создаём Command Channel
		activeChar.sendPacket(Static.THE_COMMAND_CHANNEL_HAS_BEEN_FORMED);

        return true;
    }

    /* (non-Javadoc)
     * @see ru.agecold.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    public int[] getUserCommandList()
    {
        return COMMAND_IDS;
    }
}
