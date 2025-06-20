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

import ru.agecold.Config;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.instancemanager.RaidBossSpawnManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Spawn;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.util.Rnd;


/**
 * This class manages all RaidBoss. In a group mob, there are one master called
 * RaidBoss and several slaves called Minions.
 *
 * @version $Revision: 1.20.4.6 $ $Date: 2005/04/06 16:13:39 $
 */
public class L2RaidBossInstance extends L2MonsterInstance {

    private static final int RAIDBOSS_MAINTENANCE_INTERVAL = 30000; // 30 sec
    private RaidBossSpawnManager.StatusEnum _raidStatus;

    /**
     * Constructor of L2RaidBossInstance (use L2Character and L2NpcInstance
     * constructor).<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Call the L2Character constructor to
     * set the _template of the L2RaidBossInstance (copy skills from template to
     * object and link _calculators to NPC_STD_CALCULATOR) </li> <li>Set the
     * name of the L2RaidBossInstance</li> <li>Create a RandomAnimation Task
     * that will be launched after the calculated delay if the server allow it
     * </li><BR><BR>
     *
     * @param objectId Identifier of the object to initialized
     * @param L2NpcTemplate Template to apply to the NPC
     */
    public L2RaidBossInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    @Override
    public boolean isRaid() {
        return true;
    }

    @Override
    protected int getMaintenanceInterval() {
        return RAIDBOSS_MAINTENANCE_INTERVAL;
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        L2PcInstance player = killer.getPlayer();
        if (player != null) {
            if (Config.RAID_CUSTOM_DROP && getNpcId() != 25325) {
                dropRaidCustom(player);
            }

            if (Config.RAID_CLANPOINTS_REWARD > 0) {
                if (player.getClan() != null) {
                    player.getClan().addPoints(Config.RAID_CLANPOINTS_REWARD);
                }
            }

            if (getNpcId() == 25325 && Config.BARAKIEL_NOBLESS) {
                player.setNoble(true);
                player.addItem("rewardNoble", 7694, 1, this, true);
            }

            broadcastPacket(Static.RAID_WAS_SUCCESSFUL);

			if (Config.ANNOUNCE_RAID_KILLS) {
				Announcements.getInstance().announceToAll("Игрок " + player.getName() + " убил босса " + getName());
			}
        }

        RaidBossSpawnManager.getInstance().updateStatus(this, true);
        return true;
    }

    /**
     * Spawn all minions at a regular interval Also if boss is too far from home
     * location at the time of this check, teleport it home
     *
     */
    @Override
    protected void manageMinions() {
        _minionList.spawnMinions();
        _minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable() {

            public void run() {
                // teleport raid boss home if it's too far from home location
                L2Spawn bossSpawn = getSpawn();
                if (!isInsideRadius(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), 5000, true, false)) {
                    teleToLocation(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), true);
                    healFull(); // prevents minor exploiting with it
                }
                _minionList.maintainMinions();
            }
        }, 60000, getMaintenanceInterval() + Rnd.get(5000));
    }

    public void setRaidStatus(RaidBossSpawnManager.StatusEnum status) {
        _raidStatus = status;
    }

    public RaidBossSpawnManager.StatusEnum getRaidStatus() {
        return _raidStatus;
    }

    /**
     * Reduce the current HP of the L2Attackable, update its _aggroList and
     * launch the doDie Task if necessary.<BR><BR>
     *
     */
    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake) {
        super.reduceCurrentHp(damage, attacker, awake);
    }

    public void healFull() {
        super.setCurrentHp(super.getMaxHp());
        super.setCurrentMp(super.getMaxMp());
    }
}
