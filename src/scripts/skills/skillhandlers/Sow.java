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

import ru.agecold.gameserver.ai.CtrlIntention;
import scripts.skills.ISkillHandler;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Manor;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2Skill.SkillType;
import ru.agecold.gameserver.model.actor.instance.L2MonsterInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.ActionFailed;
import ru.agecold.gameserver.network.serverpackets.PlaySound;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.util.Rnd;
import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author  l3x
 */
public class Sow implements ISkillHandler {
    private static Log _log = LogFactory.getLog(Sow.class.getName());
    private static final SkillType[] SKILL_IDS = {SkillType.SOW};

    private L2PcInstance _activeChar;
    private L2MonsterInstance _target;
    private int _seedId;

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets) {
        if (!(activeChar.isPlayer()))
            return;

        _activeChar = (L2PcInstance) activeChar;

		FastList<L2Object> targetList = skill.getTargetList(activeChar);

        if (targetList == null) {
            return;
        }

        if(_log.isDebugEnabled())
        	_log.info("Casting sow");

    	for (int index = 0; index < targetList.size(); index++) {
	    	if (!(targetList.getFirst().isL2Monster()))
	            continue;

	        _target = (L2MonsterInstance) targetList.getFirst();

	        if (_target.isSeeded()) {
	        	_activeChar.sendPacket(new ActionFailed());
	            continue;
	        }

	        if ( _target.isDead()) {
	        	_activeChar.sendPacket(new ActionFailed());
	            continue;
	        }

	        if (_target.getSeeder() != _activeChar) {
	        	_activeChar.sendPacket(new ActionFailed());
	            continue;
	        }

	        _seedId = _target.getSeedType();
	        if (_seedId == 0) {
	        	_activeChar.sendPacket(new ActionFailed());
	            continue;
	        }

	        L2ItemInstance item = _activeChar.getInventory().getItemByItemId(_seedId);
	        //Consuming used seed
	    	_activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);

	    	SystemMessage sm = null;
	    	if (calcSuccess()) {
	        	_activeChar.sendPacket(new PlaySound("Itemsound.quest_itemget"));
	            _target.setSeeded();
	            sm = SystemMessage.id(SystemMessageId.THE_SEED_WAS_SUCCESSFULLY_SOWN);
	        } else {
	        	sm = SystemMessage.id(SystemMessageId.THE_SEED_WAS_NOT_SOWN);
			}
	    	if (_activeChar.getParty() == null) {
	    		_activeChar.sendPacket(sm);
	    	} else {
	    		_activeChar.getParty().broadcastToPartyMembers(sm);
	    	}
	        //TODO: Mob should not agro on player, this way doesn't work really nice
	        _target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
    	}

    }

    private boolean calcSuccess() {
    	// TODO: check all the chances
        int basicSuccess = (L2Manor.getInstance().isAlternative(_seedId)?20:90);
		int minlevelSeed = 0;
		int maxlevelSeed = 0;
		minlevelSeed = L2Manor.getInstance().getSeedMinLevel(_seedId);
		maxlevelSeed = L2Manor.getInstance().getSeedMaxLevel(_seedId);

		int levelPlayer = _activeChar.getLevel(); // Attacker Level
		int levelTarget = _target.getLevel(); // taret Level

		// 5% decrease in chance if player level
		// is more then +/- 5 levels to _seed's_ level
		if (levelTarget < minlevelSeed)
			basicSuccess -= 5;
		if (levelTarget > maxlevelSeed)
			basicSuccess -= 5;

		// 5% decrease in chance if player level
		// is more than +/- 5 levels to _target's_ level
		int diff = (levelPlayer - levelTarget);
        if(diff < 0)
            diff = -diff;
		if (diff > 5)
			basicSuccess -= 5 * (diff-5);

		//chance can't be less than 1%
		if (basicSuccess < 1)
			basicSuccess = 1;

		int rate = Rnd.nextInt(99);

        return (rate < basicSuccess);
    }

    public SkillType[] getSkillIds() {
        return SKILL_IDS;
    }
}
