/*
 * $Header: IllegalPlayerAction.java, 21/10/2005 23:32:02 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 21/10/2005 23:32:02 $
 * $Revision: 1 $
 * $Log: IllegalPlayerAction.java,v $
 * Revision 1  21/10/2005 23:32:02  luisantonioa
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
package ru.agecold.gameserver.util;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.GmListTable;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public final class IllegalPlayerAction implements Runnable
{
	private static Logger _logAudit = Logger.getLogger("audit");

    String _message;
    int _punishment;
    L2PcInstance _actor;

    public static final int PUNISH_BROADCAST = 1;
    public static final int PUNISH_KICK = 2;
    public static final int PUNISH_KICKBAN = 3;
    public static final int PUNISH_JAIL = 4;

    public IllegalPlayerAction(L2PcInstance actor, String message, int punishment)
    {
        _message        = message;
        _punishment     = punishment;
        _actor          = actor;

        switch(punishment)
        {
            case PUNISH_KICK:
                _actor.sendMessage("You will be kicked for illegal action, GM informed.");
                break;
            case PUNISH_KICKBAN:
                _actor.sendMessage("You are banned for illegal action, GM informed.");
                break;
            case PUNISH_JAIL:
            	_actor.sendMessage("Illegal action performed!");
            	_actor.sendMessage("You will be teleported to GM Consultation Service area and jailed.");
            	break;
        }
    }

    public void run()
    {
		LogRecord record = new LogRecord(Level.INFO, "AUDIT:" + _message);
		record.setLoggerName("audit");
		record.setParameters(new Object[]{_actor, _punishment});
		_logAudit.log(record);

        GmListTable.broadcastMessageToGMs(_message);

        switch(_punishment)
        {
            case PUNISH_BROADCAST:
                return;
            case PUNISH_KICK:
                _actor.closeNetConnection();
                break;
            case PUNISH_KICKBAN:
                _actor.setAccessLevel(-100);
                _actor.setAccountAccesslevel(-100);
                _actor.closeNetConnection();
                break;
            case PUNISH_JAIL:
                _actor.setInJail(true, Config.DEFAULT_PUNISH_PARAM);
                break;
        }
    }
}
