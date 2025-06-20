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
package ru.agecold.gameserver.model.actor.instance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.idfactory.IdFactory;
import ru.agecold.gameserver.instancemanager.CursedWeaponsManager;
import ru.agecold.gameserver.instancemanager.ItemsOnGroundManager;
import ru.agecold.gameserver.model.Inventory;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2PetData;
import ru.agecold.gameserver.model.L2PetDataTable;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2Summon;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.PcInventory;
import ru.agecold.gameserver.model.PetInventory;
import ru.agecold.gameserver.model.actor.stat.PetStat;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.InventoryUpdate;
import ru.agecold.gameserver.network.serverpackets.MyTargetSelected;
import ru.agecold.gameserver.network.serverpackets.PetInventoryUpdate;
import ru.agecold.gameserver.network.serverpackets.PetItemList;
import ru.agecold.gameserver.network.serverpackets.PetStatusShow;
import ru.agecold.gameserver.network.serverpackets.StatusUpdate;
import ru.agecold.gameserver.network.serverpackets.StopMove;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.taskmanager.DecayTaskManager;
import ru.agecold.gameserver.templates.L2Item;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.gameserver.templates.L2Weapon;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;

/**
 *
 * This class ...
 *
 * @version $Revision: 1.15.2.10.2.16 $ $Date: 2005/04/06 16:13:40 $
 */
public class L2PetInstance extends L2Summon {

    protected static final Logger _logPet = Logger.getLogger(L2PetInstance.class.getName());
    //private byte _pvpFlag;
    private int _curFed;
    private PetInventory _inventory;
    private final int _controlItemId;
    private boolean _respawned;
    private boolean _mountable;
    private Future<?> _feedTask;
    private int _weapon;
    private int _armor;
    private int _jewel;
    private int _feedTime;
    protected boolean _feedMode;
    private L2PetData _data;
    /**
     * The Experience before the last Death Penalty
     */
    private long _expBeforeDeath = 0;
    private static final int FOOD_ITEM_CONSUME_COUNT = 5;

    public final L2PetData getPetData() {
        if (_data == null) {
            _data = L2PetDataTable.getInstance().getPetData(getTemplate().npcId, getStat().getLevel());
        }

        return _data;
    }

    public final void setPetData(L2PetData value) {
        _data = value;
    }

    /**
     * Manage Feeding Task.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR> <li>Feed or kill the pet depending on hunger
     * level</li> <li>If pet has food in inventory and feed level drops below
     * 55% then consume food from inventory</li> <li>Send a
     * broadcastStatusUpdate packet for this L2PetInstance</li><BR><BR>
     *
     */
    class FeedTask implements Runnable {

        public void run() {
            try {
                // if pet is attacking
                if (isAttackingNow()) // if its not already on battleFeed mode
                {
                    if (!_feedMode) {
                        startFeed(true); //switching to battle feed
                    } else // if its on battleFeed mode
                    if (_feedMode) {
                        startFeed(false); // normal feed
                    }
                }
                if (getCurrentFed() > FOOD_ITEM_CONSUME_COUNT) {
                    // eat
                    setCurrentFed(getCurrentFed() - FOOD_ITEM_CONSUME_COUNT);
                } else {
                    // go back to pet control item, or simply said, unsummon it
                    setCurrentFed(0);
                    stopFeed();
                    unSummon(getOwner());
                    getOwner().sendMessage("Your pet is too hungry to stay summoned.");
                }

                int foodId = L2PetDataTable.getFoodItemId(getTemplate().npcId);
                if (foodId == 0) {
                    return;
                }

                L2ItemInstance food = null;
                food = getInventory().getItemByItemId(foodId);

                if ((food != null) && (getCurrentFed() < (0.55 * getMaxFed()))) {
                    if (destroyItem("Feed", food.getObjectId(), 1, null, false)) {
                        setCurrentFed(getCurrentFed() + (100));
                        if (getOwner() != null) {
                            getOwner().sendPacket(SystemMessage.id(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY).addItemName(foodId));
                        }
                    }
                }

                broadcastStatusUpdate();
            } catch (Throwable e) {
                //if (Config.DEBUG)
                //	_logPet.warning("Pet [#"+getObjectId()+"] a feed task error has occurred: "+e);
            }
        }
    }

    public synchronized static L2PetInstance spawnPet(L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control) {
        if (L2World.getInstance().getPet(owner.getObjectId()) != null) {
            return null; // owner has a pet listed in world
        }
        L2PetInstance pet = restore(control, template, owner);
        // add the pet instance to world
        if (pet != null) {
            pet.setTitle(owner.getName());
            L2World.getInstance().addPet(owner.getObjectId(), pet);
        }

        return pet;
    }

    public L2PetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control) {
        super(objectId, template, owner);
        super.setStat(new PetStat(this));

        _controlItemId = control.getObjectId();

        // Pet's initial level is supposed to be read from DB
        // Pets start at :
        // Wolf : Level 15
        // Hatcling : Level 35
        // Tested and confirmed on official servers
        // Sin-eaters are defaulted at the owner's level
        if (template.npcId == 12564) {
            getStat().setLevel((byte) getOwner().getLevel());
        } else {
            getStat().setLevel(template.level);
        }

        _inventory = new PetInventory(this);

        int npcId = template.npcId;
        _mountable = L2PetDataTable.isMountable(npcId);
    }

    @Override
    public PetStat getStat() {
        if (super.getStat() == null || !(super.getStat() instanceof PetStat)) {
            setStat(new PetStat(this));
        }
        return (PetStat) super.getStat();
    }

    @Override
    public double getLevelMod() {
        return (100.0 - 11 + getLevel()) / 100.0;
    }

    public boolean isRespawned() {
        return _respawned;
    }

    @Override
    public int getSummonType() {
        return 2;
    }

    @Override
    public void onAction(L2PcInstance player) {
        if (isAgathion()) {
            player.sendActionFailed();
            return;
        }
        boolean isOwner = player.getObjectId() == getOwner().getObjectId();

        //player.sendPacket(new ValidateLocation(this));

        if (isOwner && player != getOwner()) {
            updateRefOwner(player);
        }

        if (this != player.getTarget()) {
            // Set the target of the L2PcInstance player
            player.setTarget(this);
            player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

            // Send a Server->Client packet StatusUpdate of the L2PetInstance to the L2PcInstance to update its HP bar
            StatusUpdate su = new StatusUpdate(getObjectId());
            su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
            su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
            player.sendPacket(su);
        } else {
            if (isOwner && player != getOwner()) {
                // update owner
                updateRefOwner(player);
            }
            player.sendPacket(new PetStatusShow(this));
        }
        player.sendActionFailed();
    }

    @Override
    public int getControlItemId() {
        return _controlItemId;
    }

    public L2ItemInstance getControlItem() {
        return getOwner().getInventory().getItemByObjectId(_controlItemId);
    }

    public int getCurrentFed() {
        if (Config.DISABLE_PET_FEED) {
            return getMaxFed();
        }
        return _curFed;
    }

    public void setCurrentFed(int num) {
        if (Config.DISABLE_PET_FEED) {
            _curFed = getMaxFed();
            return;
        }
        //_curFed = num > getMaxFed() ? getMaxFed() : num;
        _curFed = Math.min(num, getMaxFed());
    }

    //public void setPvpFlag(byte pvpFlag) { _pvpFlag = pvpFlag; }
    @Override
    public void setPkKills(int pkKills) {
        _pkKills = pkKills;
    }

    /**
     * Returns the pet's currently equipped weapon instance (if any).
     */
    @Override
    public L2ItemInstance getActiveWeaponInstance() {
        for (L2ItemInstance item : getInventory().getItems()) {
            if (item.getLocation() == L2ItemInstance.ItemLocation.PET_EQUIP
                    && item.getItem().getType1() == L2Item.TYPE2_WEAPON) {
                return item;
            }
        }

        return null;
    }

    /**
     * Returns the pet's currently equipped weapon (if any).
     */
    @Override
    public L2Weapon getActiveWeaponItem() {
        L2ItemInstance weapon = getActiveWeaponInstance();

        if (weapon == null) {
            return null;
        }

        return (L2Weapon) weapon.getItem();
    }

    @Override
    public L2ItemInstance getSecondaryWeaponInstance() {
        // temporary? unavailable
        return null;
    }

    @Override
    public L2Weapon getSecondaryWeaponItem() {
        // temporary? unavailable
        return null;
    }

    @Override
    public PetInventory getInventory() {
        return _inventory;
    }

    /**
     * Destroys item from inventory and send a Server->Client InventoryUpdate
     * packet to the L2PcInstance.
     *
     * @param process : String Identifier of process triggering this action
     * @param objectId : int Item Instance identifier of the item to be
     * destroyed
     * @param count : int Quantity of items to be destroyed
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     * about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);

        if (item == null) {
            if (sendMessage) {
                getOwner().sendPacket(Static.NOT_ENOUGH_ITEMS);
            }

            return false;
        }

        // Send Pet inventory update packet
        PetInventoryUpdate petIU = new PetInventoryUpdate();
        petIU.addItem(item);
        getOwner().sendPacket(petIU);

        if (sendMessage) {
            getOwner().sendPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addNumber(count).addItemName(item.getItemId()));
        }
        return true;
    }

    /**
     * Destroy item from inventory by using its <B>itemId</B> and send a
     * Server->Client InventoryUpdate packet to the L2PcInstance.
     *
     * @param process : String Identifier of process triggering this action
     * @param itemId : int Item identifier of the item to be destroyed
     * @param count : int Quantity of items to be destroyed
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client
     * about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);

        if (item == null) {
            if (sendMessage) {
                getOwner().sendPacket(Static.NOT_ENOUGH_ITEMS);
            }
            return false;
        }

        // Send Pet inventory update packet
        PetInventoryUpdate petIU = new PetInventoryUpdate();
        petIU.addItem(item);
        getOwner().sendPacket(petIU);

        if (sendMessage) {
            getOwner().sendPacket(SystemMessage.id(SystemMessageId.DISSAPEARED_ITEM).addNumber(count).addItemName(itemId));
        }
        return true;
    }

    public final void setWeapon(int id) {
        _weapon = id;
    }

    public final void setArmor(int id) {
        _armor = id;
    }

    public final void setJewel(int id) {
        _jewel = id;
    }

    @Override
    protected void doPickupItem(L2Object object) {
        if (isDead()) {
            return;
        }

        getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        broadcastPacket(new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading()));

        if (!(object.isL2Item())) {
            // dont try to pickup anything that is not an item :)
            _logPet.warning("trying to pickup wrong target." + object);
            getOwner().sendActionFailed();
            return;
        }

        L2ItemInstance target = (L2ItemInstance) object;

        // Herbs
        if (target.getItemId() > 8599 && target.getItemId() < 8615) {
            getOwner().sendPacket(SystemMessage.id(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
            return;
        }
        // Cursed weapons
        if (CursedWeaponsManager.getInstance().isCursed(target.getItemId())) {
            getOwner().sendPacket(SystemMessage.id(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
            return;
        }

        synchronized (target) {
            if (!target.isVisible()) {
                getOwner().sendActionFailed();
                return;
            }

            if (target.getOwnerId() != 0 && target.getOwnerId() != getOwner().getObjectId() && !getOwner().isInLooterParty(target.getOwnerId())) {
                getOwner().sendActionFailed();

                SystemMessage sm = null;
                if (target.getItemId() == 57) {
                    sm = SystemMessage.id(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA).addNumber(target.getCount());
                } else if (target.getCount() > 1) {
                    sm = SystemMessage.id(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S).addItemName(target.getItemId()).addNumber(target.getCount());
                } else {
                    sm = SystemMessage.id(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId());
                }

                getOwner().sendPacket(sm);
                sm = null;
                return;
            }
            if (target.getItemLootShedule() != null
                    && (target.getOwnerId() == getOwner().getObjectId() || getOwner().isInLooterParty(target.getOwnerId()))) {
                target.resetOwnerTimer();
            }

            target.pickupMe(this);

            if (Config.SAVE_DROPPED_ITEM) // item must be removed from ItemsOnGroundManager if is active
            {
                ItemsOnGroundManager.getInstance().removeObject(target);
            }
        }

        getInventory().addItem("Pickup", target, getOwner(), this);
        getOwner().sendPacket(new PetItemList(this));

        getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

        if (getFollowStatus()) {
            followOwner();
        }
    }

    @Override
    public void deleteMe(L2PcInstance owner) {
        super.deleteMe(owner);
        destroyControlItem(owner); //this should also delete the pet from the db
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer, true)) {
            return false;
        }
        stopFeed();
        getOwner().sendPacket(Static.MAKE_SURE_YOU_RESSURECT_YOUR_PET_WITHIN_24_HOURS);
        DecayTaskManager.getInstance().addDecayTask(this, 1200000);
        deathPenalty();
        return true;
    }

    @Override
    public void doRevive() {
        if (_curFed > (getMaxFed() / 10)) {
            _curFed = getMaxFed() / 10;
        }

        getOwner().removeReviving();

        super.doRevive();

        // stopDecay
        DecayTaskManager.getInstance().cancelDecayTask(this);
        startFeed(false);
    }

    @Override
    public void doRevive(double revivePower) {
        // Restore the pet's lost experience,
        // depending on the % return of the skill used (based on its power).
        restoreExp(revivePower);
        doRevive();
    }

    /**
     * Transfers item to another inventory
     *
     * @param process : String Identifier of process triggering this action
     * @param itemId : int Item Identifier of the item to be transfered
     * @param count : int Quantity of items to be transfered
     * @param actor : L2PcInstance Player requesting the item transfer
     * @param reference : L2Object Object referencing current action like NPC
     * selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new item or the updated item
     * in inventory
     */
    public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2PcInstance actor, L2Object reference) {
        //L2ItemInstance oldItem = getInventory().getItemByObjectId(objectId);
        L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, actor, reference);

        if (newItem == null) {
            return null;
        }

        // Send inventory update packet
		/*
         * PetInventoryUpdate petIU = new PetInventoryUpdate(); if
         * (oldItem.getCount() > 0 && oldItem != newItem)
         * petIU.addModifiedItem(oldItem); else petIU.addRemovedItem(oldItem);
         * getOwner().sendPacket(petIU);
         */

        getOwner().sendPacket(new PetItemList(this));
        broadcastPetInfo();

        // Send target update packet
        if (target instanceof PcInventory) {
            L2PcInstance targetPlayer = ((PcInventory) target).getOwner();
            InventoryUpdate playerUI = new InventoryUpdate();
            if (newItem.getCount() > count) {
                playerUI.addModifiedItem(newItem);
            } else {
                playerUI.addNewItem(newItem);
            }
            targetPlayer.sendPacket(playerUI);

            // Update current load as well
            StatusUpdate playerSU = new StatusUpdate(targetPlayer.getObjectId());
            playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
            targetPlayer.sendPacket(playerSU);
        } else if (target instanceof PetInventory) {
            /*
             * petIU = new PetInventoryUpdate(); if (newItem.getCount() > count)
             * petIU.addRemovedItem(newItem); else petIU.addNewItem(newItem);
             * ((PetInventory)target).getOwner().getOwner().sendPacket(petIU);
             */
            ((PetInventory) target).getOwner().getOwner().sendPacket(new PetItemList(this));
            broadcastPetInfo();
        }
        return newItem;
    }

    @Override
    public void giveAllToOwner() {
        try {
            Inventory petInventory = getInventory();
            L2ItemInstance[] items = petInventory.getItems();
            for (int i = 0; (i < items.length); i++) {
                L2ItemInstance giveit = items[i];
                if (((giveit.getItem().getWeight() * giveit.getCount())
                        + getOwner().getInventory().getTotalWeight())
                        < getOwner().getMaxLoad()) {
                    // If the owner can carry it give it to them
                    giveItemToOwner(giveit);
                } else {
                    // If they can't carry it, chuck it on the floor :)
                    dropItemHere(giveit);
                }
            }
        } catch (Exception e) {
            _logPet.warning("Give all items error " + e);
        }
    }

    public void giveItemToOwner(L2ItemInstance item) {
        try {
            getInventory().transferItem("PetTransfer", item.getObjectId(), item.getCount(), getOwner().getInventory(), getOwner(), this);
            PetInventoryUpdate petiu = new PetInventoryUpdate();
            petiu.addRemovedItem(item);
            getOwner().sendPacket(petiu);
            getOwner().sendItems(false);
            getOwner().sendChanges();
        } catch (Exception e) {
            _logPet.warning("Error while giving item to owner: " + e);
        }
    }

    /**
     * Remove the Pet from DB and its associated item from the player inventory
     *
     * @param owner The owner from whose invenory we should delete the item
     */
    public void destroyControlItem(L2PcInstance owner) {
        // remove the pet instance from world
        L2World.getInstance().removePet(owner.getObjectId());

        // delete from inventory
        try {
            L2ItemInstance removedItem = owner.getInventory().destroyItem("PetDestroy", getControlItemId(), 1, getOwner(), this);

            InventoryUpdate iu = new InventoryUpdate();
            iu.addRemovedItem(removedItem);

            owner.sendPacket(iu);

            //StatusUpdate su = new StatusUpdate(owner.getObjectId());
            //su.addAttribute(StatusUpdate.CUR_LOAD, owner.getCurrentLoad());
            //owner.sendPacket(su);

            //owner.broadcastUserInfo();
            owner.sendChanges();

            L2World world = L2World.getInstance();
            world.removeObject(removedItem);
        } catch (Exception e) {
            _logPet.warning("Error while destroying control item: " + e);
        }

        // pet control item no longer exists, delete the pet from the db
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
            statement.setInt(1, getControlItemId());
            statement.execute();
        } catch (Exception e) {
            _logPet.warning("could not delete pet:" + e);
        } finally {
            Close.CS(con, statement);
        }
    }

    public void dropAllItems() {
        try {
            L2ItemInstance[] items = getInventory().getItems();
            for (int i = 0; (i < items.length); i++) {
                dropItemHere(items[i]);
            }
        } catch (Exception e) {
            _logPet.warning("Pet Drop Error: " + e);
        }
    }

    public void dropItemHere(L2ItemInstance dropit) {
        dropit = getInventory().dropItem("Drop", dropit.getObjectId(), dropit.getCount(), getOwner(), this);

        if (dropit != null) {
            _logPet.finer("Item id to drop: " + dropit.getItemId() + " amount: " + dropit.getCount());
            dropit.dropMe(this, getX(), getY(), getZ() + 100);
        }
    }

//	public void startAttack(L2Character target)
//	{
//		if (!knownsObject(target))
//		{
//			target.addKnownObject(this);
//			this.addKnownObject(target);
//		}
//        if (!target.knownsObject(this))
//        {
//            target.addKnownObject(this);
//            this.addKnownObject(target);
//        }
//
//		if (!isRunning())
//		{
//			setRunning(true);
//			ChangeMoveType move = new ChangeMoveType(this, ChangeMoveType.RUN);
//			broadcastPacket(move);
//		}
//
//		super.startAttack(target);
//	}
//
    /**
     * @return Returns the mountable.
     */
    @Override
    public boolean isMountable() {
        return _mountable;
    }

    private static L2PetInstance restore(L2ItemInstance control, L2NpcTemplate template, L2PcInstance owner) {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rset = null;
        try {
            L2PetInstance pet;
            if (template.type.compareToIgnoreCase("L2BabyPet") == 0) {
                pet = new L2BabyPetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
            } else {
                pet = new L2PetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
            }

            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, karma, pkkills, fed FROM pets WHERE item_obj_id=?");
            statement.setInt(1, control.getObjectId());
            rset = statement.executeQuery();
            if (!rset.next()) {
                Close.SR(statement, rset);
                return pet;
            }

            pet._respawned = true;
            pet.setName(rset.getString("name"));

            pet.getStat().setLevel(rset.getByte("level"));
            pet.getStat().setExp(rset.getLong("exp"));
            pet.getStat().setSp(rset.getInt("sp"));

            pet.getStatus().setCurrentHp(rset.getDouble("curHp"));
            pet.getStatus().setCurrentMp(rset.getDouble("curMp"));
            pet.getStatus().setCurrentCp(pet.getMaxCp());

            pet.setKarma(rset.getInt("karma"));
            pet.setPkKills(rset.getInt("pkkills"));
            pet.setCurrentFed(rset.getInt("fed"));

            return pet;
        } catch (Exception e) {
            _logPet.warning("could not restore pet data: " + e);
            return null;
        } finally {
            Close.CSR(con, statement, rset);
        }
    }

    @Override
    public void store() {
        if (getControlItemId() == 0) {
            // this is a summon, not a pet, don't store anything
            return;
        }

        String req;
        if (!isRespawned()) {
            req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,karma,pkkills,fed,item_obj_id) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        } else {
            req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,karma=?,pkkills=?,fed=? "
                    + "WHERE item_obj_id = ?";
        }

        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement(req);
            statement.setString(1, getName());
            statement.setInt(2, getStat().getLevel());
            statement.setDouble(3, getStatus().getCurrentHp());
            statement.setDouble(4, getStatus().getCurrentMp());
            statement.setLong(5, getStat().getExp());
            statement.setInt(6, getStat().getSp());
            statement.setInt(7, getKarma());
            statement.setInt(8, getPkKills());
            statement.setInt(9, getCurrentFed());
            statement.setInt(10, getControlItemId());
            statement.executeUpdate();
            _respawned = true;
        } catch (Exception e) {
            _logPet.warning("could not store pet data: " + e);
        } finally {
            Close.CS(con, statement);
        }

        L2ItemInstance itemInst = getControlItem();
        if (itemInst != null && itemInst.getEnchantLevel() != getStat().getLevel()) {
            itemInst.setEnchantLevel(getStat().getLevel());
            itemInst.updateDatabase();
        }
    }

    public void stopFeed() {
        if (_feedTask != null) {
            _feedTask.cancel(false);
            _feedTask = null;
            //if (Config.DEBUG) _logPet.fine("Pet [#"+getObjectId()+"] feed task stop");
        }
    }

    public void startFeed(boolean battleFeed) {
        // stop feeding task if its active
        if (Config.DISABLE_PET_FEED || getTemplate().npcId == 99997) {
            return;
        }

        stopFeed();
        if (!isDead()) {
            if (battleFeed) {
                _feedMode = true;
                _feedTime = _data.getPetFeedBattle();
            } else {
                _feedMode = false;
                _feedTime = _data.getPetFeedNormal();
            }
            //  pet feed time must be different than 0. Changing time to bypass divide by 0
            if (_feedTime <= 0) {
                _feedTime = 1;
            }

            _feedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 60000 / _feedTime, 60000 / _feedTime);
        }
    }

    @Override
    public synchronized void unSummon(L2PcInstance owner) {
        stopFeed();
        stopHpMpRegeneration();
        super.unSummon(owner);

        if (!isDead()) {
            L2World.getInstance().removePet(owner.getObjectId());
        }
    }

    /**
     * Restore the specified % of experience this L2PetInstance has
     * lost.<BR><BR>
     */
    public void restoreExp(double restorePercent) {
        if (_expBeforeDeath > 0) {
            // Restore the specified % of lost experience.
            getStat().addExp(Math.round((_expBeforeDeath - getStat().getExp()) * restorePercent / 100));
            _expBeforeDeath = 0;
        }
        //sendChanges();
    }

    private void deathPenalty() {
        // TODO Need Correct Penalty

        int lvl = getStat().getLevel();
        double percentLost = -0.07 * lvl + 6.5;

        // Calculate the Experience loss
        long lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);

        // Get the Experience before applying penalty
        _expBeforeDeath = getStat().getExp();

        // Set the new Experience value of the L2PetInstance
        getStat().addExp(-lostExp);
    }

    @Override
    public void addExpAndSp(long addToExp, int addToSp) {
        if (getNpcId() == 12564) //SinEater
        {
            getStat().addExpAndSp(Math.round(addToExp * Config.SINEATER_XP_RATE), addToSp);
        } else {
            getStat().addExpAndSp(Math.round(addToExp * Config.PET_XP_RATE), addToSp);
        }
    }

    @Override
    public long getExpForThisLevel() {
        return getStat().getExpForLevel(getLevel());
    }

    @Override
    public long getExpForNextLevel() {
        return getStat().getExpForLevel(getLevel() + 1);
    }

    @Override
    public final int getLevel() {
        return getStat().getLevel();
    }

    public int getMaxFed() {
        return getStat().getMaxFeed();
    }

    @Override
    public int getAccuracy() {
        return getStat().getAccuracy();
    }

    @Override
    public int getCriticalHit(L2Character target, L2Skill skill) {
        return getStat().getCriticalHit(target, skill);
    }

    @Override
    public int getEvasionRate(L2Character target) {
        return getStat().getEvasionRate(target);
    }

    @Override
    public int getRunSpeed() {
        return getStat().getRunSpeed();
    }

    @Override
    public int getPAtkSpd() {
        return getStat().getPAtkSpd();
    }

    @Override
    public int getMAtkSpd() {
        return getStat().getMAtkSpd();
    }

    @Override
    public int getMAtk(L2Character target, L2Skill skill) {
        return getStat().getMAtk(target, skill);
    }

    @Override
    public int getMDef(L2Character target, L2Skill skill) {
        return getStat().getMDef(target, skill);
    }

    @Override
    public int getPAtk(L2Character target) {
        return getStat().getPAtk(target);
    }

    @Override
    public int getPDef(L2Character target) {
        return getStat().getPDef(target);
    }

    @Override
    public final int getSkillLevel(int skillId) {
        if (_skills == null || _skills.get(skillId) == null) {
            return -1;
        }
        int lvl = getLevel();
        return lvl > 70 ? 7 + (lvl - 70) / 5 : lvl / 10;
    }

    public void updateRefOwner(L2PcInstance owner) {
        int oldOwnerId = getOwner().getObjectId();

        setOwner(owner);
        L2World.getInstance().removePet(oldOwnerId);
        L2World.getInstance().addPet(oldOwnerId, this);
    }

    @Override
    public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
        if (miss) {
            return;
        }

        // Prevents the double spam of system messages, if the target is the owning player.
        if (target.getObjectId() != getOwner().getObjectId()) {
            if (pcrit || mcrit) {
                getOwner().sendPacket(Static.CRITICAL_HIT_BY_PET);
            }

            getOwner().sendPacket(SystemMessage.id(SystemMessageId.PET_HIT_FOR_S1_DAMAGE).addNumber(damage));
        }
    }

    @Override
    public boolean isPet() {
        return true;
    }
}
