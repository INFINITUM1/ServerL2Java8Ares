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
package ru.agecold.gameserver.model.actor.instance;

import java.util.List;

import javolution.util.FastList;
import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Spawn;
import ru.agecold.gameserver.network.serverpackets.MyTargetSelected;
import ru.agecold.gameserver.network.serverpackets.StatusUpdate;
import ru.agecold.gameserver.network.serverpackets.ValidateLocation;
import ru.agecold.gameserver.templates.L2NpcTemplate;

public class L2ControlTowerInstance extends L2NpcInstance {

    private List<L2Spawn> _guards;

    public L2ControlTowerInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public boolean isAttackable() {
        // Attackable during siege by attacker only
        return (getCastle() != null
                && getCastle().getCastleId() > 0
                && getCastle().getSiege().getIsInProgress());
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        // Attackable during siege by attacker only
        return (attacker != null
                && attacker.isPlayer()
                && getCastle() != null
                && getCastle().getCastleId() > 0
                && getCastle().getSiege().getIsInProgress()
                && getCastle().getSiege().checkIsAttacker(attacker.getClan()));
    }

    @Override
    public void onForcedAttack(L2PcInstance player) {
        onAction(player);
    }

    @Override
    public void onAction(L2PcInstance player) {
        if (!canTarget(player)) {
            return;
        }

        // Check if the L2PcInstance already target the L2NpcInstance
        if (this != player.getTarget()) {
            // Set the target of the L2PcInstance player
            player.setTarget(this);

            // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
            player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

            // Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
            StatusUpdate su = new StatusUpdate(getObjectId());
            su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
            su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
            player.sendPacket(su);

            // Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
            player.sendPacket(new ValidateLocation(this));
        } else {
            if (isAutoAttackable(player) && Math.abs(player.getZ() - getZ()) < 100 && player.canSeeTarget(this)) {
                // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);

                // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
                player.sendActionFailed();
            }
        }
    }

    public void onDeath() {
        if (getCastle().getSiege().getIsInProgress()) {
            getCastle().getSiege().killedCT(this);

            if (getGuards() != null && getGuards().size() > 0) {
                for (L2Spawn spawn : getGuards()) {
                    if (spawn == null) {
                        continue;
                    }
                    spawn.stopRespawn();
                    //spawn.getLastSpawn().doDie(spawn.getLastSpawn());
                }
            }
        }
    }

    public void registerGuard(L2Spawn guard) {
        getGuards().add(guard);
    }

    public final List<L2Spawn> getGuards() {
        if (_guards == null) {
            _guards = new FastList<L2Spawn>();
        }
        return _guards;
    }

    @Override
    public boolean isL2ControlTower() {
        return true;
    }
}
