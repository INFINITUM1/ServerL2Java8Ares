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
package ru.agecold.gameserver.network.clientpackets;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.TradeController;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.ItemTable;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2TradeList;
import ru.agecold.gameserver.model.actor.instance.L2MercManagerInstance;
import ru.agecold.gameserver.model.actor.instance.L2MerchantInstance;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.ActionFailed;
import ru.agecold.gameserver.network.serverpackets.InventoryUpdate;
import ru.agecold.gameserver.network.serverpackets.StatusUpdate;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.L2Item;
import ru.agecold.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.12.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestWearItem extends L2GameClientPacket {

    protected static final Logger _log = Logger.getLogger(RequestWearItem.class.getName());
    protected Future<?> _removeWearItemsTask;
    @SuppressWarnings("unused")
    private int _unknow;
    /** List of ItemID to Wear */
    private int _listId;
    /** Number of Item to Wear */
    private int _count;
    /** Table of ItemId containing all Item to Wear */
    private int[] _items;
    /** Player that request a Try on */
    protected L2PcInstance _activeChar;

    class RemoveWearItemsTask implements Runnable {

        public void run() {
            try {
                _activeChar.destroyWearedItems("Wear", null, true);

            } catch (Throwable e) {
                _log.log(Level.SEVERE, "", e);
            }
        }
    }

    /**
     * Decrypt the RequestWearItem Client->Server Packet and Create _items table containing all ItemID to Wear.<BR><BR>
     *
     */
    @Override
    protected void readImpl() {
        // Read and Decrypt the RequestWearItem Client->Server Packet
        _activeChar = getClient().getActiveChar();
        _unknow = readD();
        _listId = readD(); // List of ItemID to Wear
        _count = readD();  // Number of Item to Wear

        if (_count < 0) {
            _count = 0;
        }
        if (_count > 100) {
            _count = 0; // prevent too long lists
        }
        // Create _items table that will contain all ItemID to Wear
        _items = new int[_count];

        // Fill _items table with all ItemID to Wear
        for (int i = 0; i < _count; i++) {
            int itemId = readD();
            _items[i] = itemId;
        }
    }

    /**
     * Launch Wear action.<BR><BR>
     *
     */
    @Override
    protected void runImpl() {
        // Get the current player and return if null
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        // If Alternate rule Karma punishment is set to true, forbid Wear to player with Karma
        if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0) {
            return;
        }

        // Check current target of the player and the INTERACTION_DISTANCE
        L2Object target = player.getTarget();
        if (!player.isGM() && (target == null // No target (ie GM Shop)
                || !(target instanceof L2MerchantInstance || target instanceof L2MercManagerInstance) // Target not a merchant and not mercmanager
                || !player.isInsideRadius(target, L2NpcInstance.INTERACTION_DISTANCE, false, false) // Distance is too far
                )) {
            return;
        }

        L2TradeList list = null;

        // Get the current merchant targeted by the player
        L2MerchantInstance merchant = (target != null && target instanceof L2MerchantInstance) ? (L2MerchantInstance) target : null;
        if (merchant == null) {
            //Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
            return;
        }

        List<L2TradeList> lists = TradeController.getInstance().getBuyListByNpcId(merchant.getNpcId());

        if (lists == null) {
            //Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
            return;
        }

        for (L2TradeList tradeList : lists) {
            if (tradeList.getListId() == _listId) {
                list = tradeList;
            }
        }

        if (list == null) {
            //Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
            return;
        }

        _listId = list.getListId();

        // Check if the quantity of Item to Wear
        if (_count < 1 || _listId >= 1000000) {
            player.sendActionFailed();
            return;
        }

        // Total Price of the Try On
        long totalPrice = 0;

        // Check for buylist validity and calculates summary values
        int slots = 0;
        int weight = 0;

        for (int i = 0; i < _count; i++) {
            int itemId = _items[i];

            if (!list.containsItemId(itemId)) {
                //Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
                return;
            }

            L2Item template = ItemTable.getInstance().getTemplate(itemId);
            weight += template.getWeight();
            slots++;

            totalPrice += Config.WEAR_PRICE;
            if (totalPrice > Integer.MAX_VALUE) {
                //Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried to purchase over "+Integer.MAX_VALUE+" adena worth of goods.", Config.DEFAULT_PUNISH);
                return;
            }
        }

        // Check the weight
        if (!player.getInventory().validateWeight(weight)) {
            sendPacket(Static.WEIGHT_LIMIT_EXCEEDED);
            return;
        }

        // Check the inventory capacity
        if (!player.getInventory().validateCapacity(slots)) {
            sendPacket(Static.SLOTS_FULL);
            return;
        }

        // Charge buyer and add tax to castle treasury if not owned by npc clan because a Try On is not Free
        if ((totalPrice < 0) || !player.reduceAdena("Wear", (int) totalPrice, player.getLastFolkNPC(), false)) {
            sendPacket(Static.YOU_NOT_ENOUGH_ADENA);
            return;
        }

        // Proceed the wear
        InventoryUpdate playerIU = new InventoryUpdate();
        for (int i = 0; i < _count; i++) {
            int itemId = _items[i];

            if (!list.containsItemId(itemId)) {
                //Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
                return;
            }

            // If player doesn't own this item : Add this L2ItemInstance to Inventory and set properties lastchanged to ADDED and _wear to True
            // If player already own this item : Return its L2ItemInstance (will not be destroy because property _wear set to False)
            L2ItemInstance item = player.getInventory().addWearItem("Wear", itemId, player, merchant);

            // Equip player with this item (set its location)
            player.getInventory().equipItemAndRecord(item);

            // Add this Item in the InventoryUpdate Server->Client Packet
            playerIU.addItem(item);
        }

        // Send the InventoryUpdate Server->Client Packet to the player
        // Add Items in player inventory and equip them
        player.sendPacket(playerIU);

        // Send the StatusUpdate Server->Client Packet to the player with new CUR_LOAD (0x0e) information
        StatusUpdate su = new StatusUpdate(player.getObjectId());
        su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
        player.sendPacket(su);

        // Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers
        player.broadcastUserInfo();


        // All weared items should be removed in ALLOW_WEAR_DELAY sec.
        if (_removeWearItemsTask == null) {
            _removeWearItemsTask = ThreadPoolManager.getInstance().scheduleGeneral(new RemoveWearItemsTask(), Config.WEAR_DELAY * 1000);
        }

    }

    /* (non-Javadoc)
     * @see ru.agecold.gameserver.clientpackets.ClientBasePacket#getType()
     */
    @Override
    public String getType() {
        return "C.WearItem";
    }
}
