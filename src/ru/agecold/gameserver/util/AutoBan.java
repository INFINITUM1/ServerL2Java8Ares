package ru.agecold.gameserver.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import ru.agecold.L2DatabaseFactory;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;

/**
 *
 * @author Администратор
 */
public class AutoBan {
    private static Logger _log = Logger.getLogger(AutoBan.class.getName());

    public static boolean isBanned(int ObjectId)
    {
        boolean res = false;

        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        try
        {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement("SELECT MAX(endban) AS endban FROM bans WHERE obj_Id=? AND endban IS NOT NULL");
            statement.setInt(1, ObjectId);
            rset = statement.executeQuery();

            if(rset.next())
            {
                Long endban = rset.getLong("endban") * 1000L;
                res = endban > System.currentTimeMillis();
            }
        }
        catch (Exception e)
        {
            _log.warning("Could not restore ban data: " + e);
        }
        finally
        {
            Close.CSR(con, statement, rset);
        }

        return res;
    }

    public static boolean addHwidBan(String name, String hwid, String reason, long time, String gm)
    {
        Connect con = null;
        PreparedStatement statement = null;
        try
        {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("REPLACE INTO hwid_bans (player,HWID,reason,GM,end_date) values (?,?,?,?,?)");
            statement.setString(1, name);
            statement.setString(2, hwid);
            statement.setString(3, reason);
            statement.setString(4, gm);
            statement.setLong(5, time);
            statement.executeUpdate();
        }
        catch(Exception e)
        {
            return false;
        }
        finally
        {
            Close.CS(con, statement);
        }
        return true;
    }
}
