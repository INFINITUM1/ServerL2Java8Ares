package ru.agecold.gameserver.network.smartguard.integration;

import ru.agecold.mysql.Connect;
import smartguard.api.integration.DatabaseConnection;

public class PWDatabaseConnection extends DatabaseConnection
{
	final Connect connect;

	public PWDatabaseConnection(Connect connection)
	{
		super(connection, Connect.class, "_con");
		connect = connection;
	}

	@Override
	public void close() throws Exception
	{
		connect.close();
	}
}
