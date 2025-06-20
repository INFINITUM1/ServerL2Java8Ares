
package ru.agecold.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.templates.L2HelperBuff;
import ru.agecold.gameserver.templates.StatsSet;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;
import ru.agecold.util.log.AbstractLogger;

/**
 * This class represents the Newbie Helper Buff list
 *
 * Author: Ayor
 *
 */

public class HelperBuffTable
{

    private static Logger _log = AbstractLogger.getLogger(HennaTable.class.getName());

    private static HelperBuffTable _instance;

    /** The table containing all Buff of the Newbie Helper */
    private List<L2HelperBuff> _helperBuff;

    private boolean _initialized = true;

    /** The player level since Newbie Helper can give the fisrt buff <BR>
     *  Used to generate message : "Come back here when you have reached level ...") */
    private int _magicClassLowestLevel   = 100;
    private int _physicClassLowestLevel  = 100;

    /** The player level above which Newbie Helper won't give any buff <BR>
     *  Used to generate message : "Only novice character of level ... or less can receive my support magic.") */
    private int _magicClassHighestLevel  = 1;
    private int _physicClassHighestLevel = 1;


    public static HelperBuffTable getInstance()
    {
        if (_instance == null)
        {
            _instance = new HelperBuffTable();
        }
        return _instance;
    }


    /**
     * Create and Load the Newbie Helper Buff list from SQL Table helper_buff_list
     */
    private HelperBuffTable()
    {
        _helperBuff = new FastList<L2HelperBuff>();
        restoreHelperBuffData();

    }

    /**
     * Read and Load the Newbie Helper Buff list from SQL Table helper_buff_list
     */
    private void restoreHelperBuffData()
    {
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
        try
        {
            con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT * FROM helper_buff_list");
            rs = st.executeQuery();
			rs.setFetchSize(50);
            fillHelperBuffTable(rs);
        }
        catch (Exception e)
        {
             _log.severe("Table helper_buff_list not found : Update your DataPack" + e);
            e.printStackTrace();
        }
        finally
        {
			Close.CSR(con, st, rs);
        }
    }


    /**
     * Load the Newbie Helper Buff list from SQL Table helper_buff_list
     */
    private void fillHelperBuffTable(ResultSet HelperBuffData) throws Exception
    {

        while (HelperBuffData.next())
        {
            StatsSet helperBuffDat = new StatsSet();
            int id = HelperBuffData.getInt("id");

            helperBuffDat.set("id", id);
            helperBuffDat.set("skillID", HelperBuffData.getInt("skill_id"));
            helperBuffDat.set("skillLevel", HelperBuffData.getInt("skill_level"));
            helperBuffDat.set("lowerLevel", HelperBuffData.getInt("lower_level"));
            helperBuffDat.set("upperLevel", HelperBuffData.getInt("upper_level"));
            helperBuffDat.set("isMagicClass", HelperBuffData.getString("is_magic_class"));


            // Calulate the range level in wich player must be to obtain buff from Newbie Helper
            if("false".equals(HelperBuffData.getString("is_magic_class")))
            {
                if(HelperBuffData.getInt("lower_level")<_physicClassLowestLevel)
                    _physicClassLowestLevel=HelperBuffData.getInt("lower_level");

                if(HelperBuffData.getInt("upper_level")>_physicClassHighestLevel)
                    _physicClassHighestLevel=HelperBuffData.getInt("upper_level");
            }
            else
            {
                if(HelperBuffData.getInt("lower_level")<_magicClassLowestLevel)
                    _magicClassLowestLevel=HelperBuffData.getInt("lower_level");

                if(HelperBuffData.getInt("upper_level")>_magicClassHighestLevel)
                    _magicClassHighestLevel=HelperBuffData.getInt("upper_level");
            }

            // Add this Helper Buff to the Helper Buff List
            L2HelperBuff template = new L2HelperBuff(helperBuffDat);
            _helperBuff.add(template);
        }

        _log.config("Loading Helper Buff Table... total " + _helperBuff.size() + " Templates.");

    }

    public boolean isInitialized()
    {
        return _initialized;
    }


    public L2HelperBuff getHelperBuffTableItem(int id)
    {
        return _helperBuff.get(id);
    }


    /**
     * Return the Helper Buff List
     */
    public List<L2HelperBuff> getHelperBuffTable()
    {
        return _helperBuff;
    }


    /**
     * @return Returns the magicClassHighestLevel.
     */
    public int getMagicClassHighestLevel()
    {
        return _magicClassHighestLevel;
    }



    /**
     * @param magicClassHighestLevel The magicClassHighestLevel to set.
     */
    public void setMagicClassHighestLevel(int magicClassHighestLevel)
    {
        _magicClassHighestLevel = magicClassHighestLevel;
    }



    /**
     * @return Returns the magicClassLowestLevel.
     */
    public int getMagicClassLowestLevel()
    {
        return _magicClassLowestLevel;
    }



    /**
     * @param magicClassLowestLevel The magicClassLowestLevel to set.
     */
    public void setMagicClassLowestLevel(int magicClassLowestLevel)
    {
        _magicClassLowestLevel = magicClassLowestLevel;
    }



    /**
     * @return Returns the physicClassHighestLevel.
     */
    public int getPhysicClassHighestLevel()
    {
        return _physicClassHighestLevel;
    }



    /**
     * @param physicClassHighestLevel The physicClassHighestLevel to set.
     */
    public void setPhysicClassHighestLevel(int physicClassHighestLevel)
    {
        _physicClassHighestLevel = physicClassHighestLevel;
    }



    /**
     * @return Returns the physicClassLowestLevel.
     */
    public int getPhysicClassLowestLevel()
    {
        return _physicClassLowestLevel;
    }



    /**
     * @param physicClassLowestLevel The physicClassLowestLevel to set.
     */
    public void setPhysicClassLowestLevel(int physicClassLowestLevel)
    {
        _physicClassLowestLevel = physicClassLowestLevel;
    }


}
