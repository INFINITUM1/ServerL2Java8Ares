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

import java.lang.reflect.Constructor;

import ru.agecold.Config;
import ru.agecold.gameserver.GeoData;
import ru.agecold.gameserver.idfactory.IdFactory;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.entity.SpawnTerritory;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.util.Rnd;

/**
 * @author littlecrow
 * A special spawn implementation to spawn controllable mob
 */
public class L2GroupSpawn extends L2Spawn {

    private Constructor<?> _constructor;
    private L2NpcTemplate _template;

    public L2GroupSpawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException {
        super(mobTemplate);
        _constructor = Class.forName("ru.agecold.gameserver.model.actor.instance.L2ControllableMobInstance").getConstructors()[0];
        _template = mobTemplate;

        setAmount(1);
    }

    public L2NpcInstance doGroupSpawn() {
        L2NpcInstance mob = null;
        try {
            if (_template.type.equalsIgnoreCase("L2Pet") || _template.type.equalsIgnoreCase("L2Minion")) {
                return null;
            }

            Object[] parameters = {IdFactory.getInstance().getNextId(), _template};
            Object tmp = _constructor.newInstance(parameters);

            if (!(tmp instanceof L2NpcInstance)) {
                return null;
            }

            mob = (L2NpcInstance) tmp;

            int newlocx, newlocy, newlocz;
            newlocz = getLocz();
            if (isFree()) {
                if (getTerritory() == null) {
                    return null;
                }

                SpawnTerritory loc = getTerritory();

                int p[] = loc.getRandomPoint();
                newlocx = p[0];
                newlocy = p[1];
                newlocz = GeoData.getInstance().getSpawnHeight(newlocx, newlocy, loc.getMinZ(), loc.getMaxZ(), null);
            } else {
                newlocx = getLocx();
                newlocy = getLocy();
            }

            mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());

            if (getHeading() == -1) {
                mob.setHeading(Rnd.nextInt(61794));
            } else {
                mob.setHeading(getHeading());
            }

            mob.setSpawn(this);
            mob.spawnMe(newlocx, newlocy, newlocz);
            mob.onSpawn();
            return mob;

        } catch (Exception e) {
            _log.warning("NPC class not found: " + e);
            return null;
        }
    }
}