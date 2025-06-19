package ru.agecold.gameserver.model.actor.instance;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.cache.HtmCache;
import ru.agecold.gameserver.datatables.AugmentationData;
import ru.agecold.gameserver.datatables.AugmentationData.AugStat;
import ru.agecold.gameserver.datatables.ItemTable;
import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.templates.L2Item;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.gameserver.util.Util;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;
import ru.agecold.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class L2AuctionInstance extends L2FolkInstance {
    public L2AuctionInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }
    //
//    // включить аукцион
//    private static final boolean ALLOW_AUCTION = true;
//
//    private static final int PAGE_LIMIT = 5;
//    private static final int PAGE_LIMIT_TOP = 0;
//    private static final int SORT_LIMIT = 7;
//    private static final int MAX_ADENA = 2147000000;
//
//    // Показ эпик бижы в разделе Бижутерия
//    private static final boolean SHOW_EPIC_DEFAULT = true;
//
//    private static final FastMap<Integer, String> moneys = new FastMap<Integer, String>();
//    private static final String[] sorts = {"Оружие", "Броня", "Бижутерия", "Эпик", "Оружие ЛС", "Всё", "Остальное" };
//    private static final FastMap<String, String> stats_synonims = new FastMap<String, String>();
//
//    static
//    {
//        stats_synonims.put("pDef", "P. Def.");
//        stats_synonims.put("mDef", "M. Def.");
//        stats_synonims.put("maxHp", "Maximum HP");
//        stats_synonims.put("maxMp", "Maximum MP");
//        stats_synonims.put("maxCp", "Maximum CP");
//        stats_synonims.put("pAtk", "P. Atk.");
//        stats_synonims.put("mAtk", "M. Atk.");
//        stats_synonims.put("regHp", "HP Recovery");
//        stats_synonims.put("regMp", "MP Recovery");
//        stats_synonims.put("regCp", "CP Recovery");
//        stats_synonims.put("rEvas", "Dodge");
//        stats_synonims.put("accCombat", "Accuracy");
//        stats_synonims.put("rCrit", "Critical");
//    }
//
//
//    public L2AuctionInstance(int objectId, L2NpcTemplate template) {
//        super(objectId, template);
//    }
//
//    public static boolean canBypassCheck(L2PcInstance player, L2NpcInstance npc)
//    {
//        if(npc == null || player.getTarget() != npc || /*player.isActionsDisabled() || */!player.isInsideRadius(npc, INTERACTION_DISTANCE, false, false))
//        {
//            player.sendActionFailed();
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public void onBypassFeedback(L2PcInstance player, String command)
//    {
//        if(!canBypassCheck(player, this))
//            return;
//
//        if(!ALLOW_AUCTION)
//        {
//            message(player, "Сервис отключен.");
//            return;
//        }
//
//        if(!isSecondCheck(player))
//        {
//            return;
//        }
//
//        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
//        String htmltext = "";
//        if(command.equalsIgnoreCase("list"))
//        {
//            htmltext = getHtm("list");
//            htmltext = htmltext.replace("%MENU%", getMainMenu("list"));
//            htmltext = htmltext.replace("%RESULT%", showSellItems(player, 1, 0, 0, 0, 0, 5, "", 0, 0));
//            //quest.set("last_page", "list")
//        }
//        else if(command.startsWith("StockShowPage_"))
//        {
//            String[] search = command.split("_");
//
//            String name;
//            int page, me, itemId, augment, type2, black, ench;
//            try
//            {
//                page = Integer.parseInt(search[1]);
//                me = Integer.parseInt(search[2]);
//                itemId = Integer.parseInt(search[3]);
//                augment = Integer.parseInt(search[4]);
//                type2 = Integer.parseInt(search[5]);
//                black = Integer.parseInt(search[6]);
//                name = search[7];
//                ench = Integer.parseInt(search[8]);
//            }
//            catch(Exception e)
//            {
//                message(player, "Ошибка запроса листания страниц!");
//                return;
//            }
//
//            if(me == 1)
//            {
//                htmltext = getHtm("office");
//                htmltext = htmltext.replace("abcd", "» My items");
//                htmltext = htmltext.replace("%MENU%", getMainMenu("office"));
//            }
//            else
//            {
//                htmltext = getHtm("list");
//                htmltext = htmltext.replace("%MENU%", getMainMenu("list"));
//                htmltext = htmltext.replace("%RESULT%", showSellItems(player, page, me, 0, itemId, augment, type2, name, ench, black));
//            }
//            //quest.set("last_page", event)
//        }
//        else if(command.startsWith("StockShowItem_"))
//        {
//            String[] search = command.split("_");
//
//            int sellId, black;
//            try
//            {
//                sellId = Integer.parseInt(search[1]);
//                black = Integer.parseInt(search[2]);
//            }
//            catch(Exception e)
//            {
//                message(player, "Ошибка запроса просмотра шмотки!");
//                return;
//            }
//
//            String text = getHtm("show_lot");
//            Connect con = null;
//            PreparedStatement st = null;
//            ResultSet rs = null;
//
//            try
//            {
//                con = L2DatabaseFactory.get();
//                st = con.prepareStatement("SELECT itemId, enchant, augment, augLvl, price, money, ownerId, shadow, date, time, count, type, augAttr FROM `z_stock_items` WHERE `id`=? LIMIT 1");
//                st.setInt(1, sellId);
//                rs = st.executeQuery();
//                if(rs.next())
//                {
//                    int itemId = rs.getInt("itemId");
//                    L2Item brokeItem = ItemTable.getInstance().getTemplate(itemId);
//                    if(brokeItem == null)
//                    {
//                        message(player, "Просмотр шмотки: ошибка запроса!");
//                        return;
//                    }
//                    int money = rs.getInt("money");
//                    int enchant = rs.getInt("enchant");
//                    int augment = rs.getInt("augment");
//                    int auhLevel = rs.getInt("augLvl");
//                    String valute = getMoneyCall(money);
//                    text = text.replace("%ID%", String.valueOf(itemId));
//                    int price = rs.getInt("price");
//                    int charId = rs.getInt("ownerId");
//                    int shadow = rs.getInt("shadow");
//                    int count = rs.getInt("count");
//                    int itype = rs.getInt("type");
//
//                    text = text.replace("%ICON%", brokeItem.getIcon());
//                    text = text.replace("%ITEM_NAME%", brokeItem.getName());
//                    if(itype >= 0 && itype <= 2)
//                    {
//                        if(enchant > 0)
//                            text = text.replace("%ENCHANT%", "+" + enchant);
//                        else
//                            text = text.replace("%ENCHANT%", "");
//                    }
//                    else
//                    {
//                        if(count > 1)
//                            text = text.replace("%ENCHANT%", "x" + count);
//                        else
//                            text = text.replace("%ENCHANT%", "");
//                    }
//
//                    text = text.replace("%SELLER%", getSellerName(con, charId));
//
//                    String dates = rs.getString("date");
//                    String times = rs.getString("time");
//                    if(dates == null)
//                        dates = getHtm("z_lang_error_date");
//                    if(times == null)
//                        times = "";
//                    text = text.replace("%ADD_DATE%", formatTime(dates, times));
//                    if(itype == 0)
//                    {
//                        if(augment == 0)
//                            text = text.replace("%AUGMENT%", getHtm("augment_empty") + "<br1>");
//                        else
//                        {
//                            String augmentSkill = getAugmentSkill(augment, auhLevel, true);
//
//                            int augid = rs.getInt("augAttr");
//                            FastList<AugStat> aug_data = AugmentationData.getInstance().getAugStatsById(rs.getInt("augAttr"));
//                            String aug_bonus = "";
//                            String stat_name;
//                            for(AugStat aug_stat : aug_data)
//                            {
//                                stat_name = stats_synonims.get(aug_stat.getStat().getValue());
//                                String bonus_htm = getHtm("augment_bonus");
//                                bonus_htm = bonus_htm.replace("%STAT%", stat_name);
//                                bonus_htm = bonus_htm.replace("%VALUE%", String.valueOf(/*"%.1f" % */aug_stat.getValue()));
//                                aug_bonus += bonus_htm;
//                            }
//
//                            if(aug_data != null)
//                                augmentSkill = augmentSkill.replace("%BONUS%", aug_bonus);
//                            text = text.replace("%AUGMENT%", augmentSkill);
//                        }
//                    }
//                    else
//                        text = text.replace("%AUGMENT%", "");
//
//                    if(player.getObjectId() == charId)
//                    {
//                        String owner_button = getHtm("show_lot_owner_button");
//                        owner_button = owner_button.replace("%ID%", String.valueOf(sellId));
//                        text = text.replace("%BUY_BUTTON%", owner_button);
//                    }
//                    else
//                    {
//                        L2ItemInstance payment = player.getInventory().getItemByItemId(money);
//                        String buyb = getHtm("show_lot_buy_button");
//                        buyb = buyb.replace("%ID%", String.valueOf(sellId));
//                        text = text.replace("%BUY_BUTTON%", buyb);
//                    }
//
//                    String fprice = String.valueOf(Util.formatAdena(price));
//                    text = text.replace("%PRICE%", fprice);
//                    text = text.replace("%MONEY%", String.valueOf(valute));
//                    if(count > 1)
//                        text = text.replace("+" + rs.getInt("enchant"), "x" + count);
//                }
//                else
//                {
//                    if(rs != null)
//                        rs.close();
//                    if(st != null)
//                        st.close();
//                    if(con != null)
//                        con.close();
//                    //return self.onAdvEvent("find_ 888 _ 1 _ Name",npc,player)
//                }
//            }
//            catch(SQLException e)
//            {
//                System.out.println("[ERROR] Auction, getPageCount() error: " + e);
//            }
//            finally
//            {
//                Close.CSR(con, st, rs);
//            }
//
//            /*String last_page = quest.get("last_page");
//            if(last_page == "" || last_page == null)
//                last_page = "list";
//            text = text.replace("%BACK%", last_page);
//            if(player.getObjectId() == charId)
//            {
//                text = text.replace("%MENU%", getMainMenu("office"));
//                text = text.replace("» Buy »", "» My items »");
//            }
//            else
//                text = text.replace("%MENU%", getMainMenu("list"));*/
//
//            htmltext = text;
//        }
//        else if(command.startsWith("StockBuyItem_"))
//        {
//            String[] search = command.split("_");
//
//            int sellId,count,black;
//            try
//            {
//                sellId = Integer.parseInt(search[1]);
//                count = Integer.parseInt(search[2]);
//                black = Integer.parseInt(search[3]);
//            }
//            catch(Exception e)
//            {
//                message(player, "Ошибка запроса покупки шмотки!");
//                return;
//            }
//
//            String cat = "list";
//            Connect con = null;
//            PreparedStatement st = null;
//            ResultSet rs = null;
//            try
//            {
//                con = L2DatabaseFactory.get();
//                st = con.prepareStatement("SELECT itemId, itemName, enchant, augment, augAttr, augLvl, price, money, ownerId, shadow, count FROM `z_stock_items` WHERE `id`=? LIMIT 1");
//                st.setInt(1, sellId);
//                rs = st.executeQuery();
//                if(rs.next())
//                {
//                    int itemId = rs.getInt("itemId");
//                    L2Item bItem = ItemTable.getInstance().getTemplate(itemId);
//                    if(bItem == null)
//                    {
//                        message(player, "Покупка шмотки: ошибка запроса!");
//                        return;
//                    }
//                    int money = rs.getInt("money");
//                    int charId = rs.getInt("ownerId");
//                    int itemName = rs.getString("itemName");
//                    int enchant = rs.getInt("enchant");
//                    int augment = rs.getInt("augment");
//                    int augAttr = rs.getInt("augAttr");
//                    int auhLevel = rs.getInt("augLvl");
//                    String valute = getMoneyCall(money);
//                    int price = rs.getInt("price");
//                    int count = rs.getInt("count");
//                    int shadow = rs.getInt("shadow");
//
//                    if(augment == 0 && augAttr == 1)
//                        augAttr = 0;
//                    if(price <= 0)
//                    {
//                        message(player, "Покупка шмотки: у Вас нет необходимых предметов!");
//                        return;
//                    }
//                    if(player.getObjectId() == charId)
//                        price = 0;
//                    boolean payment_deleted;
//                    if(price > 0)
//                        payment_deleted = false;
//                    if(player.getInventory().getItemByItemId(money).getCount() >= price)
//                        payment_deleted = player.destroyItemByItemId("zzAuction", money, price, player, true);
//                    if(!payment_deleted)
//                    {
//                        if(rs != null)
//                            rs.close();
//                        if(st != null)
//                            st.close();
//                        if(con != null)
//                            con.close();
//
//                        sendPopupMessage(player, getHtm("z_lang_error_no_money"));
//                        String last_page = quest.get("last_page");
//                        if(last_page == "")
//                            last_page = "list";
//                        //return self.onAdvEvent(last_page,npc,player)
//                    }
//
//                    PreparedStatement zabiraem = null;
//                    try
//                    {
//                        boolean upd = false;
//                        zabiraem=con.prepareStatement("DELETE FROM `z_stock_items` WHERE `id`=?")
//                        if(upd)
//                        {
//                            zabiraem.setInt(1, count);
//                            zabiraem.setInt(2, sellId);
//                        }
//                        else
//                        {
//                            zabiraem.setInt(1, sellId);
//                            zabiraem.executeUpdate();
//                        }
//                    }
//                    catch(Exception e)
//                    {
//                        message(player, "Покупка шмотки: ошибка базы данных!");
//                        return;
//                    }
//                    finally
//                    {
//                        Close.S(zabiraem);
//                    }
//                    Log.add("#delete: " + player.getObjectId() + ", lot: " + sellId + " " + bItem.getName() + ".", "sql_error");
//
//                    String lot_data = "";
//                    String lot = bItem.getName();
//                    if(enchant > 0)
//                        lot_data += " +" + String.valueOf(enchant);
//                    if(count > 1)
//                        lot_data += " x" + String.valueOf(count);
//                    lot += lot_data;
//                    if augment > 0:
//                    lot += "\n" + self.getAugmentSkillPopup(augment, auhLevel)
//                    lot_data += " (" + self.getAugmentSkillPopup(augment, auhLevel) + ")"
//                    lot_name = self.getLangItemDataOffline(itemId, bItem.getName(), lot_data)
//                    #>
//                    if price > 0 and not self.transferPay(con,charId,0,0,0,0,0,price,money,player.getObjectId(),lot_name):
//                    if rs != None:
//                    rs.close()
//                    if st != None:
//                    st.close()
//                    if con != None:
//                    con.close()
//                    return self.error(self.getHtm("z_lang_error_purchase", player),self.getHtm("z_lang_error_view", player) + "8")
//                    #>
//                    GiveItem.insertItem(con, player.getObjectId(), itemId, count, enchant, augAttr, augment, auhLevel, PAYMENT_LOC, charId)
//                    if player.getObjectId() == charId:
//                    self.log("delete", "#owner: " + str(charId) + ", lot: " + lot_name + ".")
//                    else:
//                    self.log("purchased", "#buyer: " + str(player.getObjectId()) + ", owner: " + str(charId) + ", price: " + str(price) + " " + str(self.getMoneyCall(money, player)) + ", lot: " + lot_name + ".")
//                    #>
//                    msg = self.getHtm("z_lang_you", player)
//                    if price == 0:
//                    cat = "office"
//                    msg += self.getHtm("z_lang_you_got_back", player) + "\n"
//                    else:
//                    msg += self.getHtm("z_lang_you_bought", player) + "\n"
//                    msg += lot
//                    self.sendPopupMessage(player, msg)
//                    #>
//                    if price > 0:
//                    alarm = L2World.getInstance().getPlayer(charId)
//                    if alarm:
//                    lot = self.getLangItemName(itemId, bItem.getName(), alarm)
//                    if enchant > 0:
//                    lot += " +" + str(enchant)
//                    if count > 1:
//                    lot += " x" + str(count)
//                    lot_aug = lot
//                    if augment > 0:
//                    lot_aug += " (" + self.getAugmentSkillPopup(augment, auhLevel) + ")"
//                    msg_notify = self.getHtm("z_lang_notify", alarm)
//                    msg_notify = msg_notify.replace("%ITEM%", lot_aug)
//                    alarm.sendMessage(msg_notify)
//                    #>
//                }
//                else:
//                if rs != None:
//                rs.close()
//                if st != None:
//                st.close()
//                if con != None:
//                con.close()
//                return self.onAdvEvent("find_ 888 _ 1 _ Name",npc,player)
//                #return self.error(self.getHtm("z_lang_error_purchase", player),self.getHtm("z_lang_error_not_found", player))
//            }
//            except:
//            raise
//            #       pass
//            if rs != None:
//            rs.close()
//            if st != None:
//            st.close()
//            if con != None:
//            con.close()
//            return self.onAdvEvent(cat,npc,player)
//        }
//
//        html.setHtml(htmltext);
//        player.sendPacket(html);
//        htmltext = null;
//        html = null;
//    }
//
//    private String showSellItems(L2PcInstance player, int page, int me, int last, int itemId, int augment, int type2, String name, int ench, int black)
//    {
//        String text = getHtm("list_items");
//        if(me == 0)
//            text = text.replace("%MENU%", getHtm("list_items_menu"));
//        else
//            text = text.replace("%MENU%", getHtm("list_items_menu_owner"));
//
//        int limit1 = (page-1) * PAGE_LIMIT;
//        int limit2 = PAGE_LIMIT;
//
//        Connect con = null;
//        ResultSet rs = null;
//        PreparedStatement st = null;
//
//        int found = 0;
//        try
//        {
//            con = L2DatabaseFactory.get();
//            if(type2 >= 0)
//            {
//                if(type2 == 2)
//                {
//                    st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` WHERE `itemId` NOT BETWEEN ? AND ? AND `itemId` <> ? AND `type` = ? AND `augAttr` <> ? ORDER BY `id` DESC LIMIT ?, ?");
//                    if(SHOW_EPIC_DEFAULT)
//                    {
//                        st.setInt(1, 0);
//                        st.setInt(2, 1);
//                        st.setInt(3, 2);
//                    }
//                    else
//                    {
//                        st.setInt(1, 6656);
//                        st.setInt(2, 6662);
//                        st.setInt(3, 8191);
//                    }
//                    st.setInt(4, 2);
//                    st.setInt(5, 1);
//                    st.setInt(6, limit1);
//                    st.setInt(7, limit2);
//                }
//                else if(type2 == 3)
//                {
//                    st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` WHERE `itemId` BETWEEN ? AND ? OR `itemId` = ? ORDER BY `id` DESC LIMIT ?, ?");
//                    st.setInt(1, 6656);
//                    st.setInt(2, 6662);
//                    st.setInt(3, 8191);
//                    st.setInt(4, limit1);
//                    st.setInt(5, limit2);
//                }
//                else if(type2 == 4)
//                {
//                    st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` WHERE `type` = ? AND `augment` > ? ORDER BY `id` DESC LIMIT ?, ?");
//                    st.setInt(1, 0);
//                    st.setInt(2, 1);
//                    st.setInt(3, limit1);
//                    st.setInt(4, limit2);
//                }
//                else if(type2 == 5)
//                {
//                    st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` ORDER BY `id` DESC LIMIT ?, ?");
//                    st.setInt(1, limit1);
//                    st.setInt(2, limit2);
//                }
//                else if(type2 == 6)
//                {
//                    st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` WHERE `type` > ? OR `augAttr` = ? ORDER BY `id` DESC LIMIT ?, ?");
//                    st.setInt(1, 2);
//                    st.setInt(2, 1);
//                    st.setInt(3, limit1);
//                    st.setInt(4, limit2);
//                }
//                else
//                {
//                    st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` WHERE `type` = ? ORDER BY `id` DESC LIMIT ?, ?");
//                    st.setInt(1, type2);
//                    st.setInt(2, limit1);
//                    st.setInt(3, limit2);
//                }
//            }
//            else if(itemId > 0)
//            {
//                st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` WHERE `itemId` = ? ORDER BY `id` DESC LIMIT ?, ?");
//                st.setInt(1, itemId);
//                st.setInt(2, limit1);
//                st.setInt(3, limit2);
//            }
//            else if(name != "")
//            {
//                st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` WHERE `itemName` LIKE ? ORDER BY `id` DESC LIMIT ?, ?");
//                st.setString(1, "%"+ name +"%");
//                st.setInt(2, limit1);
//                st.setInt(3, limit2);
//            }
//            else if(me == 1)
//            {
//                st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` WHERE `ownerId` = ? ORDER BY `id` DESC LIMIT ?, ?");
//                st.setInt(1, player.getObjectId());
//                st.setInt(2, limit1);
//                st.setInt(3, limit2);
//            }
//            else
//            {
//                st = con.prepareStatement("SELECT id, itemId, enchant, augment, augLvl, price, money, ownerId, shadow, top, count, type FROM `z_stock_items` ORDER BY `id` DESC LIMIT ?, ?");
//                st.setInt(1, limit1);
//                st.setInt(2, limit2);
//            }
//            rs=st.executeQuery();
//
//            String LIST_ITEMS = "";
//            String PRICE = getHtm("price");
//            while(rs.next())
//            {
//                int sId = rs.getInt("id");
//                int itmId = rs.getInt("itemId");
//                int ownerId = rs.getInt("ownerId");
//                int count = rs.getInt("count");
//                int itype = rs.getInt("type");
//                int encant = rs.getInt("enchant");
//
//                L2Item brokeItem = ItemTable.getInstance().getTemplate(itmId);
//                if(brokeItem == null)
//                    continue;
//
//                String HPRICEB = "";
//                String LIST_ITEMSN = getHtm("list_items_lot");
//                HPRICEB = PRICE.replace("%PRICE%", String.valueOf(Util.formatAdena(rs.getInt("price"))));
//                HPRICEB = HPRICEB.replace("%MONEY%", getMoneyCall(rs.getInt("money")));
//                HPRICEB = HPRICEB.replace("%SELLER%", getSellerName(con,ownerId));
//
//                String HLISTB = LIST_ITEMSN.replace("%ID%", String.valueOf(sId));
//                HLISTB = HLISTB.replace("%ICON%", brokeItem.getIcon());
//                HLISTB = HLISTB.replace("%ITEM_NAME%", brokeItem.getName());
//                HLISTB = HLISTB.replace("%PRICE%", HPRICEB);
//                HLISTB = HLISTB.replace("%AUGMENT%", getAugmentSkill(rs.getInt("augment"), rs.getInt("augLvl"), false));
//                if(itype >= 0 && itype <= 2)
//                {
//                    if(encant > 0)
//                        HLISTB = HLISTB.replace("%ENCHANT%", "+" + encant);
//                    else
//                        HLISTB = HLISTB.replace("%ENCHANT%", "");
//                }
//                else
//                {
//                    if(count > 1)
//                        HLISTB = HLISTB.replace("%ENCHANT%", "x" + count);
//                    else
//                        HLISTB = HLISTB.replace("%ENCHANT%", "");
//                }
//
//                LIST_ITEMS += HLISTB;
//                found += 1;
//            }
//
//            int search = 1;
//            String search_type = "";
//            if(type2 >= 0)
//            {
//                search = 2;
//
//                String stype_htm = getHtm("category_label_" + type2 + "");
//                if(stype_htm == null)
//                {
//                    String value = sorts[type2];
//                    search_type = getHtm("add_category");
//                    search_type = search_type.replace("%CATEGORY%", value);
//                }
//                else
//                    search_type = stype_htm;
//            }
//            else
//            {
//                if(name != "")
//                {
//                    search_type = getHtm("search_type");
//                    search_type = search_type.replace("%TEXT%", name);
//                }
//                else if(itemId != 0)
//                {
//                    search_type = getHtm("search_type");
//                    search_type = search_type.replace("%TEXT%", String.valueOf(itemId));
//                }
//                else
//                {
//                    search = 0;
//                    search_type = "";
//                }
//            }
//
//            if(found == 0)
//            {
//                search_type = "";
//                if(me == 1)
//                    LIST_ITEMS = "<tr><td></td></tr></table>" + getHtm("office_empty") + "<table><tr><td></td></tr>";
//                else if(search == 1)
//                    LIST_ITEMS = "<tr><td></td></tr></table>" + getHtm("search_empty") + "<table><tr><td></td></tr>";
//                else
//                    LIST_ITEMS = "<tr><td></td></tr></table>" + getHtm("category_empty") + "<table><tr><td></td></tr>";
//            }
//
//            text = text.replace("%SEARCH_TYPE%", search_type);
//            text = text.replace("%LIST_ITEMS%", LIST_ITEMS);
//
//            String HLAST = "";
//            int pages = 0;
//            if(last == 1 || found == 0)
//                HLAST = "<br>";
//            else
//            {
//                pages = getPageCount(con, me, itemId, augment, type2, player.getObjectId(), name, ench, black);
//                HLAST = sortInventoryPages(page, pages, "StockShowPage_", me + "_" + itemId + "_" + augment + "_" + type2 + "_" + black + "_" + name + "_" + ench);
//            }
//
//            text = text.replace("%LAST%", HLAST);
//        }
//        catch(SQLException e)
//        {
//            System.out.println("[ERROR] Auction, StockShowItem error: " + e);
//        }
//        finally
//        {
//            Close.CSR(con, st, rs);
//        }
//        return text;
//    }
//
//    private boolean isSecondCheck(L2PcInstance player)
//    {
//        if(player.getActiveTradeList() != null)
//        {
//            player.cancelActiveTrade();
//            message(player, "Ошибка запроса; 0x101");
//            return false;
//        }
//
//        if(player.isTransactionInProgress() || player.getActiveWarehouse() != null)
//        {
//            message(player, "Ошибка запроса; 0x102");
//            return false;
//        }
//
//        if(player.getPrivateStoreType() != 0)
//        {
//            message(player, "Ошибка запроса; 0x103");
//            return false;
//        }
//
//        if(player.getActiveEnchantItem() != null)
//        {
//            player.setActiveEnchantItem(null);
//            message(player, "Ошибка запроса; 0x104");
//            return false;
//        }
//
//        return true;
//    }
//
//    private String getMoneyCall(int money)
//    {
//        return moneys.get(Integer.valueOf(money));
//    }
//
//    private String getSellerName(Connect con, int charId)
//    {
//        L2PcInstance player = L2World.getInstance().getPlayer(charId);
//        if(player != null)
//            return player.getName();
//
//        String name = "???";
//        PreparedStatement st = null;
//        ResultSet rset = null;
//        try
//        {
//            st = con.prepareStatement("SELECT char_name FROM `characters` WHERE `obj_Id`=? LIMIT 1");
//            st.setInt(1, charId);
//            rset = st.executeQuery();
//            if(rset.next())
//            {
//                name = rset.getString("char_name");
//                return name;
//            }
//        }
//        catch(Exception e)
//        {
//            System.out.println("[ERROR] Auction, getSellerName() error: " + e);
//        }
//        finally
//        {
//            Close.SR(st, rset);
//        }
//        return name;
//    }
//
//    private int getPageCount(Connect con, int me, int itemId, int augment, int type2, int charId, String name, int ench, int black)
//    {
//        int rowCount = 0;
//        int pages = 0;
//        PreparedStatement st = null;
//        ResultSet rs = null;
//        try
//        {
//            if(type2 >= 0)
//            {
//                if(type2 == 2)
//                {
//                    st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `itemId` NOT BETWEEN ? AND ? AND `itemId` <> ? AND `type` = ? AND `augAttr` <> ?");
//                    if(SHOW_EPIC_DEFAULT)
//                    {
//                        st.setInt(1, 0);
//                        st.setInt(2, 1);
//                        st.setInt(3, 2);
//                    }
//                    else
//                    {
//                        st.setInt(1, 6656);
//                        st.setInt(2, 6662);
//                        st.setInt(3, 8191);
//                        st.setInt(4, 2);
//                        st.setInt(5, 1);
//                    }
//                }
//                else if(type2 == 3)
//                {
//                    st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `itemId` BETWEEN ? AND ? OR `itemId` = ?");
//                    st.setInt(1, 6656);
//                    st.setInt(2, 6662);
//                    st.setInt(3, 8191);
//                }
//                else if(type2 == 4)
//                {
//                    st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `type` = ? AND `augment` > ?");
//                    st.setInt(1, 0);
//                    st.setInt(2, 1);
//                }
//                else if(type2 == 5)
//                {
//                    st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `id` > ?");
//                    st.setInt(1, 0);
//                }
//                else if(type2 == 6)
//                {
//                    st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `type` > ? OR `augAttr` = ?");
//                    st.setInt(1, 2);
//                    st.setInt(2, 1);
//                }
//                else
//                {
//                    st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `type` = ?");
//                    st.setInt(1, type2);
//                }
//            }
//            else if(itemId > 0)
//            {
//                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `itemId` = ?");
//                st.setInt(1, itemId);
//            }
//            else if(augment > 0)
//            {
//                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `augment` = ?");
//                st.setInt(1, augment);
//            }
//            else if(name != "")
//            {
//                if(ench == 0)
//                {
//                    st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `itemName` LIKE ?");
//                    st.setString(1, "%" + name + "%");
//                }
//                else
//                {
//                    st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `itemName` LIKE ? AND `enchant` = ?");
//                    st.setString(1, "%"+ name +"%");
//                    st.setInt(2, ench);
//                }
//            }
//            else if(ench > 0)
//            {
//                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `enchant` = ?");
//                st.setInt(1, ench);
//            }
//            else if(me == 1)
//            {
//                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `ownerId` = ?");
//                st.setInt(1, charId);
//            }
//            else
//                st = con.prepareStatement("SELECT COUNT(`id`) FROM `z_stock_items` WHERE `id` > 0");
//
//            rs = st.executeQuery();
//            if(rs.next())
//                rowCount = rs.getInt(1);
//        }
//        catch(SQLException e)
//        {
//            System.out.println("[ERROR] Auction, getPageCount() error: " + e);
//        }
//        finally
//        {
//            Close.SR(st, rs);
//        }
//
//        if(rowCount == 0)
//            return 0;
//        pages = (int) Math.ceil(rowCount / (double) PAGE_LIMIT);
//        return pages;
//    }
//
//    private String sortInventoryPages(int page, int pages, String bypass, String data)
//    {
//        if(pages == 1)
//            return "<center><font color=LEVEL> 1</font></center>";
//        if(pages == 0)
//            return "EMPTY";
//
//        TextBuilder text = new TextBuilder("<table width=292 border=0><tr><td align=center width=292> <table width=260 border=0><tr>")
//        int s = Math.max(1, page - 3);
//        int f = page + 3;
//        if(page < 5)
//        {
//            s = 1;
//            f = SORT_LIMIT;
//        }
//        f = Math.min(pages, f);
//        if(pages <= (SORT_LIMIT + 1))
//        {
//            s = 1;
//            f = pages;
//        }
//        int rows = (f - s) + 1;
//        if(page >= 5)
//            rows += 2;
//        if(pages >= SORT_LIMIT && f < pages)
//            rows += 2;
//        int td_width = (292 / rows) + 1;
//        if(page >= 5 && pages > (SORT_LIMIT + 1))
//            text.append("<td width=" + td_width + " align=center><a action=\"bypass -h Quest purchase " + bypass + "1_" + data +  "\"> 1 </a></td><td width=" + td_width + " align=center>..</td>");
//        for(int i = s; i <= f + 1; i++) // TODO [V] - перепроверить цикл
//        {
//            if(i == page)
//                text.append("<td width=" + td_width + " align=center><font color=LEVEL>" + i + "</font></td>");
//            else
//                text.append("<td width=" + td_width + " align=center><a action=\"bypass -h Quest purchase " + bypass + "" + i + "_" + data +  "\">" + i + "</a></td>");
//        }
//        if(pages > (SORT_LIMIT + 1) && f < pages)
//            text.append("<td width=" + td_width + " align=center>..</td><td width=" + td_width + " align=center><a action=\"bypass -h Quest purchase " + bypass + "" + pages + "_" + data +  "\"> " + pages + " </a></td>");
//        text.append("</tr></table> </td></tr></table>");
//
//        String htmltext = text.toString();
//        text.clear();
//        return htmltext;
//    }
//
//    private String getAugmentSkill(int aid, int lvl, boolean full)
//    {
//        L2Skill augment = SkillTable.getInstance().getInfo(aid, lvl);
//        if(augment == null)
//            return "";
//        String augName = augment.getName();
//        String stype = "chance";
//        if(augment.isActive())
//            stype = "active";
//        else if(augment.isPassive())
//            stype = "passive";
//
//        String auginfo;
//        if(full)
//        {
//            auginfo = getHtm("show_lot_augment");
//            auginfo = auginfo.replace("%AUG_SKILL%", getAugmentSkill(aid, lvl, false));
//        }
//        else
//        {
//            augName = augName.replace("Item Skill: ", "");
//            //if(augment.isMassSkill())
//            //    augName = "Mass " + augName;
//            auginfo = getHtm("list_items_augment_" + stype + "");
//            auginfo = auginfo.replace("%AUG_NAME%", augName);
//        }
//
//        return auginfo;
//    }
//
//    private String getHtm(String htm)
//    {
//        return HtmCache.getInstance().getHtm("data/html/mods/auction/" + htm + ".htm");
//    }
//
//    private String getMainMenu(String cat)
//    {
//        return getHtm("main_menu_" + cat + "");
//    }
//
//    private String formatTime(String date, String time)
//    {
//        if(time == "")
//            return date;
//        String[] d = date.split("-");
//        date = d[0] + "." + d[1] + "." + d[2] + " "  + time;
//        return date;
//    }
//
//    private void sendPopupMessage(L2PcInstance player, String msg)
//    {
//        player.sendPacket(SystemMessage.id(2058).addString(msg));
//    }
//
//    private void message(L2PcInstance player, String text)
//    {
//        String htmltext = "<html><body><br>" + text + "</body></html>";
//        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
//        html.setHtml(htmltext);
//        player.sendPacket(html);
//        htmltext = null;
//        html = null;
//    }
}