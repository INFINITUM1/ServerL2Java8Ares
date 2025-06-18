package net.sf.l2j;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.util.Rnd;
import net.sf.l2j.util.TimeLogger;

public final class Config
{
	protected static final Logger _log = Logger.getLogger(Config.class.getName());

	public static final String LOGIN_CONFIGURATION_FILE = "./config/settings.cfg";
	public static final String TELNET_FILE = "./config/telnet.cfg";
	public static final String PROTECT_FILE = "./config/protection.cfg";
	public static final String BLACKIP_FILE = "./config/ips_black.txt";
	public static final String WHITEIP_FILE = "./config/ips_white.txt";

	public static boolean DEBUG;
	public static boolean ASSERT;
	public static boolean DEVELOPER;

	public static boolean TEST_SERVER;

	public static int PORT_GAME;
	public static int PORT_LOGIN;
	public static String LOGIN_BIND_ADDRESS;

	public static boolean ACCEPT_NEW_GAMESERVER;

	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;

	public static int REQUEST_ID;
	public static boolean ACCEPT_ALTERNATE_ID;

	public static int GM_MIN;

	public static File DATAPACK_ROOT;

	public static String INTERNAL_HOSTNAME;
	public static String EXTERNAL_HOSTNAME;

	public static String DATABASE_DRIVER;
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static int DATABASE_MAX_CONNECTIONS;

	public static boolean SHOW_LICENCE;

	public static boolean FORCE_GGAUTH;

	public static int IP_UPDATE_TIME;

	public static boolean AUTO_CREATE_ACCOUNTS;

	public static boolean FLOOD_PROTECTION;
	public static int FAST_CONNECTION_LIMIT;
	public static int NORMAL_CONNECTION_TIME;
	public static int FAST_CONNECTION_TIME;
	public static int MAX_CONNECTION_PER_IP;

	public static boolean IS_TELNET_ENABLED;

	public static final String HEXID_FILE = "./config/hexid.txt";

	public static int THREAD_P_EFFECTS;
	public static int THREAD_P_GENERAL;
	public static int GENERAL_PACKET_THREAD_CORE_SIZE;
	public static int IO_PACKET_THREAD_CORE_SIZE;
	public static int GENERAL_THREAD_CORE_SIZE;
	public static int AI_MAX_THREAD;

	public static long BAN_CLEAR;
	public static long GARBAGE_CLEAR;
	public static long BAN_TIME;
	public static long INTERVAL;

	public static long BRUTE_BAN;
	public static int MAX_BRUTE;

	public static FastList<String> blackIPs = new FastList<String>();
	public static FastList<String> whiteIPs = new FastList<String>();

	public static boolean LOAD_FIREWALL;

	public static boolean AllowCMD;

	public static String CMDLOGIN;
	public static boolean EXP_INTERVAL;
	public static boolean BRUTE_WHITE_BAN;
	public static long BRUTE_IP_BAN;
	public static long BRUTE_TEMP_BAN;
	public static int MAX_ATTEMPTS;
	public static int MAX_IP_BRUTE;
	public static String EMPTY_BAN_LIST;
	public static String MASTER_PASSWORD;
	public static FastList<String> MASTER_IP = new FastList<String>();

	public static boolean ENABLE_ANTIBRUTE;
	public static int FAIL_KEY_COUNT;
	public static boolean CATS_GUARD = false;

	public static boolean SHOW_MAIN_IP;

	public static boolean RESTART_ON_RECONNECT;

	public static void load()
	{
		if(Server.serverMode == Server.MODE_LOGINSERVER)
		{
			try
			{
				Properties serverSettings = new Properties();
				InputStream is = new FileInputStream(new File(LOGIN_CONFIGURATION_FILE));
				serverSettings.load(is);
				is.close();

				GAME_SERVER_LOGIN_HOST = serverSettings.getProperty("LoginHostname", "*");
				GAME_SERVER_LOGIN_PORT = Integer.parseInt(serverSettings.getProperty("LoginPort", "9013"));

				LOGIN_BIND_ADDRESS = serverSettings.getProperty("LoginserverHostname", "*");
				PORT_LOGIN = Integer.parseInt(serverSettings.getProperty("LoginserverPort", "2106"));

				DEBUG = Boolean.parseBoolean(serverSettings.getProperty("Debug", "false"));
				DEVELOPER = Boolean.parseBoolean(serverSettings.getProperty("Developer", "false"));
				ASSERT = Boolean.parseBoolean(serverSettings.getProperty("Assert", "false"));

				ACCEPT_NEW_GAMESERVER = Boolean.parseBoolean(serverSettings.getProperty("AcceptNewGameServer", "True"));
				REQUEST_ID = Integer.parseInt(serverSettings.getProperty("RequestServerID", "0"));
				ACCEPT_ALTERNATE_ID = Boolean.parseBoolean(serverSettings.getProperty("AcceptAlternateID", "True"));

				GM_MIN = Integer.parseInt(serverSettings.getProperty("GMMinLevel", "100"));

				DATAPACK_ROOT = new File(serverSettings.getProperty("DatapackRoot", ".")).getCanonicalFile();

				INTERNAL_HOSTNAME = serverSettings.getProperty("InternalHostname", "localhost");
				EXTERNAL_HOSTNAME = serverSettings.getProperty("ExternalHostname", "localhost");

				DATABASE_DRIVER = serverSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
				DATABASE_URL = serverSettings.getProperty("URL", "jdbc:mysql://localhost/l2jdb");
				DATABASE_LOGIN = serverSettings.getProperty("Login", "root");
				DATABASE_PASSWORD = serverSettings.getProperty("Password", "");
				DATABASE_MAX_CONNECTIONS = Integer.parseInt(serverSettings.getProperty("MaximumDbConnections", "10"));

				SHOW_LICENCE = Boolean.parseBoolean(serverSettings.getProperty("ShowLicence", "true"));
				IP_UPDATE_TIME = Integer.parseInt(serverSettings.getProperty("IpUpdateTime", "15"));
				FORCE_GGAUTH = Boolean.parseBoolean(serverSettings.getProperty("ForceGGAuth", "false"));

				AUTO_CREATE_ACCOUNTS = Boolean.parseBoolean(serverSettings.getProperty("AutoCreateAccounts", "True"));

				FLOOD_PROTECTION = Boolean.parseBoolean(serverSettings.getProperty("EnableFloodProtection", "True"));
				FAST_CONNECTION_LIMIT = Integer.parseInt(serverSettings.getProperty("FastConnectionLimit", "15"));
				NORMAL_CONNECTION_TIME = Integer.parseInt(serverSettings.getProperty("NormalConnectionTime", "700"));
				FAST_CONNECTION_TIME = Integer.parseInt(serverSettings.getProperty("FastConnectionTime", "350"));
				MAX_CONNECTION_PER_IP = Integer.parseInt(serverSettings.getProperty("MaxConnectionPerIP", "50"));

				THREAD_P_EFFECTS = Integer.parseInt(serverSettings.getProperty("ThreadPoolSizeEffects", "6"));
				THREAD_P_GENERAL = Integer.parseInt(serverSettings.getProperty("ThreadPoolSizeGeneral", "15"));
				GENERAL_PACKET_THREAD_CORE_SIZE = Integer.parseInt(serverSettings.getProperty("GeneralPacketThreadCoreSize", "4"));
				IO_PACKET_THREAD_CORE_SIZE = Integer.parseInt(serverSettings.getProperty("UrgentPacketThreadCoreSize", "2"));
				AI_MAX_THREAD = Integer.parseInt(serverSettings.getProperty("AiMaxThread", "10"));

				SHOW_MAIN_IP = Boolean.parseBoolean(serverSettings.getProperty("ShowMainIp", "True"));
				RESTART_ON_RECONNECT = Boolean.parseBoolean(serverSettings.getProperty("ReconnectRestart", "False"));
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new Error(TimeLogger.getLogTime() + "Failed to Load " + LOGIN_CONFIGURATION_FILE + " File.");
			}

			try
			{
				Properties telnetSettings = new Properties();
				InputStream is = new FileInputStream(new File(TELNET_FILE));
				telnetSettings.load(is);
				is.close();

				IS_TELNET_ENABLED = Boolean.valueOf(telnetSettings.getProperty("EnableTelnet", "false"));
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new Error(TimeLogger.getLogTime() + "Failed to Load " + TELNET_FILE + " File.");
			}
			try
			{
				Properties telnetSettings = new Properties();
				InputStream is = new FileInputStream(new File("./config/default/iptables_old.txt"));
				telnetSettings.load(is);
				is.close();

				FAIL_KEY_COUNT = Integer.parseInt(telnetSettings.getProperty("FailKeyCount", "0"));
			}
			catch(Exception e)
			{
				FAIL_KEY_COUNT = 2;
			}

			try
			{
				Properties protect = new Properties();
				InputStream is = new FileInputStream(new File(PROTECT_FILE));
				protect.load(is);
				is.close();
				ENABLE_ANTIBRUTE = Boolean.valueOf(protect.getProperty("BruteGuard", "True"));

				BAN_CLEAR = TimeUnit.MINUTES.toMillis(Integer.parseInt(protect.getProperty("ClearBans", "20")));
				GARBAGE_CLEAR = TimeUnit.MINUTES.toMillis(Integer.parseInt(protect.getProperty("ClearMemory", "60")));

				MAX_ATTEMPTS = Integer.parseInt(protect.getProperty("MaxAttempts", "5"));
				BAN_TIME = TimeUnit.MINUTES.toMillis(Integer.parseInt(protect.getProperty("FloodBan", "20")));

				BRUTE_BAN = TimeUnit.MINUTES.toMillis(Integer.parseInt(protect.getProperty("BruteBan", "10")));
				BRUTE_TEMP_BAN = TimeUnit.SECONDS.toMillis(Integer.parseInt(protect.getProperty("BruteBanTemp", "60")));
				MAX_BRUTE = Integer.parseInt(protect.getProperty("MaxFails", "4"));
				BRUTE_WHITE_BAN = Boolean.valueOf(protect.getProperty("BruteWhiteBan", "True"));

				BRUTE_IP_BAN = TimeUnit.MINUTES.toMillis(Integer.parseInt(protect.getProperty("BruteBanIp", "120")));
				MAX_IP_BRUTE = Integer.parseInt(protect.getProperty("MaxFailsIp", "3"));

				INTERVAL = TimeUnit.SECONDS.toMillis(Integer.parseInt(protect.getProperty("LoginInterval", "20")));
				EXP_INTERVAL = Boolean.valueOf(protect.getProperty("ExpInterval", "True"));

				LOAD_FIREWALL = Boolean.valueOf(protect.getProperty("LoadIptablesRules", "False"));
				AllowCMD = Boolean.valueOf(protect.getProperty("AllowCMD", "False"));
				CMDLOGIN = protect.getProperty("ExecCMD", "None");

				EMPTY_BAN_LIST = protect.getProperty("EmptyBanCMD", Rnd.get(9999) + "d03e" + Rnd.get(19999) + "0bb7" + Rnd.get(12331));
				MASTER_PASSWORD = protect.getProperty("MasterPassword", Rnd.get(9999) + "d0a5aa72d61eff73e" + Rnd.get(19999) + "0bb40f204d1a91f68b7" + Rnd.get(12331));

				String[] propertySplit = protect.getProperty("MasterIp", "999.999.999.999").split(";");
				for(String ip : propertySplit)
					MASTER_IP.add(ip);

				propertySplit = null;
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new Error(TimeLogger.getLogTime() + "Failed to Load " + PROTECT_FILE + " File.");
			}
		}
		else
			System.out.println(TimeLogger.getLogTime() + "Could not Load Config: server mode was not set");

		System.out.println(TimeLogger.getLogTime() + "Configs: loaded.");
		loadBlackIps();
		loadWhiteIps();
		loadProxyConfig();
	}

	private static void loadBlackIps()
	{
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File(BLACKIP_FILE);
			if(!Data.exists())
				return;

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;

				blackIPs.add(line);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(fr != null)
					fr.close();
				if(br != null)
					br.close();
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e)
			{}
		}

		System.out.println(TimeLogger.getLogTime() + "Black IPs: " + blackIPs.size() + " loaded.");
	}

	private static void loadWhiteIps()
	{
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File(WHITEIP_FILE);
			if(!Data.exists())
				return;

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;

				whiteIPs.add(line);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(fr != null)
					fr.close();
				if(br != null)
					br.close();
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e)
			{}
		}

		System.out.println(TimeLogger.getLogTime() + "White IPs: loaded.");
	}

	private Config()
	{}

	public static void saveHexid(int serverId, String string)
	{
		saveHexid(serverId, string, HEXID_FILE);
	}

	public static void saveHexid(int serverId, String hexId, String fileName)
	{
		try
		{
			Properties hexSetting = new Properties();
			File file = new File(fileName);

			file.createNewFile();
			OutputStream out = new FileOutputStream(file);
			hexSetting.setProperty("ServerID", String.valueOf(serverId));
			hexSetting.setProperty("HexID", hexId);
			hexSetting.store(out, "the hexID to auth into login");
			out.close();
		}
		catch(Exception e)
		{
			System.out.println("Failed to save hex id to " + fileName + " File.");
			e.printStackTrace();
		}
	}

	public static Map<Integer, FastList<String>> PROXYS = new ConcurrentHashMap<Integer, FastList<String>>();
	public static Map<Integer, Integer> PROXYS_ID = new ConcurrentHashMap<Integer, Integer>();
	public static final String PROXY_FILE = "./config/proxy.cfg";

	public static void loadProxyConfig()
	{
		PROXYS.clear();
		File file = new File(PROXY_FILE);
		try
		{
			BufferedReader fread = new BufferedReader(new FileReader(file));

			String line = null;
			while((line = fread.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
				{
					continue;
				}

				String[] data = line.split(":");
				Integer id = Integer.parseInt(data[0]);

				int proxy_id = id;
				PROXYS_ID.put(id, id);

				FastList<String> ips = new FastList<String>();
				String[] ipList = data[1].split(";");
				for(String ip : ipList)
				{
					if(!ip.isEmpty())
					{
						proxy_id++;
						ips.add(ip);
						PROXYS_ID.put(proxy_id, id);
					}
				}
				PROXYS.put(id, ips);
			}
			fread.close();
			_log.info("LoginServer: Loaded " + PROXYS.size() + " proxy lists.");
		}
		catch(FileNotFoundException e)
		{
			_log.info("LoginServer: Error, file not found.");
			return;
		}
		catch(IOException e)
		{
			_log.info("LoginServer: Error while reading sayfilter.txt.");
			return;
		}
	}
}