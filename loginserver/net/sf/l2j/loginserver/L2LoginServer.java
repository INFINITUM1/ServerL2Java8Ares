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

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Server;
import net.sf.l2j.status.Status;
import net.sf.l2j.util.TimeLogger;
import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;

/**
 *
 * @author  KenM
 */
public class L2LoginServer
{
	public static final int PROTOCOL_REV = 0x0102;
	
	private static L2LoginServer _instance;
	private Logger _log = Logger.getLogger(L2LoginServer.class.getName());
	private GameServerListener _gameServerListener;
	private SelectorThread<L2LoginClient> _selectorThread;
	private Status _statusServer;
        
	static
	{
		//org.rt.lib.Run.main(new String[] {});
	}
	
	public static void main(String[] args)
	{
		_instance = new L2LoginServer();
	}
	
	public static L2LoginServer getInstance()
	{
		return _instance;
	}
	
	public L2LoginServer()
	{
		Server.serverMode = Server.MODE_LOGINSERVER;
//      Local Constants
		final String LOG_FOLDER = "log"; // Name of folder for log file
		final String LOG_NAME = "./config/log.cfg"; // Name of log file
		
		/*** Main ***/
		// Create log folder
		File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
		logFolder.mkdir();

		// Create input stream for log file -- or store file data into memory
		InputStream is = null;
		try
		{
			is = new FileInputStream(new File(LOG_NAME));
			LogManager.getLogManager().readConfiguration(is);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (is != null)
				{
					is.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		// Load Config
		System.out.println(TimeLogger.getLogTime() + "Welcome to pwLogin.");
		
		// Prepare Database
		Config.load();
		if (isPortBusy())
		{
			System.out.println("[ERROR] Login Server is already running.");
			System.exit(0);
		}
		if (isMysqlDown())
		{
			System.out.println("[ERROR] MySQL Server is not running.");
			System.exit(0);
		}
		if (Config.LOAD_FIREWALL)
		{
			LineNumberReader lnr = null;
			BufferedReader br = null;
			FileReader fr = null;
			try
			{
				File Data = new File("./config/iptables.txt");
				if (!Data.exists()) {
					return;
				}
				fr = new FileReader(Data);
				br = new BufferedReader(fr);
				lnr = new LineNumberReader(br);

				Runtime r = Runtime.getRuntime();
				String line;
				while ((line = lnr.readLine()) != null) {
					if (line.trim().length() == 0 || line.startsWith("#")) {
						continue;
					}

					try
					{
						r.exec(line);
					}
					catch (Exception e1)
					{
						System.out.println(TimeLogger.getLogTime() + "pwLogin: [ERROR] Can't exec: " + line);
						System.out.println(TimeLogger.getLogTime() + "Reason: " + e1.getMessage());
					}
				}

				System.out.println(TimeLogger.getLogTime() + "config/iptables.txt loaded.");
			}
			catch (Exception e)
			{
				System.out.println(TimeLogger.getLogTime() + "pwLogin: [ERROR] Can't load config/iptables.txt");
				System.out.println(TimeLogger.getLogTime() + "Reason: " + e.getMessage());
			}
			finally
			{
				try
				{
					if(fr != null)
					{
						fr.close();
					}
					if(br != null)
					{
						br.close();
					}
					if(lnr != null)
					{
						lnr.close();
					}
				}
				catch(Exception e2)
				{}
			}
		}

		try
		{
			L2DatabaseFactory.getInstance();
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "DATABASE: [ERROR] Failed initializing.");
			System.out.println(TimeLogger.getLogTime() + "Reason: " + e.getMessage());
			if (Config.DEVELOPER) {
				e.printStackTrace();
			}
			System.exit(1);
		}

		try
		{
			LoginController.load();
		}
		catch (GeneralSecurityException e)
		{
			System.out.println(TimeLogger.getLogTime() + "LoginController: [ERROR] Failed initializing.");
			System.out.println(TimeLogger.getLogTime() + "Reason: " + e.getMessage());
			if (Config.DEVELOPER) {
				e.printStackTrace();
			}
			System.exit(1);
		}
		try
		{
			GameServerTable.load();
		}
		catch (GeneralSecurityException e)
		{
			System.out.println(TimeLogger.getLogTime() + "GameServerTable: [ERROR] Failed to load.");
			System.out.println(TimeLogger.getLogTime() + "Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
			System.exit(1);
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "GameServerTable: [ERROR] Failed to load.");
			System.out.println(TimeLogger.getLogTime() + "Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
			System.exit(1);
		}

		InetAddress bindAddress = null;
		if (!Config.LOGIN_BIND_ADDRESS.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.LOGIN_BIND_ADDRESS);
			}
			catch (UnknownHostException e1)
			{
				System.out.println(TimeLogger.getLogTime() + "pwLogin: [ERROR] Config IP is invalid, using all avaliable IPs.");
				System.out.println(TimeLogger.getLogTime() + "Reason: " + e1.getMessage());
				if (Config.DEVELOPER)
				{
					e1.printStackTrace();
				}
			}
		}
		
		L2LoginPacketHandler loginPacketHandler = new L2LoginPacketHandler();
		SelectorHelper sh = new SelectorHelper();
		SelectorConfig ssc = new SelectorConfig(null, null, sh, loginPacketHandler);
		try
		{
			_selectorThread = new SelectorThread(ssc, sh, sh, sh);
			_selectorThread.setAcceptFilter(sh);
		}
		catch (IOException e)
		{
			System.out.println(TimeLogger.getLogTime() + "Selector: [ERROR] Failed to open.");
			System.out.println(TimeLogger.getLogTime() + "Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
			System.exit(1);
		}
		
		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();
			System.out.println(TimeLogger.getLogTime() + "Game Server Listener: " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);
		}
		catch (IOException e)
		{
			System.out.println(TimeLogger.getLogTime() + "Game Server Listener: [ERROR] Failed to start.");
			System.out.println(TimeLogger.getLogTime() + "Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
			System.exit(1);
		}
		
		if ( Config.IS_TELNET_ENABLED )
		{
			try
			{
				_statusServer = new Status(Server.serverMode);
				_statusServer.start();
			}
			catch (IOException e)
			{
				System.out.println(TimeLogger.getLogTime() + "TELNET: [ERROR] Failed to start.");
				System.out.println(TimeLogger.getLogTime() + "Reason: " + e.getMessage());
				if (Config.DEVELOPER)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			System.out.println(TimeLogger.getLogTime() + "TELNET: off.");
		}
		
		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.PORT_LOGIN);
		}
		catch (IOException e)
		{
			System.out.println(TimeLogger.getLogTime() + "[ERROR] Failed to open server socket.");
			System.out.println(TimeLogger.getLogTime() + "Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
			System.exit(1);
		}
		_selectorThread.start();
		System.out.println(TimeLogger.getLogTime() + "########################################################");
		System.out.println(TimeLogger.getLogTime() + "#pwLogin: ready on " + (bindAddress == null ? "*" : bindAddress.getHostAddress()) + ":" + Config.PORT_LOGIN);
		System.out.println(TimeLogger.getLogTime() + "########################################################");
	}
	
	public Status getStatusServer()
	{
		return _statusServer;
	}
	
	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}
	
	public void shutdown(boolean restart)
	{
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}


	private static boolean isPortBusy()
	{
		Socket sock = null;
		try
		{
			sock = new Socket(Config.LOGIN_BIND_ADDRESS, Config.PORT_LOGIN);
			if(sock.isConnected())
			{
				return true;
			}
		}
		catch(Exception e)
		{}
		finally
		{
			try
			{
				if(sock != null)
				{
					sock.close();
				}
			}
			catch(Exception e2)
			{}
		}
		return false;
	}

	private static boolean isMysqlDown()
	{
		String[] a = Config.DATABASE_URL.split("/");
		String host = a[2];
		String port = "3306";

		a = host.split(":");
		host = a[0];
		try
		{
			port = a[1];
		}
		catch(Exception e)
		{}
		Socket sock = null;
		try
		{
			sock = new Socket(host, Integer.valueOf(port));
			if(sock.isConnected())
			{
				return false;
			}
		}
		catch(Exception e)
		{}
		finally
		{
			try
			{
				if(sock != null)
				{
					sock.close();
				}
			}
			catch(Exception e)
			{}
		}

		return true;
	}
}