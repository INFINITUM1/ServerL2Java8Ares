
package ru.agecold.gameserver.datatables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import javolution.util.FastMap;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.idfactory.IdFactory;
import ru.agecold.gameserver.model.actor.instance.L2StaticObjectInstance;
import ru.agecold.util.log.AbstractLogger;

public class StaticObjects
{
    private static Logger _log = AbstractLogger.getLogger(StaticObjects.class.getName());

    private static StaticObjects _instance;
    private FastMap<Integer,L2StaticObjectInstance> _staticObjects;


    public static StaticObjects getInstance()
    {
        if(_instance == null)
            _instance = new StaticObjects();
        return _instance;
    }

    public StaticObjects()
    {
        _staticObjects = new FastMap<Integer,L2StaticObjectInstance>().shared("StaticObjects._staticObjects");
        parseData();
        _log.config("Loading StaticObjects... total " + _staticObjects.size() + " objects.");
    }

    private void parseData()
    {
        LineNumberReader lnr = null;
        try
        {
            File doorData = new File(Config.DATAPACK_ROOT, "data/staticobjects.csv");
            lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));

            String line = null;
            while ((line = lnr.readLine()) != null)
            {
                if (line.trim().length() == 0 || line.startsWith("#"))
                    continue;

                L2StaticObjectInstance obj = parse(line);
                _staticObjects.put(obj.getStaticObjectId(),obj);
            }
        }
        catch (FileNotFoundException e)
        {
            _log.warning("staticobjects.csv is missing in data folder");
        }
        catch (Exception e)
        {
            _log.warning("error while creating StaticObjects table " + e);
        }
        finally
        {
            try { lnr.close(); } catch (Exception e) {}
        }
    }

    public static L2StaticObjectInstance parse(String line)
    {
        StringTokenizer st = new StringTokenizer(line, ";");

        st.nextToken(); //Pass over static object name (not used in server)

        int id = Integer.parseInt(st.nextToken());
        int x = Integer.parseInt(st.nextToken());
        int y = Integer.parseInt(st.nextToken());
        int z = Integer.parseInt(st.nextToken());
        int type = Integer.parseInt(st.nextToken());
        String texture = st.nextToken();
        int map_x = Integer.parseInt(st.nextToken());
        int map_y = Integer.parseInt(st.nextToken());

        L2StaticObjectInstance obj = new L2StaticObjectInstance(IdFactory.getInstance().getNextId());
        obj.setType(type);
        obj.setStaticObjectId(id);
        obj.setXYZ(x, y, z);
        obj.setMap(texture, map_x, map_y);
        obj.spawnMe();

        return obj;
    }
}
