package ru.agecold.gameserver.instancemanager;

import ru.agecold.L2DatabaseFactory;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

public class PlayerManager
{
	private static Logger _log = Logger.getLogger(PlayerManager.class.getName());

	public static int getObjectIdByName(String name)
	{
		int result = 0;

		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.get();
			statement = con.prepareStatement("SELECT obj_Id FROM characters WHERE char_name=? LIMIT 1");
			statement.setString(1, name);
			rset = statement.executeQuery();
			if(rset.next())
				result = rset.getInt(1);
		}
		catch(Exception e)
		{
			_log.warning("PlayerManager.getObjectIdByName(String): " + e);
		}
		finally
		{
			Close.CSR(con, statement, rset);
		}

		return result;
	}

	public static String getNameByObjectId(int objectId)
	{
		String result = "";

		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.get();
			statement = con.prepareStatement("SELECT char_name FROM characters WHERE obj_Id=? LIMIT 1");
			statement.setInt(1, objectId);
			rset = statement.executeQuery();
			if(rset.next())
				result = rset.getString(1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			Close.CSR(con, statement, rset);
		}

		return result;
	}

	public static String getLastHWIDByName(String n)
	{
		String hwid = null;
		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.get();
			statement = con.prepareStatement("SELECT LastHWID FROM characters WHERE char_name=? LIMIT 1");
			statement.setString(1, n);
			rset = statement.executeQuery();
			if(rset.next())
				hwid = rset.getString(1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			Close.CSR(con, statement, rset);
		}
		return hwid != null ? hwid : "";
	}

	public static String getAccNameByName(String n)
	{
		String result = "";

		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.get();
			statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=? LIMIT 1");
			statement.setString(1, n);
			rset = statement.executeQuery();
			if(rset.next())
				result = rset.getString(1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			Close.CSR(con, statement, rset);
		}

		return result;
	}
}