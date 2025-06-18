package net.sf.l2j;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import net.sf.l2j.mysql.Connect;

public class L2DatabaseFactory
{
	static Logger _log = Logger.getLogger(L2DatabaseFactory.class.getName());

	private static L2DatabaseFactory _instance;

	private ProviderType _providerType;
	private BoneCPDataSource connectionPool;

	public static enum ProviderType
	{
		MySql,
		MsSql;
	}

	//список используемых на данный момент коннектов
	private final Hashtable<String, Connect> Connections = new Hashtable<String, Connect>();

	public L2DatabaseFactory() throws SQLException
	{
		try
		{
			if(Config.DATABASE_MAX_CONNECTIONS < 2)
			{
				Config.DATABASE_MAX_CONNECTIONS = 2;
				System.out.println("at least " + Config.DATABASE_MAX_CONNECTIONS + " db connections are required.");
			}

			BoneCPConfig config = new BoneCPConfig();
			config.setJdbcUrl(Config.DATABASE_URL);
			config.setUsername(Config.DATABASE_LOGIN);
			config.setPassword(Config.DATABASE_PASSWORD); // the settings below are optional -- c3p0 can work with defaults
			config.setMinConnectionsPerPartition(5);
			config.setMaxConnectionsPerPartition(10);
			config.setPartitionCount(3);
			config.setAcquireIncrement(5); // if pool is exhausted, get 5 more Connections at a time
			config.setIdleConnectionTestPeriod(60);
			config.setIdleMaxAge(10);
			config.setReleaseHelperThreads(5);
			config.setAcquireRetryDelay(7000);
			config.setAcquireRetryAttempts(5);
			config.setLazyInit(false);
			config.setTransactionRecoveryEnabled(false);
			config.setQueryExecuteTimeLimit(0);
			config.setConnectionTimeout(0);

			connectionPool = new BoneCPDataSource(config);

			/* Test the connection */
			connectionPool.getConnection().close();
		}
		catch(SQLException x)
		{
			if (Config.DEBUG)
			{
				System.out.println("Database Connection FAILED");
			}
			// rethrow the exception
			throw x;
		}
		catch(Exception e)
		{
			if (Config.DEBUG)
			{
				System.out.println("Database Connection FAILED");
			}
			throw new SQLException("could not init DB connection:" + e);
		}
	}

	public final String prepQuerySelect(String[] fields, String tableName, String whereClause, boolean returnOnlyTopRecord)
	{
		String msSqlTop1 = "";
		String mySqlTop1 = "";
		if (returnOnlyTopRecord)
		{
			if (getProviderType() == ProviderType.MsSql)
			{
				msSqlTop1 = " Top 1 ";
			}
			if (getProviderType() == ProviderType.MySql)
			{
				mySqlTop1 = " Limit 1 ";
			}
		}
		String query = "SELECT " + msSqlTop1 + safetyString(fields) + " FROM " + tableName + " WHERE " + whereClause + mySqlTop1;
		return query;
	}

	public final String safetyString(String[] whatToCheck)
	{
		String braceLeft = "`";
		String braceRight = "`";
		if (getProviderType() == ProviderType.MsSql)
		{
			braceLeft = "[";
			braceRight = "]";
		}
		String result = "";
		for (String word : whatToCheck)
		{
			if (result != "")
			{
				result += ", ";
			}
			result += braceLeft + word + braceRight;
		}
		return result;
	}

	public static L2DatabaseFactory getInstance() throws SQLException
	{
		if (_instance == null)
		{
			_instance = new L2DatabaseFactory();
		}
		return _instance;
	}

	public Connect getConnection()
	{
		Connect connection;

		String key = generateKey();
		//Пробуем получить коннект из списка уже используемых. Если для данного потока уже открыт
		//коннект - не мучаем пул коннектов, а отдаем этот коннект.
		connection = Connections.get(key);
		if(connection == null)
			try
			{
				//не нашли - открываем новый
				connection = new Connect(connectionPool.getConnection());
			}
			catch(SQLException e)
			{
				System.out.println("Couldn't create connection. Cause: " + e.getMessage());
			}
		else
			//нашли - увеличиваем счетчик использования
			connection.updateCounter();

		//добавляем коннект в список
		if(connection != null)
			synchronized (Connections)
			{
				Connections.put(key, connection);
			}

		return connection;
	}

	public Hashtable<String, Connect> getConnections()
	{
		return Connections;
	}

	public void shutdown()
	{
		try
		{
			connectionPool.close();
		}
		catch(Exception e)
		{
			_log.log(Level.INFO, "", e);
		}
		Connections.clear();
	}

	/**
	 * Генерация ключа для хранения коннекта
	 *
	 * Ключ равен хэш-коду текущего потока
	 *
	 * @return сгенерированный ключ.
	 */
	public String generateKey()
	{
		return String.valueOf(Thread.currentThread().hashCode());
	}

	public final ProviderType getProviderType()
	{
		return _providerType;
	}
}