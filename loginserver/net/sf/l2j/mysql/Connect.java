package net.sf.l2j.mysql;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;

public class Connect {

    static Logger _log = Logger.getLogger(Connect.class.getName());
    private final Connection myConnection;
    private int counter;

    public Connect(Connection con) {
        myConnection = con;
        counter = 1;
    }

    public void updateCounter() {
        counter++;
    }

    public void close() {
        counter--;
        if (counter == 0) {
            try {
                L2DatabaseFactory f = L2DatabaseFactory.getInstance();
                synchronized (f.getConnections())
                {
                    myConnection.close();
                    String key = f.generateKey();
                    f.getConnections().remove(key);
                }
            } catch (Exception e) {
                System.out.println("Couldn't close connection. Cause: " + e.getMessage());
            }
        }
    }

    public Statement createStatement() throws SQLException {
        return myConnection.createStatement();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return myConnection.prepareStatement(sql);
    }
}
