package net.sf.l2j.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Close {

    public static void closeConnection(Connect conn) {
        if (conn != null) {
            conn.close();
        }
    }

    public static void closeStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // quiet
            }
        }
    }

    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // quiet
            }
        }
    }

    public static void closeConStatRes(Connect conn, PreparedStatement stmt, ResultSet rs) {
        closeResultSet(rs);
        closeStatement(stmt);
        closeConnection(conn);
    }

    public static void closeConStat(Connect conn, PreparedStatement stmt) {
        closeStatement(stmt);
        closeConnection(conn);
    }

    //
    public static void closeStatRes(PreparedStatement stmt, ResultSet rs) {
        closeResultSet(rs);
        closeStatement(stmt);
    }
}
