package ru.agecold.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javolution.util.FastMap;
import java.util.logging.Logger;

import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.model.L2TeleportLocation;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;
import ru.agecold.util.log.AbstractLogger;

public class TeleportLocationTable
{
	private static Logger _log = AbstractLogger.getLogger(TeleportLocationTable.class.getName());

	private static TeleportLocationTable _instance;

	private static FastMap<Integer, L2TeleportLocation> _teleports = new FastMap<Integer, L2TeleportLocation>().shared("TeleportLocationTable._teleports");
	
	public static TeleportLocationTable getInstance()
	{
		return _instance;
	}
	
	public static void init()
	{
		_instance = new TeleportLocationTable();
	}
	
	public TeleportLocationTable()
	{
		load();
	}

	public void reloadAll()
	{
		load();
	}
	public void load()
	{
		//_teleports = new FastMap<Integer, L2TeleportLocation>().shared();

		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT Description, id, loc_x, loc_y, loc_z, price, fornoble FROM teleport");
			rs = st.executeQuery();
			rs.setFetchSize(50);
			L2TeleportLocation teleport;

			while (rs.next())
			{
				teleport = new L2TeleportLocation();

				teleport.setTeleId(rs.getInt("id"));
				teleport.setLocX(rs.getInt("loc_x"));
				teleport.setLocY(rs.getInt("loc_y"));
				teleport.setLocZ(rs.getInt("loc_z"));
				teleport.setPrice(rs.getInt("price"));
				teleport.setIsForNoble(rs.getInt("fornoble")==1);

				_teleports.put(teleport.getTeleId(), teleport);
			}
		}
		catch (Exception e)
		{
			_log.warning("error while creating teleport table "+e);
		}
		finally
		{	
			Close.CSR(con, st, rs);
		}
		_log.config("Loading TeleportLocationTable... total " + _teleports.size() + " Teleports.");
	}


	/**
	 * @param template id
	 * @return
	 */
	public L2TeleportLocation getTemplate(int id)
	{
		return _teleports.get(id);
	}
}
