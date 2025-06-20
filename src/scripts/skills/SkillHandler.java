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
package scripts.skills;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import scripts.skills.skillhandlers.*;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2Skill.SkillType;
import ru.agecold.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: 1.1.4.4 $ $Date: 2005/04/03 15:55:06 $
 */
public class SkillHandler {

    private static final Logger _log = AbstractLogger.getLogger(SkillHandler.class.getName());
    private static SkillHandler _instance;
    private Map<L2Skill.SkillType, ISkillHandler> _datatable;

    public static SkillHandler getInstance() {
        if (_instance == null) {
            _instance = new SkillHandler();
        }
        return _instance;
    }

    private SkillHandler() {
        _datatable = new TreeMap<SkillType, ISkillHandler>();
        registerSkillHandler(new Blow());
        registerSkillHandler(new Pdam());
        registerSkillHandler(new Mdam());
        registerSkillHandler(new CpDam());
        registerSkillHandler(new Manadam());
        registerSkillHandler(new Heal());
        registerSkillHandler(new CombatPointHeal());
        registerSkillHandler(new ManaHeal());
        registerSkillHandler(new BalanceLife());
        registerSkillHandler(new Charge());
        registerSkillHandler(new Continuous());
        registerSkillHandler(new Resurrect());
        registerSkillHandler(new Spoil());
        registerSkillHandler(new Sweep());
        registerSkillHandler(new StrSiegeAssault());
        registerSkillHandler(new SummonFriend());
        registerSkillHandler(new SummonTreasureKey());
        registerSkillHandler(new Disablers());
        registerSkillHandler(new Recall());
        registerSkillHandler(new SiegeFlag());
        registerSkillHandler(new TakeCastle());
        registerSkillHandler(new Unlock());
        registerSkillHandler(new Craft());
        registerSkillHandler(new Fishing());
        registerSkillHandler(new FishingSkill());
        registerSkillHandler(new BeastFeed());
        registerSkillHandler(new DeluxeKey());
        registerSkillHandler(new Sow());
        registerSkillHandler(new Harvest());
        registerSkillHandler(new GetPlayer());
        registerSkillHandler(new ClanGate());
        registerSkillHandler(new GateChant());
        registerSkillHandler(new WeddingTP());
        registerSkillHandler(new ZakenTeleports());
        _log.config("SkillHandler: Loaded " + _datatable.size() + " handlers.");
    }

    public void registerSkillHandler(ISkillHandler handler) {
        SkillType[] types = handler.getSkillIds();
        for (SkillType t : types) {
            _datatable.put(t, handler);
        }
    }

    public ISkillHandler getSkillHandler(SkillType skillType) {
        return _datatable.get(skillType);
    }

    /**
     * @return
     */
    public int size() {
        return _datatable.size();
    }
}
