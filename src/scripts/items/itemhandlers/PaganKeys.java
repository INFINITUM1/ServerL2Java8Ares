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
package scripts.items.itemhandlers;

import scripts.items.IItemHandler;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.actor.instance.L2DoorInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PlayableInstance;
import ru.agecold.gameserver.network.serverpackets.PlaySound;
import ru.agecold.gameserver.network.serverpackets.SocialAction;
import ru.agecold.util.Rnd;

/**
 * @author  chris
 */
public class PaganKeys implements IItemHandler {

    private static final int[] ITEM_IDS = {8273, 8274, 8275};
    public static final int INTERACTION_DISTANCE = 100;

    public void useItem(L2PlayableInstance playable, L2ItemInstance item) {

        int itemId = item.getItemId();
        if (!(playable.isPlayer())) {
            return;
        }
        L2PcInstance activeChar = (L2PcInstance) playable;
        L2Object target = activeChar.getTarget();

        if (target == null || !(target.isL2Door())) {
            activeChar.sendPacket(Static.INCORRECT_TARGET);
            activeChar.sendActionFailed();
            return;
        }
        L2DoorInstance door = (L2DoorInstance) target;

        if (!(activeChar.isInsideRadius(door, INTERACTION_DISTANCE, false, false))) {
            activeChar.sendMessage("Too far.");
            activeChar.sendActionFailed();
            return;
        }
        if (activeChar.getAbnormalEffect() > 0 || activeChar.isInCombat()) {
            activeChar.sendMessage("You cannot use the key now.");
            activeChar.sendActionFailed();
            return;
        }

        int openChance = 35;

        if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false)) {
            return;
        }

        switch (itemId) {
            case 8273: //AnteroomKey
                if (door.getDoorName().startsWith("Anteroom")) {
                    if (openChance > 0 && Rnd.get(100) < openChance) {
                        activeChar.sendMessage("You opened Anterooms Door.");
                        door.openMe();
                        door.onOpen(); // Closes the door after 60sec
                        activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
                    } else {
                        //test with: activeChar.sendPacket(SystemMessage.id(SystemMessage.FAILED_TO_UNLOCK_DOOR));
                        activeChar.sendMessage("You failed to open Anterooms Door.");
                        activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
                        PlaySound playSound = new PlaySound("interfacesound.system_close_01");
                        activeChar.sendPacket(playSound);
                    }
                } else {
                    activeChar.sendMessage("Incorrect Door.");
                }
                break;
            case 8274: //Chapelkey, Capel Door has a Gatekeeper?? I use this key for Altar Entrance
                if (door.getDoorName().startsWith("Altar_Entrance")) {
                    if (openChance > 0 && Rnd.get(100) < openChance) {
                        activeChar.sendMessage("You opened Altar Entrance.");
                        door.openMe();
                        door.onOpen();
                        activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
                    } else {
                        activeChar.sendMessage("You failed to open Altar Entrance.");
                        activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
                        PlaySound playSound = new PlaySound("interfacesound.system_close_01");
                        activeChar.sendPacket(playSound);
                    }
                } else {
                    activeChar.sendMessage("Incorrect Door.");
                }
                break;
            case 8275: //Key of Darkness
                if (door.getDoorName().startsWith("Door_of_Darkness")) {
                    if (openChance > 0 && Rnd.get(100) < openChance) {
                        activeChar.sendMessage("You opened Door of Darkness.");
                        door.openMe();
                        door.onOpen();
                        activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
                    } else {
                        activeChar.sendMessage("You failed to open Door of Darkness.");
                        activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
                        PlaySound playSound = new PlaySound("interfacesound.system_close_01");
                        activeChar.sendPacket(playSound);
                    }
                } else {
                    activeChar.sendMessage("Incorrect Door.");
                }
                break;
        }
    }

    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
