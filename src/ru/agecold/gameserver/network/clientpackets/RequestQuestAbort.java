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

import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.instancemanager.QuestManager;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.quest.Quest;
import ru.agecold.gameserver.model.quest.QuestState;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.QuestList;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestQuestAbort extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestQuestAbort.class.getName());
	private int _questId;

	@Override
	protected void readImpl()
	{
		_questId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		    return;

        Quest qe = QuestManager.getInstance().getQuest(_questId);
        if (qe != null)
        {
    		QuestState qs = player.getQuestState(qe.getName());
            if(qs != null)
            {
        		qs.exitQuest(true);
                player.sendPacket(new QuestList(player));
                player.sendPacket(SystemMessage.id(SystemMessageId.S1_S2).addString("Quest aborted."));
            }
			// else
            //{
            //    if (Config.DEBUG) _log.info("Player '"+player.getName()+"' try to abort quest "+qe.getName()+" but he didn't have it started.");
            //}
        } 
		//else
        //{
        //    if (Config.DEBUG) _log.warning("Quest (id='"+_questId+"') not found.");
        //}
	}

	/* (non-Javadoc)
	 * @see ru.agecold.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[C] 64 RequestQuestAbort";
	}
}