package ru.agecold.gameserver.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.datatables.ItemTable;
import ru.agecold.gameserver.idfactory.IdFactory;
import ru.agecold.gameserver.lib.Log;
import ru.agecold.gameserver.model.L2Augmentation;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.ItemList;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.L2Item;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;

public class GiveItem {

    public static boolean insertItem(Connect con, int char_id, int item_id, int item_count, int item_ench, int aug_attr, int aug_skill, int aug_lvl, String inv_loc, int first_owner_id)
    {
        L2PcInstance player = L2World.getInstance().getPlayer(char_id);
        if(player == null)
        {
            return insertOffline(con, char_id, item_id, item_count, item_ench, aug_skill, aug_lvl, inv_loc, first_owner_id);
        }

        L2Item _item = ItemTable.getInstance().getTemplate(item_id);

        if(_item.isStackable())
        {
            long overflow = needFowardAdena(player.getItemCount(item_id), item_count);
            if(overflow > 1)
            {
                sendLetter(con, player.getObjectId(), 57, (int) overflow, ItemTable.getInstance().getItemName(item_id));
                player.sendCritMessage("Адена с аукциона перенеправлена на почту! Превышен лимит в инвентаре!");
                item_count = item_count - (int) overflow;
            }
            /*if (item_count > 1) {
                player.addAdena("GiveItem", item_count, player, true);
            }
            return true;*/
        }
        if(player.getActiveTradeList() != null)
        {
            player.cancelActiveTrade();
        }
        if(player.getActiveWarehouse() != null)
        {
            player.cancelActiveWarehouse();
        }
        L2ItemInstance reward = player.getInventory().addItem("external", item_id, item_count, player, player.getTarget());
        if(reward == null)
        {
            return false;
        }

        if(item_ench > 0)
        {
            reward.setEnchantLevel(item_ench);
        }

        if(aug_attr > 0)
        {
            reward.setAugmentation(new L2Augmentation(reward, aug_attr, aug_skill, aug_lvl, true));
        }

        SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
        smsg.addItemName(item_id);
        smsg.addNumber(item_count);
        player.sendPacket(smsg);
        player.sendPacket(new ItemList(player, true));
        Log.add("[Online] Player: " + char_id + "; itemId: " + item_id + "(" + reward.getObjectId() + "); itemCount: " + item_count + "; itemEnch" + item_ench + "; augAttr: " + aug_attr + "; augSkill: " + aug_skill + "; augLvl: " + aug_lvl + "; inv: " + inv_loc + "; firstOwner: " + first_owner_id, "give_items/GiveItem");

        //if(Config.notifyGiveItem(item_id, item_count))
        //{
        //    Log.add("[Online] Player: " + char_id + "; itemId: " + item_id + "(" + reward.getObjectId() + "); itemCount: " + item_count + "; itemEnch" + item_ench + "; augAttr: " + aug_attr + "; augSkill: " + aug_skill + "; augLvl: " + aug_lvl + "; inv: " + inv_loc + "; firstOwner: " + first_owner_id, "warnings/giveItems_GiveItem_" + item_id);
        //}
        return true;
    }

    public static long needFowardAdena(long current, int next)
    {
        if(current >= 2147000000)
        {
            return next;
        }

        if(current + next >= 2147000000)
        {
            return ((current + next) - 2147000000);
        }

        return 0;
    }

    public static void sendLetter(Connect con, int charId, int item_id, int item_count, String item_name)
    {
        sendLetter(con, charId, item_id, item_count, item_name, 0);
    }

    public static void sendLetter(Connect con, int charId, int item_id, int item_count, String item_name, int item_ench)
    {
        boolean ext = false;
        PreparedStatement st = null;
        ResultSet rs = null;
        try
        {
            if(con == null)
            {
                ext = true;
                con = L2DatabaseFactory.get();
            }

            st = con.prepareStatement("INSERT INTO items (owner_id,object_id,item_id,count,enchant_level,loc,loc_data,price_sell,price_buy,custom_type1,custom_type2,mana_left) VALUES (?,?,?,?,?,?,0,0,0,0,0,-1)");
            st.setInt(1, charId);
            st.setInt(2, IdFactory.getInstance().getNextId());
            st.setInt(3, item_id);
            st.setInt(4, item_count);
            st.setInt(5, item_ench);
            st.setInt(6, 0);
            st.execute();
        }
        catch(final SQLException e)
        {
            System.out.println("[ERROR] GiveItem, sendLetter() error: " + e);
        }
        finally
        {
            try
            {
                if(st != null)
                {
                    st.close();
                }
                if(rs != null)
                {
                    rs.close();
                }
                if(ext)
                {
                    if(con != null)
                    {
                        con.close();
                    }
                }
            }
            catch(final SQLException ignored)
            {
            }
        }
    }

    public static boolean insertOffline(Connect con, int char_id, int item_id, int item_count, int item_ench, int aug_id, int aug_lvl, String inv_loc)
    {
        return insertOffline(con, char_id, item_id, item_count, item_ench, aug_id, aug_lvl, inv_loc, 0);
    }

    public static boolean insertOffline(Connect con, int char_id, int item_id, int item_count, int item_ench, int aug_id, int aug_lvl, String inv_loc, int first_owner_id)
    {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            if (item_count > 1) {
                st = con.prepareStatement("SELECT object_id,count FROM items WHERE `owner_id`=? AND `item_id`=? AND `loc`=? LIMIT 1");
                st.setInt(1, char_id);
                st.setInt(2, item_id);
                st.setString(3, inv_loc);
                rs = st.executeQuery();
                if (rs.next()) {
                    int obj = rs.getInt("object_id");
                    int count = rs.getInt("count");
                    Close.SR(st, rs);

                    st = con.prepareStatement("UPDATE items SET `count`=? WHERE `object_id`=? AND `loc`=?");
                    st.setInt(1, (count + item_count));
                    st.setInt(2, obj);
                    st.setString(3, inv_loc);
                    st.execute();
                    return true;
                } else {
                    st = con.prepareStatement("INSERT INTO items (owner_id,object_id,item_id,count,enchant_level,loc,loc_data,price_sell,price_buy,custom_type1,custom_type2,mana_left) VALUES (?,?,?,?,0,?,0,0,0,0,0,-1)");
                    st.setInt(1, char_id);
                    st.setInt(2, IdFactory.getInstance().getNextId());
                    st.setInt(3, item_id);
                    st.setInt(4, item_count);
                    st.setString(5, inv_loc);
                    st.execute();
                    return true;
                }
            } else {
                st = con.prepareStatement("INSERT INTO items (owner_id,object_id,item_id,count,enchant_level,loc,loc_data,price_sell,price_buy,custom_type1,custom_type2,mana_left) VALUES (?,?,?,?,?,?,0,0,0,0,0,-1)");
                st.setInt(1, char_id);
                st.setInt(2, IdFactory.getInstance().getNextId());
                st.setInt(3, item_id);
                st.setInt(4, item_count);
                st.setInt(5, item_ench);
                st.setString(6, inv_loc);
                st.execute();
                return true;
            }
        } catch (final SQLException e) {
            System.out.println("[ERROR] GiveItem, insertOffline() error: " + e);
        } finally {
            Close.SR(st, rs);
        }
        return false;
    }
}
