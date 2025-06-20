package ru.agecold.gameserver.datatables;

import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Properties;
import javolution.util.FastMap;
import javolution.util.FastTable;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.idfactory.IdFactory;
import ru.agecold.gameserver.instancemanager.ClanHallManager;
import ru.agecold.gameserver.model.actor.instance.L2DoorInstance;
import ru.agecold.gameserver.model.entity.ClanHall;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.pathfinding.AbstractNodeLoc;
import ru.agecold.gameserver.templates.L2CharTemplate;
import ru.agecold.gameserver.templates.StatsSet;
import ru.agecold.util.log.AbstractLogger;

public class DoorTable {

    private static Logger _log = AbstractLogger.getLogger(DoorTable.class.getName());
    private FastMap<Integer, L2DoorInstance> _staticItems;
    private static FastTable<String> _castleDoors = new FastTable<String>();
    private static boolean _loaded = false;
    private static DoorTable _instance;

    public static DoorTable getInstance() {
        if (_instance == null) {
            _instance = new DoorTable();
        }

        return _instance;
    }

    public DoorTable() {
        _staticItems = new FastMap<Integer, L2DoorInstance>().shared("DoorTable._staticItems");
        //parseData();
    }

    public void reloadAll() {
        respawn();
    }

    public void respawn() {
//	    L2DoorInstance[] currentDoors = getDoors();
        _staticItems = null;
        _instance = null;
        _instance = new DoorTable();
    }

    public void parseData() {
        LineNumberReader lnr = null;
        try {
            File doorData = new File(Config.DATAPACK_ROOT, "data/door.csv");
            lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));

            String line = null;
            //_log.warning("Searching clan halls doors:");

            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                L2DoorInstance door = parseList(line);
                _staticItems.put(door.getDoorId(), door);
                door.spawnMe(door.getX(), door.getY(), door.getZ());
                ClanHall clanhall = ClanHallManager.getInstance().getNearbyClanHall(door.getX(), door.getY(), 500);
                if (clanhall != null) {
                    clanhall.getDoors().add(door);
                    door.setClanHall(clanhall);
                    //if (Config.DEBUG)
                    // _log.warning("door "+door.getDoorName()+" attached to ch "+clanhall.getName());
                }
            }
        } catch (FileNotFoundException e) {
            _initialized = false;
            _log.warning("door.csv is missing in data folder");
        } catch (IOException e) {
            _initialized = false;
            _log.warning("error while creating door table " + e);
        } finally {
            try {
                lnr.close();
            } catch (Exception e1) { /*
                 * ignore problems
                 */ }
        }
        _log.config("Loading DoorTable... total " + _staticItems.size() + " Doors.");
    }

    public static L2DoorInstance parseList(String line) {
        StringTokenizer st = new StringTokenizer(line, ";");

        String name = st.nextToken();
        int id = Integer.parseInt(st.nextToken());
        int x = Integer.parseInt(st.nextToken());
        int y = Integer.parseInt(st.nextToken());
        int z = Integer.parseInt(st.nextToken());
        int rangeXMin = Integer.parseInt(st.nextToken());
        int rangeYMin = Integer.parseInt(st.nextToken());
        int rangeZMin = Integer.parseInt(st.nextToken());
        int rangeXMax = Integer.parseInt(st.nextToken());
        int rangeYMax = Integer.parseInt(st.nextToken());
        int rangeZMax = Integer.parseInt(st.nextToken());
        int hp = Integer.parseInt(st.nextToken());
        int pdef = Integer.parseInt(st.nextToken());
        int mdef = Integer.parseInt(st.nextToken());
        boolean unlockable = false;

        if (st.hasMoreTokens()) {
            unlockable = Boolean.parseBoolean(st.nextToken());
        }
        boolean autoOpen = false;
        if (st.hasMoreTokens()) {
            autoOpen = Boolean.parseBoolean(st.nextToken());
        }

        if (rangeXMin > rangeXMax) {
            _log.severe("Error in door data, ID:" + id);
        }
        if (rangeYMin > rangeYMax) {
            _log.severe("Error in door data, ID:" + id);
        }
        if (rangeZMin > rangeZMax) {
            _log.severe("Error in door data, ID:" + id);
        }
        int collisionRadius; // (max) radius for movement checks
        if ((rangeXMax - rangeXMin) > (rangeYMax - rangeYMin)) {
            collisionRadius = rangeYMax - rangeYMin;
        } else {
            collisionRadius = rangeXMax - rangeXMin;
        }

        StatsSet npcDat = new StatsSet();
        npcDat.set("npcId", id);
        npcDat.set("level", 0);
        npcDat.set("jClass", "door");

        npcDat.set("baseSTR", 0);
        npcDat.set("baseCON", 0);
        npcDat.set("baseDEX", 0);
        npcDat.set("baseINT", 0);
        npcDat.set("baseWIT", 0);
        npcDat.set("baseMEN", 0);

        npcDat.set("baseShldDef", 0);
        npcDat.set("baseShldRate", 0);
        npcDat.set("baseAccCombat", 38);
        npcDat.set("baseEvasRate", 38);
        npcDat.set("baseCritRate", 38);

        //npcDat.set("name", "");
        npcDat.set("collision_radius", collisionRadius);
        npcDat.set("collision_height", rangeZMax - rangeZMin);
        npcDat.set("sex", "male");
        npcDat.set("type", "");
        npcDat.set("baseAtkRange", 0);
        npcDat.set("baseMpMax", 0);
        npcDat.set("baseCpMax", 0);
        npcDat.set("rewardExp", 0);
        npcDat.set("rewardSp", 0);
        npcDat.set("basePAtk", 0);
        npcDat.set("baseMAtk", 0);
        npcDat.set("basePAtkSpd", 0);
        npcDat.set("aggroRange", 0);
        npcDat.set("baseMAtkSpd", 0);
        npcDat.set("rhand", 0);
        npcDat.set("lhand", 0);
        npcDat.set("armor", 0);
        npcDat.set("baseWalkSpd", 0);
        npcDat.set("baseRunSpd", 0);
        npcDat.set("name", name);
        npcDat.set("baseHpMax", hp);
        npcDat.set("baseHpReg", 3.e-3f);
        npcDat.set("baseMpReg", 3.e-3f);
        npcDat.set("basePDef", pdef);
        npcDat.set("baseMDef", mdef);

        L2CharTemplate template = new L2CharTemplate(npcDat);
        L2DoorInstance door = new L2DoorInstance(IdFactory.getInstance().getNextId(), template, id, name, unlockable);
        door.setRange(rangeXMin, rangeYMin, rangeZMin, rangeXMax, rangeYMax, rangeZMax);
        try {
            door.setMapRegion(MapRegionTable.getInstance().getMapRegion(x, y));
        } catch (Exception e) {
            _log.severe("Error in door data, ID:" + id);
        }
        door.setCurrentHpMp(door.getMaxHp(), door.getMaxMp());
        door.setOpen(autoOpen);
        door.setXYZInvisible(x, y, z);

        return door;
    }

    public boolean isInitialized() {
        return _initialized;
    }
    private boolean _initialized = true;

    public L2DoorInstance getDoor(Integer id) {
        return _staticItems.get(id);
    }

    public void putDoor(L2DoorInstance door) {
        _staticItems.put(door.getDoorId(), door);
    }

    public L2DoorInstance[] getDoors() {
        L2DoorInstance[] _allTemplates = _staticItems.values().toArray(new L2DoorInstance[_staticItems.size()]);
        return _allTemplates;
    }

    /**
     * Performs a check and sets up a scheduled task for those doors that
     * require auto opening/closing.
     */
    public void checkAutoOpen() {
        for (L2DoorInstance doorInst : getDoors()) // Garden of Eva (every 7 minutes)
        {
            if (doorInst.getDoorName().startsWith("goe")) {
                doorInst.setAutoActionDelay(420000);
            } // Tower of Insolence (every 5 minutes)
            else if (doorInst.getDoorName().startsWith("aden_tower")) {
                doorInst.setAutoActionDelay(300000);
            }
        }
    }

    public void checkDoorsBetween() {
        if (_loaded) {
            return;
        }
    }

    public boolean checkIfDoorsBetween(AbstractNodeLoc start, AbstractNodeLoc end) {
        return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
    }

    public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId, boolean doubleFaceCheck) {
        return checkIfDoorsBetween(x, y, z, tx, ty, tz);
    }

    public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId) {
        return checkIfDoorsBetween(x, y, z, tx, ty, tz);
    }

    public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz) {
        /*
         * long start = System.currentTimeMillis();
         */
        int region;
        try {
            region = MapRegionTable.getInstance().getMapRegion(x, y);
        } catch (Exception e) {
            return false;
        }

        if (intersectsBorders(x, y, z, tx, ty, tz)) {
            return true;
        }

        //boolean result = false;
        for (L2DoorInstance doorInst : getDoors()) {
            if (doorInst == null) {
                continue;
            }

            if (doorInst.getMapRegion() != region) {
                continue;
            }

            if (doorInst.intersectsLine(x, y, z, tx, ty, tz)) {
                return true;
            }

            /*
             * // line segment goes through box // heavy approximation disabling
             * some shooting angles especially near 2-piece doors // but most
             * calculations should stop short // phase 1, x if (x <=
             * doorInst.getXMax() && tx >= doorInst.getXMin() || tx <=
             * doorInst.getXMax() && x >= doorInst.getXMin()) { //phase 2, y if
             * (y <= doorInst.getYMax() && ty >= doorInst.getYMin() || ty <=
             * doorInst.getYMax() && y >= doorInst.getYMin()) { // phase 3, z
             * (there's a small problem when the other is above/under door
             * level..) if (z >= doorInst.getZMin() && z <= doorInst.getZMax()
             * && tz >= doorInst.getZMin() && tz <= doorInst.getZMax()) { if
             * (doorInst.getCurrentHp() > 0 && !doorInst.getOpen()) return true;
             * } }
            }
             */
        }
        return false;
        /*
         * long time = System.currentTimeMillis() - start;
         * System.out.println("checkIfDoorsBetween; Time: " + time + " ms.");
         * return result;
         */
    }

    public boolean checkIfDoorsBetween(L2Object obj, int x, int y, int z, int tx, int ty, int tz) {
        //long start = System.currentTimeMillis();
        if (obj == null) {
            return false;
        }

        if (intersectsBorders(x, y, z, tx, ty, tz)) {
            return true;
        }

        //boolean result = false;
        for (L2DoorInstance door : obj.getKnownList().getKnownDoors()) {
            if (door == null) {
                continue;
            }

            if (door.intersectsLine(x, y, z, tx, ty, tz)) {
                return true;
            }
        }
        return false;
        /*
         * long time = System.currentTimeMillis() - start;
         * System.out.println("checkIfDoorsBetween; Time: " + time + " ms.");
         * return result;
         */
    }

    public boolean intersectsBorders(int x, int y, int z, int tx, int ty, int tz) {
        return CustomServerData.getInstance().intersectEventZone(x, y, z, tx, ty, tz);
    }
}
