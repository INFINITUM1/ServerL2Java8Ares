package ru.agecold.gameserver.network.clientpackets;

import ru.agecold.Config;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.Inventory;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.EnchantResult;
import ru.agecold.gameserver.network.serverpackets.ShowCalculator;
import ru.agecold.gameserver.templates.L2ArmorType;
import ru.agecold.gameserver.templates.L2Weapon;
import ru.agecold.gameserver.templates.L2WeaponType;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.L2GameClient;
import ru.agecold.gameserver.model.base.ClassId;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import scripts.items.IItemHandler;
import scripts.items.ItemHandler;
import ru.agecold.gameserver.model.actor.appearance.PcAppearance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

public final class UseItem extends L2GameClientPacket {

    private int _objectId;

    @Override
    protected void readImpl() {
        _objectId = readD();
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
        if (item == null) {
            return;
        }

        if (item.getItemId() == 57 || player.isParalyzed()) {
            return;
        }
        int itemId = item.getItemId();

        //if((itemId != 5591 && itemId != 5592) && (System.currentTimeMillis() - player.gCPBA() < 200))
        //	return;
        if (!item.isEquipable()) {
            if (System.currentTimeMillis() - player.gCPBA() < 200) {
                return;
            }
            player.sCPBA();
        }

        if (player.isStunned() || player.isSleeping() || player.isAfraid() || player.isFakeDeath()) {
            return;
        }

        if (player.isDead() || player.isAlikeDead()) {
            return;
        }

        if (player.getPrivateStoreType() != 0) {
            player.sendPacket(Static.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
            return;
        }

        if (player.getActiveTradeList() != null) {
            player.cancelActiveTrade();
            return;
        }

        if (player.getActiveWarehouse() != null) {
            player.cancelActiveWarehouse();
            return;
        }

        if (player.getActiveEnchantItem() != null) {
            player.setActiveEnchantItem(null);
            player.sendPacket(new EnchantResult(0, true));
            return;
        }

        synchronized (player.getInventory()) {
            // Items that cannot be used

            if (player.isFishing() && (itemId < 6535 || itemId > 6540)) {
                // You cannot do anything else while fishing
                player.sendPacket(Static.CANNOT_DO_WHILE_FISHING_3);
                player.sendActionFailed();
                return;
            }

            // Char cannot use pet items
            if (item.getItem().isForWolf() || item.getItem().isForHatchling() || item.getItem().isForStrider() || item.getItem().isForBabyPet()) {
                player.sendPacket(Static.CANNOT_EQUIP_PET_ITEM);
                player.sendActionFailed();
                return;
            }

            if (item.isEquipable()) {
                //int bodyPart = item.getItem().getBodyPart();
                //System.out.println(bodyPart);

                // Don't allow weapon/shield hero equipment during Olympiads
                if ((player.isInOlympiadMode() || (Config.FORBIDDEN_EVENT_ITMES && player.isInEventChannel())) && (item.isHeroItem() || item.notForOly())) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (player.getChannel() == 8 && Config.TVT_CUSTOM_ITEMS) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (player.getChannel() == 67 && item.notForBossZone()) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (item.getItem().isHippy() && player.underAttack()) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (player.isForbidItem(itemId)) {
                    player.sendPacket(Static.RES_DISABLED);
                    player.sendActionFailed();
                    return;
                }

                if (Config.PREMIUM_ITEMS && item.isPremiumItem() && !player.isPremium()) {
                    player.sendPacket(Static.PREMIUM_ONLY);
                    player.sendActionFailed();
                    return;

                }

                if (item.getItem().isWeapon()) {
                    if (player.isCursedWeaponEquiped() || itemId == 6408
                    || item.isExpired()) // Don't allow to put formal wear
                    {
                        player.sendActionFailed();
                        return;
                    }
                    player.equipWeapon(item);
                    return;
                }

                player.useEquippableItem(item, true);
                return;
            }

            if (itemId == 4393) {
                player.sendPacket(new ShowCalculator(4393));
                return;
            }

            if (forbidWeapon(player, player.getActiveWeaponItem(), item)) {
                return;
            }

            IItemHandler handler = ItemHandler.getInstance().getItemHandler(itemId);
            if (handler != null) {
                handler.useItem(player, item);
            }
        }
    }

    private boolean forbidWeapon(L2PcInstance player, L2Weapon weaponItem, L2ItemInstance item) {
        if (weaponItem == null) {
            return false;
        }

        if (item.isLure() && weaponItem.getItemType() == L2WeaponType.ROD) {
            player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
            player.sendItems(false);
            player.broadcastUserInfo();
            return true;
        }

        if (item.getItemId() == 8192 && weaponItem.getItemType() != L2WeaponType.BOW) {
            player.sendMessage("Используется с луком");
            return true;
        }
        return false;
    }
}
