
package ru.agecold.gameserver.network.clientpackets;

//import java.util.logging.Level;
//import java.util.logging.LogRecord;
//import java.util.logging.Logger;

//import ru.agecold.Config;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.FriendRecvMsg;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

/**
 * Recieve Private (Friend) Message - 0xCC
 *
 * Format: c SS
 *
 * S: Message
 * S: Receiving Player
 *
 * @author Tempy
 *
 */
public final class RequestSendFriendMsg extends L2GameClientPacket
{
    private String _message;
    private String _reciever;

    @Override
	protected void readImpl()
    {
        _message = readS();
        _reciever = readS();
    }

    @Override
	protected void runImpl()
    {
    	L2PcInstance player = getClient().getActiveChar();
    	if (player == null) 
			return;
			
        L2PcInstance targetPlayer = L2World.getInstance().getPlayer(_reciever);
        if (targetPlayer == null)
        {
        	player.sendPacket(Static.TARGET_IS_NOT_FOUND_IN_THE_GAME);
        	return;
        }
		
		if (!player.haveFriend(targetPlayer.getObjectId()))
		{
        	//player.sendPacket(SystemMessage.id(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME));
        	player.sendPacket(Static.FRIEND_NOT_FOUND);
        	return;
		}
		
		targetPlayer.sendPacket(new FriendRecvMsg(player.getName(), _reciever, _message));
    }
}