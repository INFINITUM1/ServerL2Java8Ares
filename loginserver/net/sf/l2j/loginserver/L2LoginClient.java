/* This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.loginserver;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;

import net.sf.l2j.loginserver.crypt.LoginCrypt;
import net.sf.l2j.loginserver.crypt.ScrambledKeyPair;
import net.sf.l2j.loginserver.serverpackets.AccountKicked;
import net.sf.l2j.loginserver.serverpackets.AccountKicked.AccountKickedReason;
import net.sf.l2j.loginserver.serverpackets.L2LoginServerPacket;
import net.sf.l2j.loginserver.serverpackets.LoginFail;
import net.sf.l2j.loginserver.serverpackets.PlayFail;
import net.sf.l2j.loginserver.serverpackets.LoginFail.LoginFailReason;
import net.sf.l2j.loginserver.serverpackets.PlayFail.PlayFailReason;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.TimeLogger;
import org.mmocore.network.ISocket;
import org.mmocore.network.MMOClient;
import org.mmocore.network.MMOConnection;

/**
 * Represents a client connected into the LoginServer
 *
 * @author  KenM
 */
public final class L2LoginClient extends MMOClient<MMOConnection<L2LoginClient>>
{
	public static enum LoginClientState
	{ 
		CONNECTED,
		AUTHED_GG,
		AUTHED_LOGIN
	}
	
	private LoginClientState _state;
	
	// Crypt
	private LoginCrypt _loginCrypt;
	private ScrambledKeyPair _scrambledPair;
	private byte[] _blowfishKey;
	
	private String _account;
	private int _accessLevel;
	private int _lastServer;
	private boolean _usesInternalIP;
	private SessionKey _sessionKey;
	private int _sessionId;
	private boolean _joinedGS;
	
	private long _connectionStartTime;

	private String _ip;
	
	/**
	 * @param con
	 */
	public L2LoginClient(MMOConnection<L2LoginClient> con)
	{
		super(con);
		_state = LoginClientState.CONNECTED;
		_ip = getConnection().getSocket().getInetAddress().getHostAddress();

		if (_ip.startsWith("192.168") || _ip.startsWith("10.0") || _ip.equals("127.0.0.1"))
		{
			_usesInternalIP = true;
		}
		
		_scrambledPair = LoginController.getInstance().getScrambledRSAKeyPair();
		_blowfishKey = LoginController.getInstance().getBlowfishKey();
		_sessionId = Rnd.nextInt();
		_connectionStartTime = System.currentTimeMillis();
		_loginCrypt = new LoginCrypt();
		_loginCrypt.setKey(_blowfishKey);
	}
	
	public boolean usesInternalIP()
	{
		return _usesInternalIP;
	}
	
	/**
	 * @see org.mmocore.interfaces.MMOClient#decrypt(java.nio.ByteBuffer, int)
	 */
	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		boolean ret = false;
		try
		{
			ret = _loginCrypt.decrypt(buf.array(), buf.position(), size);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			closeNow();
			return false;
		}
		
		if (!ret)
		{
			byte[] dump = new byte[size];
			System.arraycopy(buf.array(), buf.position(), dump, 0, size);
			System.out.println(TimeLogger.getLogTime() + "Wrong checksum from client: " + toString());
			closeNow();
		}
		
		return ret;
	}

	/**
	 * @see org.mmocore.interfaces.MMOClient#encrypt(java.nio.ByteBuffer, int)
	 */
	@Override
	public boolean encrypt(ByteBuffer buf, int size)
	{
		final int offset = buf.position();
		try
		{
			size = _loginCrypt.encrypt(buf.array(), offset, size);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		
		buf.position(offset + size);
		return true;
	}

	public LoginClientState getState()
	{
		return _state;
	}
	
	public void setState(LoginClientState state)
	{
		_state = state;
	}
	
	public byte[] getBlowfishKey()
	{
		return _blowfishKey;
	}
	
	public byte[] getScrambledModulus()
	{
		return _scrambledPair._scrambledModulus;
	}
	
	public RSAPrivateKey getRSAPrivateKey()
	{
		return (RSAPrivateKey) _scrambledPair._pair.getPrivate();
	}
	
	public String getAccount()
	{
		return _account;
	}
	
	public void setAccount(String account)
	{
		_account = account;
	}
	
	public void setAccessLevel(int accessLevel)
	{
		_accessLevel = accessLevel;
	}
	
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	public void setLastServer(int lastServer)
	{
		_lastServer = lastServer;
	}
	
	public int getLastServer()
	{
		return _lastServer;
	}
	
	public int getSessionId()
	{
		return _sessionId;
	}
	
	public boolean hasJoinedGS()
	{
		return _joinedGS;
	}
	
	public void setJoinedGS(boolean val)
	{
		_joinedGS = val;
	}
	
	public void setSessionKey(SessionKey sessionKey)
	{
		_sessionKey = sessionKey;
	}
	
	public SessionKey getSessionKey()
	{
		return _sessionKey;
	}
	
	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}

	public void sendPacket(L2LoginServerPacket lsp)
	{
		getConnection().sendPacket(lsp);
	}

	public void close(AccountKickedReason reason)
	{
		getConnection().close(new AccountKicked(reason));
	}
	
	public void close(LoginFailReason reason)
	{
		getConnection().close(new LoginFail(reason));
	}
	
	public void close(PlayFailReason reason)
	{
		getConnection().close(new PlayFail(reason));
	}
	
	public void close(L2LoginServerPacket lsp)
	{
		getConnection().close(lsp);
	}
	
	@Override
	public void onDisconnection()
	{
		if (getState() != LoginClientState.AUTHED_LOGIN)
		{
			LoginController.getInstance().removeLoginClient(this);
		}
		else if (!hasJoinedGS())
		{
			LoginController.getInstance().removeAuthedLoginClient(getAccount());
		}
	}

	@Override
	public String toString()
	{
		InetAddress address = getConnection().getSocket().getInetAddress();
		if (getState() == LoginClientState.AUTHED_LOGIN)
		{
			return "["+getAccount()+" ("+(address == null ? "disconnected" : address.getHostAddress())+")]";
		}
		else
		{
			return "["+(address == null ? "disconnected" : address.getHostAddress())+"]";
		}
	}

	String _hwid = "none";

	public void setHWID(String hwid)
	{
		_hwid = hwid;
	}

	public String getHWID()
	{
		return _hwid;
	}

	String _email = "none";

	public void setEmail(String email)
	{
		_email = email;
	}

	public boolean hasEmail()
	{
		return !_email.equalsIgnoreCase("null@null");
	}

	public String getIP()
	{
		return _ip;
	}

	private String _phoneNum = "";

	public void setPhoneNum(String num)
	{
		_phoneNum = num;
	}

	public String getPhoneNum()
	{
		return _phoneNum;
	}
}
