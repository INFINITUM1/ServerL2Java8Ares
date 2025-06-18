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
package scripts.zone.type;

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2Character;
import scripts.zone.L2ZoneType;

/**
 * An arena
 *
 * @author  durgus
 */
public class L2ArenaZone extends L2ZoneType {

    @SuppressWarnings("unused")
    private String _arenaName;
    private int[] _spawnLoc;
    private int _pvpType;
    private boolean _saveBuff;

    public L2ArenaZone(int id) {
        super(id);

        _spawnLoc = new int[3];
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("name")) {
            _arenaName = value;
        } else if (name.equals("spawnX")) {
            _spawnLoc[0] = Integer.parseInt(value);
        } else if (name.equals("spawnY")) {
            _spawnLoc[1] = Integer.parseInt(value);
        } else if (name.equals("spawnZ")) {
            _spawnLoc[2] = Integer.parseInt(value);
        } else if (name.equals("pvpType")) {
            _pvpType = Integer.parseInt(value);
        } else if (name.equals("saveBuff")) {
            _saveBuff = Boolean.parseBoolean(value);
        } else {
            super.setParameter(name, value);
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        enter(character, true);
        character.sendPacket(Static.ENTERED_COMBAT_ZONE);
    }

    @Override
    protected void onExit(L2Character character) {
        enter(character, false);
        character.sendPacket(Static.LEFT_COMBAT_ZONE);
    }

    private void enter(L2Character character, boolean in) {
        switch (_pvpType) {
            case 14:
                character.setFreeArena(in);
                break;
            case 7:
                character.setFreePk(in);
                character.broadcastUserInfo();
                break;
            case 8:
                character.setFreePk(in);
                if (!in) {
                    character.startPvPFlag();
                }
                character.broadcastUserInfo();
                break;
            default:
                character.setInsideZone(L2Character.ZONE_PVP, in);
                break;
        }

        if (_saveBuff) {
            character.setSaveBuff(in);
        }
    }

    @Override
    protected void onDieInside(L2Character character) {
    }

    @Override
    protected void onReviveInside(L2Character character) {
    }

    public final int[] getSpawnLoc() {
        return _spawnLoc;
    }

    @Override
    public boolean isArenaZone() {
        return true;
    }

    @Override
    public L2ArenaZone getArenaZone() {
        return this;
    }
}
