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
package ru.agecold.gameserver.model.actor.position;

import java.util.logging.Logger;
import ru.agecold.Config;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.L2WorldRegion;
import ru.agecold.util.Point3D;
import ru.agecold.util.log.AbstractLogger;

public class ObjectPosition {

    private static final Logger _log = AbstractLogger.getLogger(ObjectPosition.class.getName());
    // =========================================================
    // Data Field
    private L2Object _activeObject;
    private int _heading = 0;
    private Point3D _worldPosition;
    private L2WorldRegion _worldRegion;         // Object localization : Used for items/chars that are seen in the world

    // =========================================================
    // Constructor
    public ObjectPosition(L2Object activeObject) {
        _activeObject = activeObject;
        setWorldRegion(L2World.getInstance().getRegion(getWorldPosition()));
    }

    // =========================================================
    // Method - Public
    /**
     * Set the x,y,z position of the L2Object and if necessary modify its
     * _worldRegion.<BR><BR>
     *
     * <B><U> Assert </U> :</B><BR><BR> <li> _worldRegion != null</li><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> Update position during and
     * after movement, or after teleport </li><BR>
     */
    public final void setXYZ(int x, int y, int z) {
        if (Config.ASSERT) {
            assert getWorldRegion() != null;
        }

        setWorldPosition(x, y, z);

        try {
            if (L2World.getInstance().getRegion(getWorldPosition()) != getWorldRegion()) {
                updateWorldRegion();
            }
        } catch (Exception e) {
            _log.warning("Object Id at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + "); Going to Giran.");
            if (getActiveObject().isPlayer()) {
                getActiveObject().setXYZ(82737, 148571, -3470);
                getActiveObject().kick();
                return;
            }

            getActiveObject().decayMe();
        }
    }

    /**
     * Set the x,y,z position of the L2Object and make it invisible.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR> A L2Object is invisble if
     * <B>_hidden</B>=true or <B>_worldregion</B>==null <BR><BR>
     *
     * <B><U> Assert </U> :</B><BR><BR> <li> _worldregion==null <I>(L2Object is
     * invisible)</I></li><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR> <li> Create a Door</li> <li>
     * Restore L2PcInstance</li><BR>
     */
    public final void setXYZInvisible(int x, int y, int z) {
        if (Config.ASSERT) {
            assert getWorldRegion() == null;
        }
        if (x > L2World.MAP_MAX_X) {
            x = L2World.MAP_MAX_X - 5000;
        }
        if (x < L2World.MAP_MIN_X) {
            x = L2World.MAP_MIN_X + 5000;
        }
        if (y > L2World.MAP_MAX_Y) {
            y = L2World.MAP_MAX_Y - 5000;
        }
        if (y < L2World.MAP_MIN_Y) {
            y = L2World.MAP_MIN_Y + 5000;
        }

        setWorldPosition(x, y, z);
        getActiveObject().setIsVisible(false);
    }

    /**
     * checks if current object changed its region, if so, update referencies
     */
    public void updateWorldRegion() {
        if (!getActiveObject().isVisible()) {
            return;
        }

        L2WorldRegion newRegion = L2World.getInstance().getRegion(getWorldPosition());
        if (newRegion != getWorldRegion()) {
            getWorldRegion().removeVisibleObject(getActiveObject());

            setWorldRegion(newRegion);

            // Add the L2Oject spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
            getWorldRegion().addVisibleObject(getActiveObject());
        }
    }

    // =========================================================
    // Method - Private
    // =========================================================
    // Property - Public
    public final L2Object getActiveObject() {
        return _activeObject;
    }

    public final int getHeading() {
        return _heading;
    }

    public final void setHeading(int value) {
        _heading = value;
    }

    /**
     * Return the x position of the L2Object.
     */
    public final int getX() {
        return getWorldPosition().getX();
    }

    public final void setX(int value) {
        getWorldPosition().setX(value);
    }

    /**
     * Return the y position of the L2Object.
     */
    public final int getY() {
        return getWorldPosition().getY();
    }

    public final void setY(int value) {
        getWorldPosition().setY(value);
    }

    /**
     * Return the z position of the L2Object.
     */
    public final int getZ() {
        return getWorldPosition().getZ();
    }

    public final void setZ(int value) {
        getWorldPosition().setZ(value);
    }

    public final Point3D getWorldPosition() {
        if (_worldPosition == null) {
            _worldPosition = new Point3D(0, 0, 0);
        }
        return _worldPosition;
    }

    public final void setWorldPosition(int x, int y, int z) {
        getWorldPosition().setXYZ(x, y, z);
        //if (_activeObject.isPlayer())
        //	_log.info("Object coords: (x: " + x + ", y: " + y + ", z: " + z + ").");
    }

    public final void setWorldPosition(Point3D newPosition) {
        setWorldPosition(newPosition.getX(), newPosition.getY(), newPosition.getZ());
    }

    public final L2WorldRegion getWorldRegion() {
        return _worldRegion;
    }

    public final void setWorldRegion(L2WorldRegion value) {
        _worldRegion = value;
    }
}
