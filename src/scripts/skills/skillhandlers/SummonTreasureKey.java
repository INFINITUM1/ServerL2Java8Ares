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

import java.util.logging.Logger;

import scripts.skills.ISkillHandler;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2Skill.SkillType;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.util.Rnd;
import javolution.util.FastList;

/**
 * @author evill33t
 *
 */
public class SummonTreasureKey implements ISkillHandler
{
    static Logger _log = Logger.getLogger(SummonTreasureKey.class.getName());
    private static final SkillType[] SKILL_IDS = {SkillType.SUMMON_TREASURE_KEY};

    public void useSkill(L2Character activeChar, L2Skill skill, FastList<L2Object> targets)
    {
        if (activeChar == null || !(activeChar.isPlayer())) return;

        L2PcInstance player = (L2PcInstance) activeChar;

        try
        {

            int item_id = 0;

            switch (skill.getLevel())
            {
                case 1:
                {
                  item_id = Rnd.get(6667, 6669);
                  break;
                }
                case 2:
                {
                  item_id = Rnd.get(6668, 6670);
                  break;
                }
                case 3:
                {
                  item_id = Rnd.get(6669, 6671);
                  break;
                }
                case 4:
                {
                  item_id = Rnd.get(6670, 6672);
                  break;
                }
            }
            player.addItem("Skill", item_id, Rnd.get(2,3), player, false);
        }
        catch (Exception e)
        {
            _log.warning("Error using skill summon Treasure Key:" + e);
        }
		//targets.clear();
    }

    public SkillType[] getSkillIds()
    {
        return SKILL_IDS;
    }

}
