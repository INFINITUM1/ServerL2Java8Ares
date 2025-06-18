package ru.agecold.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import javolution.util.FastList;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;
import ru.agecold.util.log.AbstractLogger;

public class HwidSpamTable {
    private static final Logger _log = AbstractLogger.getLogger(HwidSpamTable.class.getName());
    private static final FastList<String> _list = new FastList<String>();
    private static final FastList<String> _exclude = new FastList<String>();

    public static void init() {
        _list.clear();
        _exclude.clear();

        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            st = con.prepareStatement("SELECT hwid FROM `z_spam_hwid_list` ORDER BY `hwid`");
            rs = st.executeQuery();
            while (rs.next()) {
                String hwid = rs.getString("hwid");
                if (hwid.isEmpty()) {
                    continue;
                }

                _list.add(hwid);
            }
            Close.SR(st, rs);

            st = con.prepareStatement("SELECT hwid FROM `z_spam_hwid_ignore` ORDER BY `hwid`");
            rs = st.executeQuery();
            while (rs.next()) {
                String hwid = rs.getString("hwid");
                if (hwid.isEmpty()) {
                    continue;
                }

                _exclude.add(hwid);
            }
            Close.SR(st, rs);
        } catch (Exception e) {
            _log.warning("HwidSpamTable [ERROR]: cacheDonateSkills() error: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
    }

    public static boolean isSpamer(String pwhid) {
        if (_exclude.contains(pwhid)) {
            return false;
        }
        for (String hwid : _list) {
            if (hwid == null || hwid.isEmpty()) {
                continue;
            }

            if (pwhid.equalsIgnoreCase(hwid) || pwhid.endsWith(hwid)) {
                return true;
            }
        }
        return false;
    }

    public static FastList<String> getHwList()
    {
        return _list;
    }

    public static void banPlayer(L2PcInstance player, String hwid)
    {
        player.setHwidSpamer();
        for (L2PcInstance other : L2World.getInstance().getAllPlayers()) {
            if (other == null) {
                continue;
            }

            if (other.getHWID().endsWith(hwid) || other.getHWid().endsWith(hwid)) {
                other.setHwidSpamer();
            }
        }
        _list.add(hwid);
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("INSERT INTO z_spam_hwid_list (hwid) VALUES (?)");
            st.setString(1, hwid);
            st.execute();
        } catch (Exception e) {
            _log.warning("HwidSpamTable [ERROR] banPlayer()L " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    public static void ignorePlayer(L2PcInstance player, String hwid) {
        player.setHwidSpamer(false);
        for (L2PcInstance other : L2World.getInstance().getAllPlayers()) {
            if (other == null) {
                continue;
            }

            if (other.getHWID().endsWith(hwid) || other.getHWid().endsWith(hwid)) {
                other.setHwidSpamer(false);
            }
        }
        _exclude.add(hwid);
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("INSERT INTO z_spam_hwid_ignore (hwid) VALUES (?)");
            st.setString(1, hwid);
            st.execute();
        } catch (Exception e) {
            _log.warning("HwidSpamTable [ERROR] ignorePlayer()L " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    public static void deleteHwid(String hwid) {
        _list.remove(_list.indexOf(hwid));
        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("DELETE FROM z_spam_hwid_list WHERE hwid = ?");
            st.setString(1, hwid);
            st.execute();
        } catch (Exception e) {
            _log.warning("HwidSpamTable [ERROR] deleteHwid()L " + e);
        } finally {
            Close.CS(con, st);
        }
        for (L2PcInstance other : L2World.getInstance().getAllPlayers()) {
            if (other == null) {
                continue;
            }

            if (other.getHWID().endsWith(hwid) || other.getHWid().endsWith(hwid)) {
                other.setHwidSpamer(false);
            }
        }
    }
}