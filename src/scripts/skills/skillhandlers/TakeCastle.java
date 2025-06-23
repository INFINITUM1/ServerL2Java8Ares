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
package scripts.skills.skillhandlers;

import ru.agecold.gameserver.util.Util;
import scripts.skills.ISkillHandler;
import ru.agecold.gameserver.instancemanager.CastleManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2Skill.SkillType;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.entity.Castle;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import javolution.util.FastList;

/**
 * @author _drunk_
 *
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class TakeCastle implements ISkillHandler {
    //private static Logger _log = Logger.getLogger(TakeCastle.class.getName());

    private static final SkillType[] SKILL_IDS = {SkillType.TAKECASTLE};

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (activeChar == null || !(activeChar.isPlayer())) {
            return;
        }
        if (targets.size() == 0) {
            return;
        }

        L2PcInstance player = activeChar.getPlayer();
        if (player.getClan() == null || player.getClan().getLeaderId() != player.getObjectId()) {
            return;
        }

        Castle castle = CastleManager.getInstance().getCastle(player);
        if (castle == null || !checkIfOkToCastSealOfRule(player, castle, true, skill, targets.get(0))) {
            return;
        }

        try {
            castle.Engrave(player.getClan(), targets.get(0).getObjectId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //targets.clear();
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }

    /**
     * Return true if character clan place a flag<BR><BR>
     *
     * @param activeChar The L2Character of the character placing the flag
     *
     */
    public static boolean checkIfOkToCastSealOfRule(L2Character activeChar, Castle castle, boolean isCheckOnly, L2Skill skill, L2Object object) {
        if (activeChar == null || !(activeChar.isPlayer())) {
            return false;
        }

        SystemMessage sm = SystemMessage.id(SystemMessageId.S1_S2);
        L2PcInstance player = activeChar.getPlayer();
        if (castle == null || castle.getCastleId() <= 0) {
            sm.addString("Не подходящее место для чтения печати.");
        } else if (!castle.getSiege().getIsInProgress()) {
            sm.addString("You can only use this skill during a siege.");
        } else if (!Util.checkIfInRange(200, activeChar, object, true)) {
            sm.addString("Dist too far casting stopped");
        } else if (castle.getSiege().getAttackerClan(activeChar.getClan()) == null) {
            sm.addString("You must be an attacker to use this skill");
        } else if (!activeChar.getPlayer().isInSiegeRuleArea()) {
            sm.addString("You must be an attacker to use this skill");
        } else {
            if (!isCheckOnly) {
                castle.getSiege().announceToPlayer("Клан " + player.getClan().getName() + " начал чтение печати.", true);
            }
            return true;
        }

        activeChar.sendPacket(sm);

        return false;
    }
}
