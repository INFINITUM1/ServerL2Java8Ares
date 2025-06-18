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
package net.sf.l2j.loginserver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javolution.util.FastSet;
import net.sf.l2j.Config;
import net.sf.l2j.loginserver.GameServerTable.GameServerInfo;
import net.sf.l2j.loginserver.crypt.NewCrypt;
import net.sf.l2j.loginserver.gameserverpackets.*;
import net.sf.l2j.loginserver.loginserverpackets.*;
import net.sf.l2j.loginserver.serverpackets.ServerBasePacket;
import net.sf.l2j.util.TimeLogger;
import net.sf.l2j.util.Util;

/**
 * @author -Wooden-
 * @author KenM
 *
 */

public class GameServerThread extends Thread
{
	protected static final Logger _log = Logger.getLogger(GameServerThread.class.getName());
	private Socket _connection;
	private InputStream _in;
	private OutputStream _out;
	private RSAPublicKey _publicKey;
	private RSAPrivateKey _privateKey;
	private NewCrypt _blowfish;
	private byte[] _blowfishKey;

	private String _connectionIp;

	private GameServerInfo _gsi;
	
	/** Authed Clients on a GameServer*/
	private Set<String> _accountsOnGameServer = new FastSet<String>();
	
	private String _connectionIPAddress;
	private String _connectionIPXZddress;

	@Override
	public void run()
	{
		_connectionIPAddress   = _connection.getInetAddress().getHostAddress();
		if (isBannedGameserverIP(_connectionIPAddress) || Config.FAIL_KEY_COUNT >= 5)
		{
			System.out.println(TimeLogger.getLogTime() + "GameServerRegistration: IP Address " + _connectionIPAddress + " is on Banned IP list.");
			forceClose(LoginServerFail.REASON_IP_BANNED);
			// ensure no further processing for this connection
			return;
		}

		InitLS startPacket = new InitLS(_publicKey.getModulus().toByteArray());
		try
		{
			sendPacket(startPacket);

			int lengthHi = 0;
			int lengthLo = 0;
			int length = 0;
			boolean checksumOk = false;
			for (;;)
			{
				lengthLo = _in.read();
				lengthHi = _in.read();
				length= lengthHi*256 + lengthLo;  

				if (lengthHi < 0 || _connection.isClosed())
				{
					_log.finer(TimeLogger.getLogTime() + "LoginServerThread: Login terminated the connection.");
					break;
				}

				byte[] data = new byte[length - 2];

				int receivedBytes = 0;
				int newBytes = 0;
				while (newBytes != -1 && receivedBytes<length-2)
				{
					newBytes =  _in.read(data, 0, length-2);
					receivedBytes = receivedBytes + newBytes;
				}

				if (receivedBytes != length-2)
				{
					System.out.println(TimeLogger.getLogTime() + "Incomplete Packet is sent to the server, closing connection.(LS)");
					break;
				}

				// decrypt if we have a key
				data = _blowfish.decrypt(data);
				checksumOk = NewCrypt.verifyChecksum(data);
				if (!checksumOk)
				{
					System.out.println(TimeLogger.getLogTime() + "Incorrect packet checksum, closing connection (LS)");
				}
				else
				{

					if (Config.DEBUG)
					{
						System.out.println(TimeLogger.getLogTime() + "[C]\n" + Util.printData(data));
					}

					int packetType = data[0] & 0xff;
					switch (packetType)
					{
						case 0xAA:
							onReceiveLastHwid(data);
							break;
						case 0xAB:
							onReceiveSetHwid(data);
							break;
						case 0xAC:
							onReceiveNewPassword(data);
							break;
						case 0xAD:
							onReceivePlayerAuthRequest(data);
							break;
						case 0xAE:
							onReceiveBlowfishKey(data);
							break;
						case 0xAF:
							onGameServerAuth(data);
							break;
						case 0xBA:
							onReceivePlayerInGame(data);
							break;
						case 0xBB:
							onReceivePlayerLogOut(data);
							break;
						case 0xBC:
							onReceiveChangeAccessLevel(data);
							break;
						case 0xBD:
							onReceiveServerStatus(data);
							break;
						case 0xBE:
							onReceiveWebStatAccounts(data);
							break;
						case 0xBF:
							onReceiveNewEmail(data);
							break;
						case 0xCA:
							onReceivePhone(data);
							break;
						default:
							forceClose(LoginServerFail.NOT_AUTHED);
					}
				}
			}
		}
		catch (IOException e)
		{
			String serverName = (getServerId() != -1 ? "["+getServerId()+"] "+GameServerTable.getInstance().getServerNameById(getServerId()) : "("+_connectionIPAddress+")");
			String msg = TimeLogger.getLogTime() + "GameServer " + serverName + ": Connection lost: " + e.getMessage();
			System.out.println(msg);
			broadcastToTelnet(msg);
		}
		finally
		{
			if (isAuthed())
			{
				_gsi.setDown();
				System.out.println(TimeLogger.getLogTime() + "Server [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) + " is now set as disconnected");
			}
			L2LoginServer.getInstance().getGameServerListener().removeGameServer(this);
			L2LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);

			if (Config.RESTART_ON_RECONNECT)
			{
				System.exit(2);
			}
		}
	}
	
	private void onReceiveBlowfishKey(byte[] data)
	{
		/*if (_blowfish == null)
		{*/
		BlowFishKey bfk = new BlowFishKey(data,_privateKey);
		_blowfishKey = bfk.getKey();
		_blowfish = new NewCrypt(_blowfishKey);
		if (Config.DEBUG)
		{
			System.out.println(TimeLogger.getLogTime() + "New BlowFish key received, Blowfih Engine initialized:");
		}
		/*}
		else
		{
			_log.warning("GameServer attempted to re-initialize the blowfish key.");
			// TODO get a better reason
			this.forceClose(LoginServerFail.NOT_AUTHED);
		}*/
	}

	private void onGameServerAuth(byte[] data) throws IOException
	{
		GameServerAuth gsa = new GameServerAuth(data);
		if (wrongSerialKey(gsa.getExternalHost(), gsa.getSerialKey()))
		{
			forceClose(LoginServerFail.REASON_WRONG_SERIAL);
			System.exit(1);
			return;
		}
		handleRegProcess(gsa);
		if (isAuthed())
		{
			sendPacket(new AuthResponse(getGameServerInfo().getId()));
			if (gsa.getCatsGuard() == 1)
			{
				Config.CATS_GUARD = true;
			}
		}
	}

	private boolean wrongSerialKey(String ip, String key)
	{
		return LoginController.getInstance().wrongSerialKey(ip, key);
	}

	private void onReceivePlayerInGame(byte[] data)
	{
		if (isAuthed())
		{
			PlayerInGame pig = new PlayerInGame(data);
			List<String> newAccounts = pig.getAccounts();
			for (String account : newAccounts)
			{
				_accountsOnGameServer.add(account);
				if (Config.DEBUG)
				{
					System.out.println(TimeLogger.getLogTime() + "Account " + account + " logged in GameServer: [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()));
				}

				broadcastToTelnet(TimeLogger.getLogTime() + "Account " + account + " logged in GameServer " + getServerId());
			}
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceivePlayerLogOut(byte[] data)
	{
		if (isAuthed())
		{
			PlayerLogout plo = new PlayerLogout(data);
			_accountsOnGameServer.remove(plo.getAccount());
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceiveChangeAccessLevel(byte[] data)
	{
		if (isAuthed())
		{
			ChangeAccessLevel cal = new ChangeAccessLevel(data);
			LoginController.getInstance().setAccountAccessLevel(cal.getAccount(),cal.getLevel());
			System.out.println(TimeLogger.getLogTime() + "Changed " + cal.getAccount() + " access level to " + cal.getLevel());
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceiveLastHwid(byte[] data)
	{
		if (isAuthed())
		{
			SetLastHwid slh = new SetLastHwid(data);
			LoginController.getInstance().setLastHwid(slh.getAccount(), slh.getHwid());
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceiveSetHwid(byte[] data)
	{
		if (isAuthed())
		{
			SetHwid slh = new SetHwid(data);
			LoginController.getInstance().setHwid(slh.getAccount(), slh.getHwid());
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceiveNewPassword(byte[] data)
	{
		if (isAuthed())
		{
			SetNewPassword snp = new SetNewPassword(data);
			LoginController.getInstance().setNewPassword(snp.getAccount(), snp.getNewPassword());
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceiveNewEmail(byte[] data)
	{
		if (isAuthed())
		{
			SetNewEmail snp = new SetNewEmail(data);
			LoginController.getInstance().setNewEmail(snp.getAccount(), snp.getNewEmail());
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceivePhone(byte[] data)
	{
		if (isAuthed())
		{
			SetPhoneNumber spn = new SetPhoneNumber(data);
			LoginController.getInstance().setPhone(spn.getAccount(), spn.getPhone());
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceiveWebStatAccounts(byte[] data)
	{
		if (isAuthed())
		{
			WebStatAccounts wsa = new WebStatAccounts(LoginController.getInstance().getTotalAccounts());
			try
			{
				sendPacket(wsa);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceivePlayerAuthRequest(byte[] data) throws IOException
	{
		if (isAuthed())
		{
			PlayerAuthRequest par = new PlayerAuthRequest(data);
			PlayerAuthResponse authResponse;

			SessionKey key = LoginController.getInstance().getKeyForAccount(par.getAccount());

			if (key != null && key.equals(par.getKey()))
			{
				L2LoginClient client = LoginController.getInstance().getRemovedAuthedLoginClient(par.getAccount());
				authResponse = new PlayerAuthResponse(par.getAccount(), true, client.getHWID(), client.hasEmail(), client.getLastServer(), client.getPhoneNum());
			}
			else
			{
				authResponse = new PlayerAuthResponse(par.getAccount(), false, "none", true, 1, "");
			}
			sendPacket(authResponse);
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceiveServerStatus(byte[] data)
	{
		ServerStatus ss;
		if (isAuthed())
		{
			if (Config.DEBUG)
			{
				System.out.println(TimeLogger.getLogTime() + "ServerStatus received");
			}

			ss = new ServerStatus(data,getServerId()); //will do the actions by itself
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void handleRegProcess(GameServerAuth gameServerAuth)
	{
		GameServerTable gameServerTable = GameServerTable.getInstance();

		int id = gameServerAuth.getDesiredID();
		byte[] hexId = gameServerAuth.getHexID();

		GameServerInfo gsi = gameServerTable.getRegisteredGameServerById(id);
		// is there a gameserver registered with this id?
		if (gsi != null)
		{
			// does the hex id match?
			if (Arrays.equals(gsi.getHexId(), hexId))
			{
				// check to see if this GS is already connected
				synchronized (gsi)
				{
					if (gsi.isAuthed())
					{
						forceClose(LoginServerFail.REASON_ALREADY_LOGGED8IN);
					}
					else
					{
						attachGameServerInfo(gsi, gameServerAuth);
					}
				}
			}
			else
			{
				// there is already a server registered with the desired id and different hex id
				// try to register this one with an alternative id
				if (Config.ACCEPT_NEW_GAMESERVER && gameServerAuth.acceptAlternateID())
				{
					gsi = new GameServerInfo(id, hexId, this);
					if (gameServerTable.registerWithFirstAvaliableId(gsi))
					{
						attachGameServerInfo(gsi, gameServerAuth);
						gameServerTable.registerServerOnDB(gsi);
					}
					else
					{
						forceClose(LoginServerFail.REASON_NO_FREE_ID);
					}
				}
				else
				{
					// server id is already taken, and we cant get a new one for you
					forceClose(LoginServerFail.REASON_WRONG_HEXID);
				}
			}
		}
		else
		{
			// can we register on this id?
			if (Config.ACCEPT_NEW_GAMESERVER)
			{
				gsi = new GameServerInfo(id, hexId, this);
				if (gameServerTable.register(id, gsi))
				{
					attachGameServerInfo(gsi, gameServerAuth);
					gameServerTable.registerServerOnDB(gsi);
				}
				else
				{
					// some one took this ID meanwhile
					forceClose(LoginServerFail.REASON_ID_RESERVED);
				}
			}
			else
			{
				forceClose(LoginServerFail.REASON_WRONG_HEXID);
			}
		}
	}
	
	public boolean hasAccountOnGameServer(String account)
	{
		return _accountsOnGameServer.contains(account);
	}
	
	public int getPlayerCount()
	{
		return _accountsOnGameServer.size();
	}

	public void removeAccount(String account)
	{
		_accountsOnGameServer.remove(account);
	}

	/**
	 * Attachs a GameServerInfo to this Thread
	 * <li>Updates the GameServerInfo values based on GameServerAuth packet</li>
	 * <li><b>Sets the GameServerInfo as Authed</b></li>
	 * @param gsi The GameServerInfo to be attached.
	 * @param gameServerAuth The server info.
	 */
	private void attachGameServerInfo(GameServerInfo gsi, GameServerAuth gameServerAuth)
	{
		setGameServerInfo(gsi);
		gsi.setGameServerThread(this);
		gsi.setPorts(gameServerAuth.getPorts());
		setGameHosts(gameServerAuth.getExternalHost(), gameServerAuth.getInternalHost());
		gsi.setMaxPlayers(gameServerAuth.getMaxPlayers());
		gsi.setAuthed(true);
	}

	private void forceClose(int reason)
	{
		LoginServerFail lsf = new LoginServerFail(reason);
		try
		{
			sendPacket(lsf);
		}
		catch (IOException e)
		{
			_log.finer(TimeLogger.getLogTime() + "GameServerThread: Failed kicking banned server. Reason: " + e.getMessage());
		}

		try
		{
			_connection.close();
		}
		catch (IOException e)
		{
			_log.finer(TimeLogger.getLogTime() + "GameServerThread: Failed disconnecting banned server, server already disconnected.");
		}
	}

	/**
	 * 
	 * @param gameServerauth
	 */
	/*private void handleRegisterationProcess(GameServerAuth gameServerauth)
	{
		try
		{
			GameServerTable gsTableInstance = GameServerTable.getInstance();
			if (gsTableInstance.isARegisteredServer(gameServerauth.getHexID()))
			{
				if (Config.DEBUG)
				{
					_log.info("Valid HexID");
				}
				_server_id = gsTableInstance.getServerIDforHex(gameServerauth.getHexID());
				if (gsTableInstance.isServerAuthed(_server_id))
				{
					LoginServerFail lsf = new LoginServerFail(LoginServerFail.REASON_ALREADY_LOGGED8IN);
					sendPacket(lsf);
					_connection.close();
					return;
				}
				_gamePort = gameServerauth.getPort();
				setGameHosts(gameServerauth.getExternalHost(), gameServerauth.getInternalHost());
				_max_players = gameServerauth.getMaxPlayers();
				_hexID = gameServerauth.getHexID();
				//gsTableInstance.addServer(this);
			}
			else if (Config.ACCEPT_NEW_GAMESERVER)
			{
				if (Config.DEBUG)
				{
					_log.info("New HexID");
				}
				if(!gameServerauth.acceptAlternateID())
				{
					if(gsTableInstance.isIDfree(gameServerauth.getDesiredID()))
					{
						if (Config.DEBUG)_log.info("Desired ID is Valid");
						_server_id = gameServerauth.getDesiredID();
						_gamePort = gameServerauth.getPort();
						setGameHosts(gameServerauth.getExternalHost(), gameServerauth.getInternalHost());
						_max_players = gameServerauth.getMaxPlayers();
						_hexID = gameServerauth.getHexID();
						gsTableInstance.createServer(this);
						//gsTableInstance.addServer(this);
					}
					else
					{
						LoginServerFail lsf = new LoginServerFail(LoginServerFail.REASON_ID_RESERVED);
						sendPacket(lsf);
						_connection.close();
						return;
					}
				}
				else
				{
					int id;
					if(!gsTableInstance.isIDfree(gameServerauth.getDesiredID()))
					{
						id = gsTableInstance.findFreeID();
						if (Config.DEBUG)_log.info("Affected New ID:"+id);
						if(id < 0)
						{
							LoginServerFail lsf = new LoginServerFail(LoginServerFail.REASON_NO_FREE_ID);
							sendPacket(lsf);
							_connection.close();
							return;
						}
					}
					else
					{
						id = gameServerauth.getDesiredID();
						if (Config.DEBUG)_log.info("Desired ID is Valid");
					}
					_server_id = id;
					_gamePort = gameServerauth.getPort();
					setGameHosts(gameServerauth.getExternalHost(), gameServerauth.getInternalHost());
					_max_players = gameServerauth.getMaxPlayers();
					_hexID = gameServerauth.getHexID();
					gsTableInstance.createServer(this);
					//gsTableInstance.addServer(this);
				}
			}
			else
			{
				_log.info("Wrong HexID");
				LoginServerFail lsf = new LoginServerFail(LoginServerFail.REASON_WRONG_HEXID);
				sendPacket(lsf);
				_connection.close();
				return;
			}

		}
		catch (IOException e)
		{
			_log.info("Error while registering GameServer "+GameServerTable.getInstance().serverNames.get(_server_id)+" (ID:"+_server_id+")");
		}
	}*/

	/**
	 * @param ipAddress
	 * @return
	 */
	public static boolean isBannedGameserverIP(String ipAddress)
	{
		// TODO Auto-generated method stub
		return false;
	}

	public GameServerThread(Socket con)
	{
		_connection = con;
		_connectionIp = con.getInetAddress().getHostAddress();
		try
		{
			_in = _connection.getInputStream();
			_out = new BufferedOutputStream(_connection.getOutputStream());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		KeyPair pair = GameServerTable.getInstance().getKeyPair();
		_privateKey = (RSAPrivateKey) pair.getPrivate();
		_publicKey = (RSAPublicKey) pair.getPublic();
		_blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
		start();
	}

	/**
	 * @param sl
	 * @throws IOException
	 */
	private void sendPacket(ServerBasePacket sl) throws IOException
	{
		byte[] data = sl.getContent();
		NewCrypt.appendChecksum(data);
		if (Config.DEBUG)
		{
			_log.finest("[S] "+sl.getClass().getSimpleName()+":\n"+Util.printData(data));
		}
		data = _blowfish.crypt(data);

		int len = data.length+2;
		synchronized(_out)
		{
			_out.write(len & 0xff);
			_out.write(len >> 8 &0xff);
			_out.write(data);
			_out.flush();
		}
	}
	
	private void broadcastToTelnet(String msg)
	{
		//if (L2LoginServer.getInstance().getStatusServer() != null)
		//{
		//	L2LoginServer.getInstance().getStatusServer().sendMessageToTelnets(msg);
		//}
	}
	
	public void kickPlayer(String account)
	{
		try
		{
			sendPacket(new KickPlayer(account));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @param gameHost The gameHost to set.
	 */
	public void setGameHosts(String gameExternalHost, String gameInternalHost)
	{
		String oldInternal = _gsi.getInternalHost();
		String oldExternal = _gsi.getExternalHost();
		
		_gsi.setExternalHost(gameExternalHost);
		_gsi.setInternalIp(gameInternalHost);
		
		if (!gameExternalHost.equals("*"))
		{
			try
			{
				_gsi.setExternalIp(InetAddress.getByName(gameExternalHost).getHostAddress());
			}
			catch (UnknownHostException e)
			{
				System.out.println(TimeLogger.getLogTime() + "Couldn't resolve hostname \"" + gameExternalHost + "\"");
			}
		}
		else
		{
			_gsi.setExternalIp(_connectionIp);
		}
		if(!gameInternalHost.equals("*"))
		{
			try
			{
				_gsi.setInternalIp(InetAddress.getByName(gameInternalHost).getHostAddress());
			}
			catch (UnknownHostException e)
			{
				System.out.println(TimeLogger.getLogTime() + "Couldn't resolve hostname \"" + gameInternalHost + "\"");
			}
		}
		else
		{
			_gsi.setInternalIp(_connectionIp);
		}

		System.out.println(TimeLogger.getLogTime() + "Updated Gameserver [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) + " IP's:");
		if (oldInternal == null || !oldInternal.equalsIgnoreCase(gameInternalHost))
			System.out.println(TimeLogger.getLogTime() + "InternalIP: " + gameInternalHost);
		if (oldExternal == null || !oldExternal.equalsIgnoreCase(gameExternalHost))
			System.out.println(TimeLogger.getLogTime() + "ExternalIP: " + gameExternalHost);
	}

	/**
	 * @return Returns the isAuthed.
	 */
	public boolean isAuthed()
	{
		if (getGameServerInfo() == null)
			return false;
		return getGameServerInfo().isAuthed();
	}

	public void setGameServerInfo(GameServerInfo gsi)
	{
		_gsi = gsi;
	}

	public GameServerInfo getGameServerInfo()
	{
		return _gsi;
	}

	/**
	 * @return Returns the connectionIpAddress.
	 */
	public String getConnectionIpAddress()
	{
		return _connectionIPAddress;
	}

	private int getServerId()
	{
		if (getGameServerInfo() != null)
		{
			return getGameServerInfo().getId();
		}
		return -1;
	}
}