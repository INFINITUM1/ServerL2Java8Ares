/*
 * This program is free software; you can redistribute it and/or modify
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
package ru.agecold.gameserver.model.actor.instance;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import ru.agecold.Config;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.model.L2Attackable;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2MinionData;
import ru.agecold.gameserver.model.actor.knownlist.MonsterKnownList;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.gameserver.util.MinionList;
import ru.agecold.util.Rnd;

/**
 * This class manages all Monsters.
 *
 * L2MonsterInstance :<BR><BR> <li>L2MinionInstance</li> <li>L2RaidBossInstance
 * </li>
 *
 * @version $Revision: 1.20.4.6 $ $Date: 2005/04/06 16:13:39 $
 */
public class L2MonsterInstance extends L2Attackable {
    //private static Logger _log = Logger.getLogger(L2MonsterInstance.class.getName());

    protected final MinionList _minionList;
    protected ScheduledFuture<?> _minionMaintainTask = null;
    private static final int MONSTER_MAINTENANCE_INTERVAL = 1000;
    private int _weaponEnch = 0;

    /**
     * Constructor of L2MonsterInstance (use L2Character and L2NpcInstance
     * constructor).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Call the L2Character constructor to
     * set the _template of the L2MonsterInstance (copy skills from template to
     * object and link _calculators to NPC_STD_CALCULATOR) </li> <li>Set the
     * name of the L2MonsterInstance</li> <li>Create a RandomAnimation Task that
     * will be launched after the calculated delay if the server allow it
     * </li><BR><BR>
     *
     * @param objectId Identifier of the object to initialized
     * @param L2NpcTemplate Template to apply to the NPC
     */
    public L2MonsterInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
        getKnownList();	// init knownlist
        _minionList = new MinionList(this);

        if (Config.ENCH_MONSTER_CAHNCE > 0 && Rnd.get(100) < Config.ENCH_MONSTER_CAHNCE) {
            _weaponEnch = Rnd.get(Config.ENCH_MONSTER_MINMAX.nick, Config.ENCH_MONSTER_MINMAX.title);
        }
    }

    @Override
    public final MonsterKnownList getKnownList() {
        if (super.getKnownList() == null || !(super.getKnownList() instanceof MonsterKnownList)) {
            setKnownList(new MonsterKnownList(this));
        }
        return (MonsterKnownList) super.getKnownList();
    }

    /**
     * Return True if the attacker is not another L2MonsterInstance.<BR><BR>
     */
    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        if (attacker.isL2Monster()) {
            return false;
        }

        return !isEventMob;
    }

    /**
     * Return True if the L2MonsterInstance is Agressive (aggroRange >
     * 0).<BR><BR>
     */
    @Override
    public boolean isAggressive() {
        return (getTemplate().aggroRange > 0) && !isEventMob;
    }

    @Override
    public void onSpawn() {
        super.onSpawn();

        manageMinionsOnSpawn(getTemplate().getMinionData(), getSpawnedMinions());
    }

    private void manageMinionsOnSpawn(List<L2MinionData> minionData, List<L2MinionInstance> spawnedMinions) {
        if (minionData == null || spawnedMinions == null) {
            return;
        }
        try {
            for (L2MinionInstance minion : spawnedMinions) {
                if (minion == null) {
                    continue;
                }
                //getSpawnedMinions().remove(minion);
                minion.deleteMe();
            }
            _minionList.clearRespawnList();
            manageMinions();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    protected int getMaintenanceInterval() {
        return MONSTER_MAINTENANCE_INTERVAL;
    }

    /**
     * Spawn all minions at a regular interval
     *
     */
    protected void manageMinions() {
        _minionMaintainTask = ThreadPoolManager.getInstance().scheduleAi(new Runnable() {

            public void run() {
                _minionList.spawnMinions();
            }
        }, getMaintenanceInterval(), false);
    }

    public void callMinions() {
        if (_minionList.hasMinions()) {
            for (L2MinionInstance minion : _minionList.getSpawnedMinions()) {
                // Get actual coords of the minion and check to see if it's too far away from this L2MonsterInstance
                if (!isInsideRadius(minion, 200, false, false)) {
                    // Get the coords of the master to use as a base to move the minion to
                    int masterX = getX();
                    int masterY = getY();
                    int masterZ = getZ();

                    // Calculate a new random coord for the minion based on the master's coord
                    int minionX = masterX + (Rnd.nextInt(401) - 200);
                    int minionY = masterY + (Rnd.nextInt(401) - 200);
                    int minionZ = masterZ;
                    while (((minionX != (masterX + 30)) && (minionX != (masterX - 30))) || ((minionY != (masterY + 30)) && (minionY != (masterY - 30)))) {
                        minionX = masterX + (Rnd.nextInt(401) - 200);
                        minionY = masterY + (Rnd.nextInt(401) - 200);
                    }

                    // Move the minion to the new coords
                    if (!minion.isInCombat() && !minion.isDead() && !minion.isMovementDisabled()) {
                        minion.moveToLocation(minionX, minionY, minionZ, 0);
                    }
                }
            }
        }
    }

    public void callMinionsToAssist(L2Character attacker) {
        if (_minionList.hasMinions()) {
            List<L2MinionInstance> spawnedMinions = _minionList.getSpawnedMinions();
            if (spawnedMinions != null && spawnedMinions.size() > 0) {
                Iterator<L2MinionInstance> itr = spawnedMinions.iterator();
                L2MinionInstance minion;
                while (itr.hasNext()) {
                    minion = itr.next();
                    // Trigger the aggro condition of the minion
                    if (minion != null && !minion.isDead()) {
                        if (this instanceof L2RaidBossInstance) {
                            minion.addDamage(attacker, 100);
                        } else {
                            minion.addDamage(attacker, 1);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        if (_minionMaintainTask != null) {
            _minionMaintainTask.cancel(true); // doesn't do it?
        }

        if (this instanceof L2RaidBossInstance || this instanceof L2GrandBossInstance) {
            deleteSpawnedMinions();
        }

        if (Config.MOB_PVP_FLAG
                && Config.MOB_PVP_FLAG_LIST.contains(getNpcId())
                && Config.MOB_PVP_FLAG_CONUT > 0) {
            killer.increasePvpKills(Config.MOB_PVP_FLAG_CONUT);
        }

        return true;
    }

    public List<L2MinionInstance> getSpawnedMinions() {
        return _minionList.getSpawnedMinions();
    }

    public int getTotalSpawnedMinionsInstances() {
        return _minionList.countSpawnedMinions();
    }

    public int getTotalSpawnedMinionsGroups() {
        return _minionList.lazyCountSpawnedMinionsGroups();
    }

    public void notifyMinionDied(L2MinionInstance minion) {
        _minionList.moveMinionToRespawnList(minion);
    }

    public void notifyMinionSpawned(L2MinionInstance minion) {
        _minionList.addSpawnedMinion(minion);
    }

    public boolean hasMinions() {
        return _minionList.hasMinions();
    }

    @Override
    public void addDamageHate(L2Character attacker, int damage, int aggro) {
        //if (!(attacker.isL2Monster()))
        //{
        super.addDamageHate(attacker, damage, aggro);
        //}
    }

    @Override
    public void deleteMe() {
        if (hasMinions()) {
            if (_minionMaintainTask != null) {
                _minionMaintainTask.cancel(true);
            }

            deleteSpawnedMinions();
        }
        super.deleteMe();
    }

    public void deleteSpawnedMinions() {
        for (L2MinionInstance minion : getSpawnedMinions()) {
            if (minion == null) {
                continue;
            }
            minion.abortAttack();
            minion.abortCast();
            minion.deleteMe();
            getSpawnedMinions().remove(minion);
        }
        _minionList.clearRespawnList();
    }

    @Override
    public boolean isEnemyForMob(L2Attackable mob) {
        if (mob.isL2Guard() && isAggressive()) {
            return true;
        }

        return false;
    }

    @Override
    public int getWeaponEnchant() {
        return _weaponEnch;
    }

    @Override
    public boolean isAngel() {
        switch (getTemplate().npcId) {
            case 29021:
            case 20830:
            case 20831:
            case 20858:
            case 20859:
            case 20860:
            case 21062:
            case 21063:
            case 21067:
            case 21068:
            case 21070:
            case 21071:
            case 21081:
                return true;
        }
        return false;
    }

    @Override
    public boolean isL2Monster() {
        return true;
    }

    @Override
    public void teleToLocation(int x, int y, int z, boolean allowRandomOffset) {
        if (hasMinions()) {
            if (_minionMaintainTask != null) {
                _minionMaintainTask.cancel(true);
            }

            deleteSpawnedMinions();
        }
        super.teleToLocation(x, y, z, allowRandomOffset);
    }
}
