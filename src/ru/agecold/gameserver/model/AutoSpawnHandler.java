/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.agecold.gameserver.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.agecold.Config;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.datatables.MapRegionTable;
import ru.agecold.gameserver.datatables.NpcTable;
import ru.agecold.gameserver.datatables.SpawnTable;
import ru.agecold.gameserver.idfactory.IdFactory;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.taskmanager.EffectTaskManager;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;
import ru.agecold.util.log.AbstractLogger;
import ru.agecold.util.Rnd;
import ru.agecold.util.Location;

/**
 * Auto Spawn Handler
 *
 * Allows spawning of a NPC object based on a timer. (From the official idea
 * used for the Merchant and Blacksmith of Mammon)
 *
 * General Usage: - Call registerSpawn() with the parameters listed below. int
 * npcId int[][] spawnPoints or specify NULL to add points later. int
 * initialDelay (If < 0 = default value) int respawnDelay (If < 0 = default
 * value) int despawnDelay (If < 0 = default value or if = 0, function disabled)
 *
 * spawnPoints is a standard two-dimensional int array containing X,Y and Z
 * coordinates. The default respawn/despawn delays are currently every hour (as
 * for Mammon on official servers). - The resulting AutoSpawnInstance object
 * represents the newly added spawn index. - The interal methods of this object
 * can be used to adjust random spawning, for instance a call to
 * setRandomSpawn(1, true); would set the spawn at index 1 to be randomly rather
 * than sequentially-based. - Also they can be used to specify the number of NPC
 * instances to spawn using setSpawnCount(), and broadcast a message to all
 * users using setBroadcast().
 *
 * Random Spawning = OFF by default Broadcasting = OFF by default
 *
 * @author Tempy
 *
 */
public class AutoSpawnHandler {

    protected static final Logger _log = AbstractLogger.getLogger(AutoSpawnHandler.class.getName());
    private static AutoSpawnHandler _instance;
    private static final int DEFAULT_INITIAL_SPAWN = 30000; // 30 seconds after registration
    private static final int DEFAULT_RESPAWN = 3600000; // 1 hour in millisecs
    private static final int DEFAULT_DESPAWN = 3600000; // 1 hour in millisecs
    protected Map<Integer, AutoSpawnInstance> _registeredSpawns;
    @SuppressWarnings("unchecked")
    protected Map<Integer, ScheduledFuture> _runningSpawns;
    protected boolean _activeState = true;

    @SuppressWarnings("unchecked")
    private AutoSpawnHandler() {
        _registeredSpawns = new FastMap<Integer, AutoSpawnInstance>();
        _runningSpawns = new FastMap<Integer, ScheduledFuture>();

        restoreSpawnData();
    }

    public static AutoSpawnHandler getInstance() {
        return _instance;
    }

    public static void init() {
        _instance = new AutoSpawnHandler();
    }

    public final int size() {
        return _registeredSpawns.size();
    }

    private void restoreSpawnData() {
        int numLoaded = 0;

        Connect con = null;
        PreparedStatement statement = null;
        PreparedStatement statement2 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        try {

            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            // Restore spawn group data, then the location data.
            statement = con.prepareStatement("SELECT * FROM random_spawn ORDER BY groupId ASC");
            rs = statement.executeQuery();

            while (rs.next()) {
                // Register random spawn group, set various options on the
                // created spawn instance.
                AutoSpawnInstance spawnInst = registerSpawn(rs.getInt("npcId"), rs.getInt("initialDelay"), rs.getInt("respawnDelay"), rs.getInt("despawnDelay"));

                spawnInst.setSpawnCount(rs.getInt("count"));
                spawnInst.setBroadcast(rs.getBoolean("broadcastSpawn"));
                spawnInst.setRandomSpawn(rs.getBoolean("randomSpawn"));
                numLoaded++;

                // Restore the spawn locations for this spawn group/instance.
                statement2 = con.prepareStatement("SELECT * FROM random_spawn_loc WHERE groupId=?");
                statement2.setInt(1, rs.getInt("groupId"));
                rs2 = statement2.executeQuery();

                while (rs2.next()) {
                    // Add each location to the spawn group/instance.
                    spawnInst.addSpawnLocation(rs2.getInt("x"), rs2.getInt("y"), rs2.getInt("z"), rs2.getInt("heading"));
                }

                Close.S(statement2);
            }
        } catch (Exception e) {
            _log.warning("AutoSpawnHandler: Could not restore spawn data: "
                    + e);
        } finally {
            Close.SR(statement2, rs2);
            Close.CSR(con, statement, rs);
        }
    }

    /**
     * Registers a spawn with the given parameters with the spawner, and marks
     * it as active. Returns a AutoSpawnInstance containing info about the
     * spawn.
     *
     * @param int npcId
     * @param int[][] spawnPoints
     * @param int initialDelay (If < 0 = default value) @param int
     * respawnDelay (If < 0 = default value) @param int
     * despawnDelay (If < 0 = default value or if = 0, function disabled)
     * @return AutoSpawnInstance spawnInst
     */
    public AutoSpawnInstance registerSpawn(int npcId, int[][] spawnPoints, int initialDelay, int respawnDelay,
            int despawnDelay) {
        if (initialDelay < 0) {
            initialDelay = DEFAULT_INITIAL_SPAWN;
        }

        if (respawnDelay < 0) {
            respawnDelay = DEFAULT_RESPAWN;
        }

        if (despawnDelay < 0) {
            despawnDelay = DEFAULT_DESPAWN;
        }

        AutoSpawnInstance newSpawn = new AutoSpawnInstance(npcId, initialDelay, respawnDelay, despawnDelay);

        if (spawnPoints != null) {
            for (int[] spawnPoint : spawnPoints) {
                newSpawn.addSpawnLocation(spawnPoint);
            }
        }

        int newId = IdFactory.getInstance().getNextId();
        newSpawn._objectId = newId;
        _registeredSpawns.put(newId, newSpawn);

        setSpawnActive(newSpawn, true);

        //if (Config.DEBUG)
        //_log.config("AutoSpawnHandler: Registered auto spawn for NPC ID "+ npcId + " (Object ID = " + newId + ").");
        return newSpawn;
    }

    /**
     * Registers a spawn with the given parameters with the spawner, and marks
     * it as active. Returns a AutoSpawnInstance containing info about the
     * spawn. <BR>
     * <B>Warning:</B> Spawn locations must be specified separately using
     * addSpawnLocation().
     *
     * @param int npcId
     * @param int initialDelay (If < 0 = default value) @param int
     * respawnDelay (If < 0 = default value) @param int
     * despawnDelay (If < 0 = default value or if = 0, function disabled)
     * @return AutoSpawnInstance spawnInst
     */
    public AutoSpawnInstance registerSpawn(int npcId, int initialDelay, int respawnDelay, int despawnDelay) {
        return registerSpawn(npcId, null, initialDelay, respawnDelay, despawnDelay);
    }

    /**
     * Remove a registered spawn from the list, specified by the given spawn
     * instance.
     *
     * @param AutoSpawnInstance spawnInst
     * @return boolean removedSuccessfully
     */
    public boolean removeSpawn(AutoSpawnInstance spawnInst) {
        if (spawnInst == null) {
            return false;
        }

        if (!isSpawnRegistered(spawnInst)) {
            return false;
        }

        try {
            // Try to remove from the list of registered spawns if it exists.
            _registeredSpawns.remove(spawnInst.getNpcId());

            // Cancel the currently associated running scheduled task.
            ScheduledFuture<?> respawnTask = _runningSpawns.remove(spawnInst._objectId);
            respawnTask.cancel(false);

            //if (Config.DEBUG)
            //	_log.config("AutoSpawnHandler: Removed auto spawn for NPC ID "
            //			+ spawnInst._npcId + " (Object ID = "
            //			+ spawnInst._objectId + ").");
        } catch (Exception e) {
            _log.warning("AutoSpawnHandler: Could not auto spawn for NPC ID "
                    + spawnInst._npcId + " (Object ID = " + spawnInst._objectId
                    + "): " + e);
            return false;
        }

        return true;
    }

    /**
     * Remove a registered spawn from the list, specified by the given spawn
     * object ID.
     *
     * @param int objectId
     * @return boolean removedSuccessfully
     */
    public void removeSpawn(int objectId) {
        removeSpawn(_registeredSpawns.get(objectId));
    }

    /**
     * Sets the active state of the specified spawn.
     *
     * @param AutoSpawnInstance spawnInst
     * @param boolean isActive
     */
    public void setSpawnActive(AutoSpawnInstance spawnInst, boolean isActive) {
        if (spawnInst == null) {
            return;
        }

        int objectId = spawnInst._objectId;

        if (isSpawnRegistered(objectId)) {
            ScheduledFuture<?> spawnTask = null;

            if (isActive) {
                AutoSpawner rs = new AutoSpawner(objectId);

                if (spawnInst._desDelay > 0) {
                    spawnTask = EffectTaskManager.getInstance().scheduleAtFixedRate(rs,
                            spawnInst._initDelay, spawnInst._resDelay);
                } else {
                    spawnTask = EffectTaskManager.getInstance().schedule(rs, spawnInst._initDelay);
                }

                _runningSpawns.put(objectId, spawnTask);
            } else {
                AutoDespawner rd = new AutoDespawner(objectId);
                spawnTask = _runningSpawns.remove(objectId);

                if (spawnTask != null) {
                    spawnTask.cancel(false);
                }

                ThreadPoolManager.getInstance().executeGeneral(rd);
            }

            spawnInst.setSpawnActive(isActive);
        }
    }

    /**
     * Sets the active state of all auto spawn instances to that specified, and
     * cancels the scheduled spawn task if necessary.
     *
     * @param boolean isActive
     */
    public void setAllActive(boolean isActive) {
        if (_activeState == isActive) {
            return;
        }

        for (AutoSpawnInstance spawnInst : _registeredSpawns.values()) {
            setSpawnActive(spawnInst, isActive);
        }

        _activeState = isActive;
    }

    /**
     * Returns the number of milliseconds until the next occurrance of the given
     * spawn.
     *
     * @param AutoSpawnInstance spawnInst
     * @param long milliRemaining
     */
    public final long getTimeToNextSpawn(AutoSpawnInstance spawnInst) {
        int objectId = spawnInst.getObjectId();

        if (!isSpawnRegistered(objectId)) {
            return -1;
        }

        return _runningSpawns.get(objectId).getDelay(TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to return the AutoSpawnInstance associated with the given NPC or
     * Object ID type. <BR>
     * Note: If isObjectId == false, returns first instance for the specified
     * NPC ID.
     *
     * @param int id
     * @param boolean isObjectId
     * @return AutoSpawnInstance spawnInst
     */
    public final AutoSpawnInstance getAutoSpawnInstance(int id, boolean isObjectId) {
        if (isObjectId) {
            if (isSpawnRegistered(id)) {
                return _registeredSpawns.get(id);
            }
        } else {
            for (AutoSpawnInstance spawnInst : _registeredSpawns.values()) {
                if (spawnInst.getNpcId() == id) {
                    return spawnInst;
                }
            }
        }
        return null;
    }

    public Map<Integer, AutoSpawnInstance> getAutoSpawnInstances(int npcId) {
        Map<Integer, AutoSpawnInstance> spawnInstList = new FastMap<Integer, AutoSpawnInstance>();

        for (AutoSpawnInstance spawnInst : _registeredSpawns.values()) {
            if (spawnInst.getNpcId() == npcId) {
                spawnInstList.put(spawnInst.getObjectId(), spawnInst);
            }
        }

        return spawnInstList;
    }

    /**
     * Tests if the specified object ID is assigned to an auto spawn.
     *
     * @param int objectId
     * @return boolean isAssigned
     */
    public final boolean isSpawnRegistered(int objectId) {
        return _registeredSpawns.containsKey(objectId);
    }

    /**
     * Tests if the specified spawn instance is assigned to an auto spawn.
     *
     * @param AutoSpawnInstance spawnInst
     * @return boolean isAssigned
     */
    public final boolean isSpawnRegistered(AutoSpawnInstance spawnInst) {
        return _registeredSpawns.containsValue(spawnInst);
    }

    /**
     * AutoSpawner Class <BR>
     * <BR>
     * This handles the main spawn task for an auto spawn instance, and
     * initializes a despawner if required.
     *
     * @author Tempy
     */
    private class AutoSpawner implements Runnable {

        private int _objectId;

        protected AutoSpawner(int objectId) {
            _objectId = objectId;
        }

        public void run() {
            try {
                // Retrieve the required spawn instance for this spawn task.
                AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);

                // If the spawn is not scheduled to be active, cancel the spawn
                // task.
                if (!spawnInst.isSpawnActive()) {
                    return;
                }

                Location[] locationList = spawnInst.getLocationList();

                // If there are no set co-ordinates, cancel the spawn task.
                if (locationList.length == 0) {
                    //_log.info("AutoSpawnHandler: No location co-ords specified for spawn instance (Object ID = "+ _objectId + ").");
                    return;
                }

                int locationCount = locationList.length;
                int locationIndex = Rnd.nextInt(locationCount);

                /*
                 * If random spawning is disabled, the spawn at the next set of
                 * co-ordinates after the last. If the index is greater than the
                 * number of possible spawns, reset the counter to zero.
                 */
                if (!spawnInst.isRandomSpawn()) {
                    locationIndex = spawnInst._lastLocIndex;
                    locationIndex++;

                    if (locationIndex == locationCount) {
                        locationIndex = 0;
                    }

                    spawnInst._lastLocIndex = locationIndex;
                }

                // Set the X, Y and Z co-ordinates, where this spawn will take
                // place.
                final int x = locationList[locationIndex].getX();
                final int y = locationList[locationIndex].getY();
                final int z = locationList[locationIndex].getZ();
                final int heading = locationList[locationIndex].getHeading();

                // Fetch the template for this NPC ID and create a new spawn.
                L2NpcTemplate npcTemp = NpcTable.getInstance().getTemplate(spawnInst.getNpcId());
                if (npcTemp == null) {
                    _log.warning("Couldnt find NPC id" + spawnInst.getNpcId() + " Try to update your DP");
                    return;
                }
                L2Spawn newSpawn = new L2Spawn(npcTemp);

                newSpawn.setLocx(x);
                newSpawn.setLocy(y);
                newSpawn.setLocz(z);
                if (heading != -1) {
                    newSpawn.setHeading(heading);
                }
                newSpawn.setAmount(spawnInst.getSpawnCount());
                if (spawnInst._desDelay == 0) {
                    newSpawn.setRespawnDelay(spawnInst._resDelay);
                }

                // Add the new spawn information to the spawn table, but do not
                // store it.
                SpawnTable.getInstance().addNewSpawn(newSpawn, false);
                L2NpcInstance npcInst = null;

                if (spawnInst._spawnCount == 1) {
                    npcInst = newSpawn.doSpawn();
                    npcInst.setXYZ(npcInst.getX(), npcInst.getY(), npcInst.getZ());
                    spawnInst.addNpcInstance(npcInst);
                } else {
                    for (int i = 0; i < spawnInst._spawnCount; i++) {
                        npcInst = newSpawn.doSpawn();

                        // To prevent spawning of more than one NPC in the exact
                        // same spot,
                        // move it slightly by a small random offset.
                        npcInst.setXYZ(npcInst.getX() + Rnd.nextInt(50), npcInst.getY() + Rnd.nextInt(50),
                                npcInst.getZ());

                        // Add the NPC instance to the list of managed
                        // instances.
                        spawnInst.addNpcInstance(npcInst);
                    }
                }

                String nearestTown = MapRegionTable.getInstance().getClosestTownName(npcInst);

                // Announce to all players that the spawn has taken place, with
                // the nearest town location.
                if (spawnInst.isBroadcasting()) {
                    Announcements.getInstance().announceToAll("The " + npcInst.getName() + " has spawned near "
                            + nearestTown + "!");
                }

                //if (Config.DEBUG)
                //	_log.info("AutoSpawnHandler: Spawned NPC ID "
                //			+ spawnInst.getNpcId() + " at " + x + ", " + y
                //			+ ", " + z + " (Near " + nearestTown + ") for "
                //		+ (spawnInst.getRespawnDelay() / 60000)
                //		+ " minute(s).");
                // If there is no despawn time, do not create a despawn task.
                if (spawnInst.getDespawnDelay() > 0) {
                    AutoDespawner rd = new AutoDespawner(_objectId);
                    ThreadPoolManager.getInstance().scheduleAi(rd, spawnInst.getDespawnDelay() - 1000, false);
                }
            } catch (Exception e) {
                _log.warning("AutoSpawnHandler: An error occurred while initializing spawn instance (Object ID = "
                        + _objectId + "): " + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * AutoDespawner Class <BR>
     * <BR>
     * Simply used as a secondary class for despawning an auto spawn instance.
     *
     * @author Tempy
     */
    private class AutoDespawner implements Runnable {

        private int _objectId;

        protected AutoDespawner(int objectId) {
            _objectId = objectId;
        }

        public void run() {
            try {
                AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);

                if (spawnInst == null) {
                    _log.info("AutoSpawnHandler: No spawn registered for object ID = " + _objectId + ".");
                    return;
                }

                for (L2NpcInstance npcInst : spawnInst.getNPCInstanceList()) {
                    if (npcInst == null) {
                        continue;
                    }

                    npcInst.deleteMe();
                    spawnInst.removeNpcInstance(npcInst);

                    //if (Config.DEBUG)
                    //	_log.info("AutoSpawnHandler: Spawns removed for spawn instance (Object ID = "+ _objectId + ").");
                }
            } catch (Exception e) {
                _log.warning("AutoSpawnHandler: An error occurred while despawning spawn (Object ID = "
                        + _objectId + "): " + e);
            }
        }
    }

    /**
     * AutoSpawnInstance Class <BR>
     * <BR>
     * Stores information about a registered auto spawn.
     *
     * @author Tempy
     */
    public static class AutoSpawnInstance {

        protected int _objectId;
        protected int _spawnIndex;
        protected int _npcId;
        protected int _initDelay;
        protected int _resDelay;
        protected int _desDelay;
        protected int _spawnCount = 1;
        protected int _lastLocIndex = -1;
        private List<L2NpcInstance> _npcList = new FastList<L2NpcInstance>();
        private List<Location> _locList = new FastList<Location>();
        private boolean _spawnActive;
        private boolean _randomSpawn = false;
        private boolean _broadcastAnnouncement = false;

        protected AutoSpawnInstance(int npcId, int initDelay, int respawnDelay, int despawnDelay) {
            _npcId = npcId;
            _initDelay = initDelay;
            _resDelay = respawnDelay;
            _desDelay = despawnDelay;
        }

        protected void setSpawnActive(boolean activeValue) {
            _spawnActive = activeValue;
        }

        protected boolean addNpcInstance(L2NpcInstance npcInst) {
            return _npcList.add(npcInst);
        }

        protected boolean removeNpcInstance(L2NpcInstance npcInst) {
            return _npcList.remove(npcInst);
        }

        public int getObjectId() {
            return _objectId;
        }

        public int getInitialDelay() {
            return _initDelay;
        }

        public int getRespawnDelay() {
            return _resDelay;
        }

        public int getDespawnDelay() {
            return _desDelay;
        }

        public int getNpcId() {
            return _npcId;
        }

        public int getSpawnCount() {
            return _spawnCount;
        }

        public Location[] getLocationList() {
            return _locList.toArray(new Location[_locList.size()]);
        }

        public L2NpcInstance[] getNPCInstanceList() {
            L2NpcInstance[] ret;
            synchronized (_npcList) {
                ret = new L2NpcInstance[_npcList.size()];
                _npcList.toArray(ret);
            }

            return ret;
        }

        public L2Spawn[] getSpawns() {
            List<L2Spawn> npcSpawns = new FastList<L2Spawn>();

            for (L2NpcInstance npcInst : _npcList) {
                npcSpawns.add(npcInst.getSpawn());
            }

            return npcSpawns.toArray(new L2Spawn[npcSpawns.size()]);
        }

        public void setSpawnCount(int spawnCount) {
            _spawnCount = spawnCount;
        }

        public void setRandomSpawn(boolean randValue) {
            _randomSpawn = randValue;
        }

        public void setBroadcast(boolean broadcastValue) {
            _broadcastAnnouncement = broadcastValue;
        }

        public boolean isSpawnActive() {
            return _spawnActive;
        }

        public boolean isRandomSpawn() {
            return _randomSpawn;
        }

        public boolean isBroadcasting() {
            return _broadcastAnnouncement;
        }

        public boolean addSpawnLocation(int x, int y, int z, int heading) {
            return _locList.add(new Location(x, y, z, heading));
        }

        public boolean addSpawnLocation(int[] spawnLoc) {
            if (spawnLoc.length != 3) {
                return false;
            }

            return addSpawnLocation(spawnLoc[0], spawnLoc[1], spawnLoc[2], -1);
        }

        public Location removeSpawnLocation(int locIndex) {
            try {
                return _locList.remove(locIndex);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }
}
