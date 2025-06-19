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

import java.nio.BufferUnderflowException;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.LoginServerThread;
import ru.agecold.gameserver.LoginServerThread.SessionKey;
import ru.agecold.gameserver.network.L2GameClient;
import ru.agecold.gameserver.network.serverpackets.ServerClose;
import ru.agecold.gameserver.network.smartguard.SmartGuard;
import ru.agecold.gameserver.network.smartguard.integration.SmartClient;
import ru.agecold.gameserver.util.WebStat;
import smartguard.api.ISmartGuardService;
import smartguard.api.integration.SessionData;
import smartguard.core.properties.GuardProperties;
import smartguard.spi.SmartGuardSPI;

/**
 * This class ...
 *
 * @version $Revision: 1.9.2.3.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class AuthLogin extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(AuthLogin.class.getName());
    // loginName + keys must match what the loginserver used.
    private String _loginName;
    /*
     * private final long _key1; private final long _key2; private final long
     * _key3; private final long _key4;
     */
    private int _playKey1;
    private int _playKey2;
    private int _loginKey1;
    private int _loginKey2;
    private byte[] data = null;

    @Override
    protected void readImpl() {
        _loginName = readS().toLowerCase();
        _playKey2 = readD();
        _playKey1 = readD();
        _loginKey1 = readD();
        _loginKey2 = readD();
        readD();
        if(SmartGuard.isActive() && _buf.remaining() > 2)
        {
            final int dataLen = readH();
            if(_buf.remaining() >= dataLen)
            {
                readB(data = new byte[dataLen]);
            }
        }
    }

    @Override
    protected void runImpl() {
        if(SmartGuard.isActive())
        {
            if(data == null)
            {
                getClient().close(new ServerClose());
                return;
            }
            ASmartClient smrtclient = new ASmartClient(getClient(), _loginName);
            smrtclient.setSessionData(new SessionData(_playKey2, _playKey1, _loginKey1, _loginKey2));
            ISmartGuardService svc = SmartGuardSPI.getSmartGuardService();
            if(!svc.getSmartGuardBus().checkAuthLogin(smrtclient, data))
            {
                smrtclient.closeLater();
                return;
            }
        }
        SessionKey key = new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2);
        L2GameClient client = getClient();

        // avoid potential exploits
        if (client.getAccountName() == null) {
            client.setAccountName(_loginName);
            LoginServerThread.getInstance().addGameServerLogin(_loginName, client);
            LoginServerThread.getInstance().addWaitingClientAndSendRequest(_loginName, client, key);
        }

        //����1
        LoginServerThread.checkClient(_loginName, client);
        if (Config.WEBSTAT_ENABLE) {
            WebStat.getInstance().addGame(client.getIpAddr());
        }
    }

    private class ASmartClient extends SmartClient
    {
        private final String _accountName;

        public ASmartClient(L2GameClient client, String accountName)
        {
            super(client);
            _accountName = accountName;
        }

        @Override
        public String getAccountName()
        {
            return _accountName;
        }
    }
}
