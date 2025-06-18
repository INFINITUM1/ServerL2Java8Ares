package ru.agecold.gameserver.taskmanager;

import java.sql.PreparedStatement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;

public class ExpiredItemsTaskManager {
    protected static final Logger _log = Logger.getLogger(ExpiredItemsTaskManager.class.getName());
    private static final long TICK = TimeUnit.SECONDS.toMillis(30L);
    private static final Map<Integer, ExpireItemData> _expiredItems = new ConcurrentHashMap<Integer, ExpireItemData>();

    public static class ExpireItemData {
        public int owner;
        public long expire;

        public ExpireItemData(int owner, long expire) {
            this.owner = owner;
            this.expire = expire;
        }
    }

    public static void scheduleItem(int owner, int item_id, long expire) {
        _expiredItems.put(item_id, new ExpireItemData(owner, expire));
    }

    private static void checkExpiredItems() {
        long now = System.currentTimeMillis();
        Integer id = null;
        ExpireItemData data = null;

        L2PcInstance player = null;
        L2ItemInstance itemToRemove = null;

        Connect con = null;
        PreparedStatement st = null;
        try {
            con = L2DatabaseFactory.get();
            for (Map.Entry<Integer, ExpireItemData> entry : _expiredItems.entrySet()) {
                id = entry.getKey();
                data = entry.getValue();
                if (id == null || data == null) {
                    continue;
                }

                if(now < data.expire) {
                    continue;
                }

                _expiredItems.remove(id);

                player = L2World.getInstance().getPlayer(data.owner);
                if (player == null) {
                    st = con.prepareStatement("DELETE FROM items WHERE object_id=? AND owner_id=?");
                    st.setInt(1, id);
                    st.setInt(2, data.owner);
                    st.executeUpdate();
                } else {
                    itemToRemove = player.getInventory().getItemByObjectId(id);
                    if (itemToRemove != null)
                    {
                        itemToRemove.setExpire(555);
                        if (itemToRemove.isEquipped()) {
                            player.getInventory().unEquipItemInBodySlotAndRecord(itemToRemove.getItem().getBodyPart());
                            player.abortCast();
                            player.abortAttack();
                        }
                        if (itemToRemove.isAugmented()) {
                            itemToRemove.removeAugmentation();
                        }
                        itemToRemove = player.getInventory().destroyItem("ExpiredItem", id, 1, player, null);
                        if (itemToRemove != null) {
                            player.sendMessage(itemToRemove.getDisplayName() + " истекло.");
                            player.sendItems(false);
                            player.sendChanges();
                            player.broadcastUserInfo();
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.log(Level.WARNING, "ExpireTask.checkExpiredItems: ", e);
        } finally {
            Close.CS(con, st);
        }
    }

    public static void start() {
        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ExpireTask(), TICK, TICK);
    }

    public static class ExpireTask implements Runnable {
        @Override
        public void run() {
            if (_expiredItems.isEmpty()) {
                return;
            }
            checkExpiredItems();
        }
    }
}