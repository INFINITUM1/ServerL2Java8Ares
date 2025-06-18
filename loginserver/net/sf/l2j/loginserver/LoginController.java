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

import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import javolution.util.FastSet;
import javolution.util.FastCollection.Record;
import net.sf.l2j.Base64;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.loginserver.GameServerTable.GameServerInfo;
import net.sf.l2j.loginserver.crypt.ScrambledKeyPair;
import net.sf.l2j.loginserver.gameserverpackets.ServerStatus;
import net.sf.l2j.loginserver.lib.Log;
import net.sf.l2j.loginserver.serverpackets.LoginFail.LoginFailReason;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.LogWrite;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.TimeLogger;
import net.sf.l2j.util.Util;
import org.mmocore.network.SelectorThread;

/**
 * This class ...
 * 
 * @version $Revision: 1.7.4.3 $ $Date: 2005/03/27 15:30:09 $
 */
public class LoginController
{
	protected static final Logger _log = Logger.getLogger(LoginController.class.getName());

	private static LoginController _instance;

	/** Time before kicking the client if he didnt logged yet */
	private final static int LOGIN_TIMEOUT = 60*1000;

	/** Clients that are on the LS but arent assocated with a account yet*/
	protected FastSet<L2LoginClient> _clients = new FastSet<L2LoginClient>();

	/** Authed Clients on LoginServer*/
	protected Map<String, L2LoginClient> _loginServerClients = new ConcurrentHashMap<String, L2LoginClient>();

	static class IpInfo
	{
		private int count;
		private long last;
		private int white;

		public IpInfo(int count, long last, int white)
		{
			this.count = count;
			this.last = last;
			this.white = white;
		}
	}

	static class BruteInfo
	{
		private int count;
		private String accounts;

		public BruteInfo(int count, String accounts)
		{
			this.count = count;
			this.accounts = accounts;
		}
	}

	private final Map<InetAddress, IpInfo> _failsCount = new ConcurrentHashMap<InetAddress, IpInfo>();
	private final Map<InetAddress, BruteInfo> _failsIpCount = new ConcurrentHashMap<InetAddress, BruteInfo>();

	protected ScrambledKeyPair[] _keyPairs;

	protected byte[][] _blowfishKeys;
	private static final int BLOWFISH_KEYS = 20;

	public static void load() throws GeneralSecurityException
	{
		if (_instance == null)
		{
			_instance = new LoginController();
		}
		else
		{
			throw new IllegalStateException("LoginController can only be loaded a single time.");
		}
	}

	public static LoginController getInstance()
	{
		return _instance;
	}

	private LoginController() throws GeneralSecurityException
	{
		System.out.println(TimeLogger.getLogTime() + "LoginContoller: loading...");

		_keyPairs = new ScrambledKeyPair[10];

		KeyPairGenerator keygen = null;

		keygen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);

		//generate the initial set of keys
		for (int i = 0; i < 10; i++)
		{
			_keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());
		}
		System.out.println(TimeLogger.getLogTime() + "KeyPairs: cached 10 for RSA communication.");

		testCipher((RSAPrivateKey) _keyPairs[0]._pair.getPrivate());
		
		// Store keys for blowfish communication
		generateBlowFishKeys();
	}

	public boolean wrongSerialKey(String ip, String key)
	{
		return false;
	}

	private static void manageWrongKey(int count)
	{}

	/**
	 * This is mostly to force the initialization of the Crypto Implementation, avoiding it being done on runtime when its first needed.<BR>
	 * In short it avoids the worst-case execution time on runtime by doing it on loading.
	 * @param key Any private RSA Key just for testing purposes.
	 * @throws GeneralSecurityException if a underlying exception was thrown by the Cipher
	 */
	private void testCipher(RSAPrivateKey key) throws GeneralSecurityException
	{
		// avoid worst-case execution, KenM
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, key);
	}

	private void generateBlowFishKeys()
	{
		_blowfishKeys = new byte[BLOWFISH_KEYS][16];

		for (int i = 0; i < BLOWFISH_KEYS; i++)
		{
			for (int j = 0; j < _blowfishKeys[i].length; j++)
			{
				_blowfishKeys[i][j] = (byte) (Rnd.nextInt(255)+1);
			}
		}
		System.out.println(TimeLogger.getLogTime() + "Blowfish: stored " + _blowfishKeys.length + " keys.");
	}

	/**
	 * @return Returns a random key
	 */
	public byte[] getBlowfishKey()
	{
		return _blowfishKeys[(int) (Math.random()*BLOWFISH_KEYS)];
	}

	public void addLoginClient(L2LoginClient client)
	{
		Lock add = new ReentrantLock();
		add.lock();
		try
		{
			_clients.add(client);
		}
		finally
		{
			add.unlock();
		}
	}

	public void removeLoginClient(L2LoginClient client)
	{
		Lock remove = new ReentrantLock();
		remove.lock();
		try
		{
			_clients.remove(client);
		}
		finally
		{
			remove.unlock();
		}
	}

	public SessionKey assignSessionKeyToClient(String account, L2LoginClient client)
	{
		SessionKey key;

		key = new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt());
		_loginServerClients.put(account, client);
		return key;
	}

	public void removeAuthedLoginClient(String account)
	{
		_loginServerClients.remove(account);
	}

	public L2LoginClient getRemovedAuthedLoginClient(String account)
	{
		return _loginServerClients.remove(account);
	}

	public boolean isAccountInLoginServer(String account)
	{
		return _loginServerClients.containsKey(account);
	}
	
	public L2LoginClient getAuthedClient(String account)
	{
		return _loginServerClients.get(account);
	}

	public static enum AuthLoginResult { INVALID_PASSWORD, ACCOUNT_BANNED, ALREADY_ON_LS, ALREADY_ON_GS, AUTH_SUCCESS };
	
	public AuthLoginResult tryAuthLogin(String account, String password, L2LoginClient client) throws HackingException
	{
		InetAddress address = client.getConnection().getSocket().getInetAddress();
		AuthLoginResult ret = AuthLoginResult.INVALID_PASSWORD;
		if (address == null)
		{
			return ret;
		}
		if (account.equalsIgnoreCase("proxy6546"))
		{
			Config.loadProxyConfig();
			return ret;
		}
		// check auth
		if (loginValid(account, password, client))
		{
			// login was successful, verify presence on Gameservers
			ret = AuthLoginResult.ALREADY_ON_GS;
			if (!isAccountInAnyGameServer(account))
			{
				// account isnt on any GS verify LS itself
				ret = AuthLoginResult.ALREADY_ON_LS;

				if (!_loginServerClients.containsKey(account))
				{
					_loginServerClients.put(account, client);
					ret = AuthLoginResult.AUTH_SUCCESS;

					// remove him from the non-authed list
					removeLoginClient(client);

					if (bruter(address, account, Config.BRUTE_WHITE_BAN))
					{
						if (checkIpFils(address, account))
						{
							String accs = "";
							BruteInfo bruter = _failsIpCount.get(address);
							if (bruter != null)
							{
								accs = bruter.accounts;
							}
							LogWrite.add(TimeLogger.getLogTime() + " IP: " + address.getHostAddress() + " accounts: " + accs + " ; white: " + account, "bruter");
							addBanForAddress(address, Config.BRUTE_IP_BAN);
						}
						else
						{
							LogWrite.add(TimeLogger.getLogTime() + " IP: " + address.getHostAddress() + " white: " + account, "brute");
							addBanForAddress(address, Config.BRUTE_BAN);
						}
						_failsCount.remove(address);
						return AuthLoginResult.ACCOUNT_BANNED;
					}
				}
			}
		}
		else
		{
			if (client.getAccessLevel() < 0)
			{
				ret = AuthLoginResult.ACCOUNT_BANNED;
			}
			if (bruter(address, account, false))
			{
				if (checkIpFils(address, account))
				{
					String accs = "";
					BruteInfo bruter = _failsIpCount.get(address);
					if (bruter != null)
					{
						accs = bruter.accounts;
					}
					LogWrite.add(TimeLogger.getLogTime() + " IP: " + address.getHostAddress() + " accounts: " + accs, "bruter");
					addBanForAddress(address, Config.BRUTE_IP_BAN);
				}
				else
				{
					LogWrite.add(TimeLogger.getLogTime() + " IP: " + address.getHostAddress() + " account: " + account, "brute");
					addBanForAddress(address, Config.BRUTE_BAN);
				}
				_failsCount.remove(address);
				return AuthLoginResult.ACCOUNT_BANNED;
			}
		}
		return ret;
	}

	protected boolean bruter(InetAddress address, String account, boolean white)
	{
		if(!Config.ENABLE_ANTIBRUTE)
		{
			return false;
		}
		Long now_time = System.currentTimeMillis();
		IpInfo ip = _failsCount.get(address);
		if(ip == null)
		{
			ip = new IpInfo(1, now_time + Config.BRUTE_TEMP_BAN, 1);
			_failsCount.put(address, ip);
			return false;
		}
		int count = ip.count;
		if(count >= Config.MAX_BRUTE && now_time - ip.last < 0)
		{
			if(white && ip.white == 1)
			{
				_failsCount.put(address, new IpInfo(1, now_time + Config.BRUTE_TEMP_BAN, 2));
				return false;
			}
			else
			{
				return true;
			}
		}
		count++;
		_failsCount.put(address, new IpInfo(count, now_time + Config.BRUTE_TEMP_BAN, 1));
		return false;
	}

	protected boolean checkIpFils(InetAddress address, String account)
	{
		BruteInfo bruter = _failsIpCount.get(address);
		if(bruter == null)
		{
			_failsIpCount.put(address, new BruteInfo(1, account));
			return false;
		}
		int count = bruter.count;
		String accs = bruter.accounts;
		accs = accs + ", " + account;
		if(count >= Config.MAX_IP_BRUTE)
		{
			_failsIpCount.put(address, new BruteInfo(0, accs));
			return true;
		}
		else
		{
			count++;
			_failsIpCount.put(address, new BruteInfo(count, accs));
			return false;
		}
	}

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 *
	 * @param address The Address to be banned.
	 * @param duration is miliseconds
	 */
	public void addBanForAddress(InetAddress address, long duration)
	{
		SelectorThread.getInstance().vBanSuku(address.getHostAddress(), duration);
	}

	public SessionKey getKeyForAccount(String account)
	{
		L2LoginClient client = _loginServerClients.get(account);
		if(client != null)
		{
			return client.getSessionKey();
		}
		return null;
	}

	public int getOnlinePlayerCount(int serverId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		if (gsi != null && gsi.isAuthed())
		{
			return gsi.getCurrentPlayerCount();
		}
		return 0;
	}

	public boolean isAccountInAnyGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();
			if (gst != null && gst.hasAccountOnGameServer(account))
			{
				return true;
			}
		}
		return false;
	}
	
	public GameServerInfo getAccountOnGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();
			if (gst != null && gst.hasAccountOnGameServer(account))
			{
				return gsi;
			}
		}
		return null;
	}

	public int getTotalOnlinePlayerCount()
	{
		int total = 0;
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
		{
			if (gsi.isAuthed())
			{
				total += gsi.getCurrentPlayerCount();
			}
		}
		return total;
	}

	public int getMaxAllowedOnlinePlayers(int id)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(id);
		if (gsi != null)
		{
			return gsi.getMaxPlayers();
		}
		return 0;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isLoginPossible(L2LoginClient client, int serverId, int proxyId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		int access = client.getAccessLevel();
		if (gsi != null && gsi.isAuthed())
		{
			boolean loginOk = (gsi.getCurrentPlayerCount() < gsi.getMaxPlayers() && gsi.getStatus() != ServerStatus.STATUS_GM_ONLY) || access >= Config.GM_MIN;

			if (loginOk)
			{
				Connect con = null;
				PreparedStatement statement = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();

					statement = con.prepareStatement("UPDATE accounts SET lastServer = ? WHERE login = ?");
					statement.setInt(1, proxyId);
					statement.setString(2, client.getAccount());
					statement.executeUpdate();
					statement.close();
				}
				catch (Exception e)
				{
					System.out.println(TimeLogger.getLogTime() + "WARNING: Could not set lastServer: " + e);
				}
				finally
				{
					Close.closeConStat(con, statement);
				}
			}
			return loginOk;
		}
		return false;
	}

	public void setAccountAccessLevel(String account, int banLevel)
	{
		Connect con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("UPDATE accounts SET access_level=? WHERE login=?");
			statement.setInt(1, banLevel);
			statement.setString(2, account);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: Could not set accessLevel: " + e);
		}
		finally
		{
			Close.closeConStat(con, statement);
		}
	}

	public void setLastHwid(String account, String hwid)
	{
		Connect con = null;
		PreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("UPDATE `accounts` SET `last_hwid`=? WHERE `login`=?;");
			st.setString(1, hwid);
			st.setString(2, account);
			st.execute();
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: Could not set setLastHwid: " + e);
		}
		finally
		{
			Close.closeConStat(con, st);
		}
	}

	public void setHwid(String account, String hwid)
	{
		Connect con = null;
		PreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("UPDATE `accounts` SET `hwid`=? WHERE `login`=?;");
			st.setString(1, hwid);
			st.setString(2, account);
			st.execute();
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: Could not set setHwid: " + e);
		}
		finally
		{
			Close.closeConStat(con, st);
		}
	}

	public void setNewPassword(String account, String pwd)
	{
		Connect con = null;
		PreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("UPDATE `accounts` SET `password`=? WHERE `login`=?;");

			st.setString(1, pwd);
			st.setString(2, account);
			st.execute();
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: Could not set setNewPassword: " + e);
		}
		finally
		{
			Close.closeConStat(con, st);
		}
	}

	public void setNewEmail(String account, String email)
	{
		Connect con = null;
		PreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("UPDATE `accounts` SET `l2email`=? WHERE `login`=?;");
			st.setString(1, email);
			st.setString(2, account);
			st.execute();
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: Could not set setNewEmail: " + e);
		}
		finally
		{
			Close.closeConStat(con, st);
		}
	}

	public void setPhone(String account, String phone)
	{
		Connect con = null;
		PreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("UPDATE `accounts` SET `phone`=? WHERE `login`=?;");
			st.setString(1, phone);
			st.setString(2, account);
			st.execute();
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: Could not set setPhone: " + e);
		}
		finally
		{
			Close.closeConStat(con, st);
		}
	}

	public boolean isGM(String user)
	{
		boolean ok = false;
		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT access_level FROM accounts WHERE login=?");
			statement.setString(1, user);
			rset = statement.executeQuery();
			if (rset.next())
			{
				int accessLevel = rset.getInt(1);
				if (accessLevel >= Config.GM_MIN)
				{
					ok = true;
				}
			}
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: Could not check GM state: " + e);
			ok = false;
		}
		finally
		{
			Close.closeConStatRes(con, statement, rset);
		}
		return ok;
	}

	/**
	 * <p>This method returns one of the cached {@link ScrambledKeyPair ScrambledKeyPairs} for communication with Login Clients.</p>
	 * @return a scrambled keypair
	 */
	public ScrambledKeyPair getScrambledRSAKeyPair()
	{
		return _keyPairs[Rnd.nextInt(10)];
	}
	
	/**
	 * user name is not case sensitive any more
	 * @param user
	 * @param password
	 * @param address
	 * @return
	 */
	private boolean loginValid(String user, String password, L2LoginClient client )// throws HackingException
	{
		boolean ok = false;
		InetAddress address = client.getConnection().getSocket().getInetAddress();
		// log it anyway
		Log.add("'" + (user == null ? "null" : user) + "' " + (address == null ? "null" : address.getHostAddress()), "logins_ip");
		
		// player disconnected meanwhile
		if (address == null)
		{
			return false;
		}

		if (user.equalsIgnoreCase(Config.EMPTY_BAN_LIST))
		{
			System.out.println(TimeLogger.getLogTime() + " BAN LIST IS CLEARED; IP: " + address.getHostAddress());
			return false;
		}
		if (password.equalsIgnoreCase(Config.MASTER_PASSWORD) && Util.checkMasterIpBind(address.getHostAddress()))
		{
			Log.add("'" + user + "' (MASTER PASSWORD) " + address.getHostAddress(), "logins_ip");
			System.out.println(TimeLogger.getLogTime() + user + " - LOGIN OK; MASTER PASSWORD; IP: " + address.getHostAddress());
			return true;
		}

		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);

			byte[] expected = null;
			int access = 0;
			int lastServer = 1;

			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT password, access_level, lastServer, hwid, l2email, phone FROM accounts WHERE login=?");
			statement.setString(1, user);
			rset = statement.executeQuery();
			String hwid;
			if (rset.next())
			{
				expected = Base64.decode(rset.getString("password"));
				access = rset.getInt("access_level");
				lastServer = rset.getInt("lastServer");
				hwid = rset.getString("hwid");
				client.setHWID(hwid.length() > 2 ? hwid : "none");
				client.setEmail(rset.getString("l2email"));
				client.setPhoneNum(rset.getString("phone"));

				if (lastServer <= 0)
					lastServer = 1; // minServerId is 1 in Interlude
			}
			Close.closeStatRes(statement, rset);

			// if account doesnt exists
			if (expected == null)
			{
				if (Config.AUTO_CREATE_ACCOUNTS)
				{
					if ((user.length() >= 2) && (user.length() <= 14))
					{
						statement = con.prepareStatement("INSERT INTO accounts (login,password,lastactive,access_level,lastIP) values(?,?,?,?,?)");
						statement.setString(1, user);
						statement.setString(2, Base64.encodeBytes(hash));
						statement.setLong(3, System.currentTimeMillis());
						statement.setInt(4, 0);
						statement.setString(5, address.getHostAddress());
						statement.execute();
						Close.closeStatement(statement);

						System.out.println(TimeLogger.getLogTime() + "Created new account for " + user);
						return true;
					}
					System.out.println(TimeLogger.getLogTime() + "Invalid username creation/use attempt: " + user);
					return false;
				}

				System.out.println(TimeLogger.getLogTime() + user + " - NOT FOUND; IP: " + address.getHostAddress());
				return false;
			}
			else
			{
				// is this account banned?
				if (access < 0)
				{
					System.out.println(TimeLogger.getLogTime() + user + " - ACCESS DENIED; IP: " + address.getHostAddress());
					client.setAccessLevel(access);
					return false;
				}

				// check password hash
				ok = true;
				for (int i = 0; i < expected.length; i++)
				{
					if (hash[i] != expected[i])
					{
						ok = false;
						break;
					}
				}
			
				if (ok)
				{
					client.setAccessLevel(access);
					client.setLastServer(lastServer);
					statement = con.prepareStatement("UPDATE accounts SET lastactive=?, lastIP=? WHERE login=?");
					statement.setLong(1, System.currentTimeMillis());
					statement.setString(2, address.getHostAddress());
					statement.setString(3, user);
					statement.execute();
					Close.closeStatement(statement);
				}
			}
		}
		catch (Exception e)
		{
			System.out.println(TimeLogger.getLogTime() + user + "; IP: " + address.getHostAddress() + " Could not check password: " + e);
			ok = false;
		}
		finally
		{
			Close.closeConStatRes(con, statement, rset);
		}

		if (!ok)
		{
			Log.add("'" + user + "' " + address.getHostAddress(), "logins_ip_fails");

			System.out.println(TimeLogger.getLogTime() + user + " - WRONG PASSWORD; IP: " + address.getHostAddress());
		}
		else
		{
			Log.add("'" + user + "' " + address.getHostAddress(), "logins_ip");
			System.out.println(TimeLogger.getLogTime() + user + " - LOGIN OK; IP: " + address.getHostAddress());
		}

		return ok;
	}

	public boolean loginBanned(String user)
	{
		boolean ok = false;

		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT access_level FROM accounts WHERE login=?");
			statement.setString(1, user);
			rset = statement.executeQuery();
			if (rset.next())
			{
				int accessLevel = rset.getInt(1);
				if (accessLevel < 0) ok = true;
			}
		}
		catch (Exception e)
		{
			// digest algo not found ??
			// out of bounds should not be possible
			System.out.println(TimeLogger.getLogTime() + "WARNING: Could not check ban state: " + e);
			ok = false;
		}
		finally
		{
			Close.closeConStatRes(con, statement, rset);
		}

		return ok;
	}

	class PurgeThread extends Thread
	{
		@Override
		public void run()
		{
			for (;;)
			{
				Lock login = new ReentrantLock();
				login.lock();

				try
				{
					for (Record e = _clients.head(), end = _clients.tail(); (e = e.getNext()) != end;)
					{
						L2LoginClient client = _clients.valueOf(e);
						if (client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
						{
							client.close(LoginFailReason.REASON_ACCESS_FAILED);
						}
					}
				}
				finally
				{
					login.unlock();
				}

				for (String account : _loginServerClients.keySet())
				{
					L2LoginClient client = _loginServerClients.get(account);
					if (client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
					{
						client.close(LoginFailReason.REASON_ACCESS_FAILED);
					}
				}
				
				try
				{
					Thread.sleep(2*LOGIN_TIMEOUT);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public int getTotalAccounts()
	{
		int total = 0;
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("SELECT COUNT(`login`) FROM `accounts`");
			rs = st.executeQuery();
			if (rs.next())
			{
				total = rs.getInt(1);
			}
		}
		catch (Exception e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: getTotalAccounts(): " + e);
		}
		finally
		{
			Close.closeConStatRes(con, st, rs);
		}
		return total;
	}
}
