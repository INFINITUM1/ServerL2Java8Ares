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
package scripts.commands.admincommandhandlers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import ru.agecold.Config;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.TradeController;
import ru.agecold.gameserver.cache.HtmCache;
import ru.agecold.gameserver.datatables.ItemTable;
import ru.agecold.gameserver.datatables.NpcTable;
import scripts.commands.IAdminCommandHandler;
import ru.agecold.gameserver.model.L2DropCategory;
import ru.agecold.gameserver.model.L2DropData;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2TradeList;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.agecold.gameserver.templates.L2Item;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.gameserver.templates.StatsSet;

/**
 * @author terry
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AdminEditNpc implements IAdminCommandHandler {
	private static Logger _log = Logger.getLogger(AdminEditChar.class.getName());
	private final static int PAGE_LIMIT = 7;

	private static final String[] ADMIN_COMMANDS = {
		"admin_edit_npc",
		"admin_save_npc",
		"admin_show_droplist",
		"admin_edit_drop",
		"admin_add_drop",
		"admin_del_drop",
		"admin_showShop",
		"admin_showShopList",
		"admin_addShopItem",
		"admin_delShopItem",
		"admin_box_access",
		"admin_editShopItem",
		"admin_close_window"
	};
	private static final int REQUIRED_LEVEL = Config.GM_NPC_EDIT;
	private static final int REQUIRED_LEVEL2 = Config.GM_NPC_VIEW;

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{//TODO: Tokenize and protect arguments parsing. Externalize HTML.
		if (!Config.ALT_PRIVILEGES_ADMIN)
			if (!((checkLevel(activeChar.getAccessLevel()) || checkLevel2(activeChar.getAccessLevel())) && activeChar.isGM()))
				return false;

		if (command.startsWith("admin_showShop "))
		{
			String[] args = command.split(" ");
			if (args.length > 1)
				showShop(activeChar, Integer.parseInt(command.split(" ")[1]));
		}
		else if(command.startsWith("admin_showShopList "))
		{
			String[] args = command.split(" ");
			if (args.length > 2)
				showShopList(activeChar, Integer.parseInt(command.split(" ")[1]), Integer.parseInt(command.split(" ")[2]));
		}
		else if(command.startsWith("admin_edit_npc "))
		{
			try
			{
				String[] commandSplit = command.split(" ");
				int npcId = Integer.valueOf(commandSplit[1]);
				L2NpcTemplate npc = NpcTable.getInstance().getTemplate(npcId);
				Show_Npc_Property(activeChar, npc);
			}
			catch (Exception e)
			{
				activeChar.sendAdmResultMessage("Wrong usage: //edit_npc <npcId>");
			}
		}
		else if(command.startsWith("admin_show_droplist "))
		{
			int npcId = 0;
			try
			{
				npcId = Integer.parseInt(command.substring(20).trim());
			}
			catch(Exception e){}

			if(npcId > 0)
				showNpcDropList(activeChar, npcId);
			else
				activeChar.sendAdmResultMessage("Usage: //show_droplist <npc_id>");
		}
		else if (!Config.ALT_PRIVILEGES_ADMIN && !(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM()))
			return false;
		else if(command.startsWith("admin_addShopItem "))
		{
			String[] args = command.split(" ");
			if (args.length > 1)
				addShopItem(activeChar, args);
		}
		else if(command.startsWith("admin_delShopItem "))
		{
			String[] args = command.split(" ");
			if (args.length > 2)
				delShopItem(activeChar, args);
		}
		else if(command.startsWith("admin_editShopItem "))
		{
			String[] args = command.split(" ");
			if (args.length > 2)
				editShopItem(activeChar, args);
		}
		else if(command.startsWith("admin_save_npc "))
		{
			try
			{
				save_npc_property(activeChar, command);
			}
			catch (StringIndexOutOfBoundsException e)
			{}
		}
		else if(command.startsWith("admin_edit_drop "))
		{
			int npcId = -1, itemId = 0, category = -1000;
			try
			{
				StringTokenizer st = new StringTokenizer(command.substring(16).trim());
				if (st.countTokens() == 3)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						itemId = Integer.parseInt(st.nextToken());
						category = Integer.parseInt(st.nextToken());
						showEditDropData(activeChar, npcId, itemId, category);
					}
					catch(Exception e)
					{}
				}
				else if (st.countTokens() == 6)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						itemId = Integer.parseInt(st.nextToken());
						category = Integer.parseInt(st.nextToken());
						int min = Integer.parseInt(st.nextToken());
						int max = Integer.parseInt(st.nextToken());
						int chance = Integer.parseInt(st.nextToken());

						updateDropData(activeChar, npcId, itemId, min, max, category, chance);
					}
					catch(Exception e)
					{
						_log.fine("admin_edit_drop parements error: " + command);
					}
				}
				else
					activeChar.sendAdmResultMessage("Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendAdmResultMessage("Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
			}
		}
		else if (command.startsWith("admin_add_drop "))
		{
			int npcId = -1;
			try
			{
				StringTokenizer st = new StringTokenizer(command.substring(15).trim());
				if(st.countTokens() == 1)
				{
					try
					{
						String[] input = command.substring(15).split(" ");
						if (input.length < 1)
							return true;
						npcId = Integer.parseInt(input[0]);
					}
					catch(Exception e){}

					if(npcId > 0)
					{
						L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
						showAddDropData(activeChar, npcData);
					}
				}
				else if(st.countTokens() == 6)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						int itemId = Integer.parseInt(st.nextToken());
						int category = Integer.parseInt(st.nextToken());
						int min = Integer.parseInt(st.nextToken());
						int max = Integer.parseInt(st.nextToken());
						int chance = Integer.parseInt(st.nextToken());

						addDropData(activeChar, npcId, itemId, min, max, category, chance);
					}
					catch(Exception e)
					{
						_log.fine("admin_add_drop parements error: " + command);
					}
				}
				else
					activeChar.sendAdmResultMessage("Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendAdmResultMessage("Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
			}
		}
		else if(command.startsWith("admin_del_drop "))
		{
			int npcId = -1, itemId = -1, category = -1000;
			try
			{
				String[] input = command.substring(15).split(" ");
				if (input.length >= 3)
				{
					npcId = Integer.parseInt(input[0]);
					itemId = Integer.parseInt(input[1]);
					category = Integer.parseInt(input[2]);
				}
			}
			catch(Exception e){}

			if(npcId > 0)
				deleteDropData(activeChar, npcId, itemId, category);
			else
				activeChar.sendAdmResultMessage("Usage: //del_drop <npc_id> <item_id> <category>");
		}

		return true;
	}

	private void editShopItem(L2PcInstance activeChar, String[] args)
	{
		int tradeListID = Integer.parseInt(args[1]);
		int itemID = Integer.parseInt(args[2]);
		L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);

		L2Item item = ItemTable.getInstance().getTemplate(itemID);
		if (tradeList.getPriceForItemId(itemID) < 0)
		{
			return;
		}

		if (args.length > 3)
		{
			int price = Integer.parseInt(args[3]);
			int order =  findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);

			tradeList.replaceItem(itemID, Integer.parseInt(args[3]));
			updateTradeList(itemID, price, tradeListID, order);

			activeChar.sendAdmResultMessage("Updated price for "+item.getName()+" in Trade List "+tradeListID);
			showShopList(activeChar, tradeListID, 1);
			return;
		}

		NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);

		TextBuilder replyMSG = new TextBuilder();
		replyMSG.append("<html><title>Merchant Shop Item Edit</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Edit an entry in merchantList.");
		replyMSG.append("<br>Editing Item: "+item.getName());
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=100>Property</td><td width=100>Edit Field</td><td width=100>Old Value</td></tr>");
		replyMSG.append("<tr><td><br></td><td></td></tr>");
		replyMSG.append("<tr><td>Price</td><td><edit var=\"price\" width=80></td><td>"+tradeList.getPriceForItemId(itemID)+"</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><br><br><br>");
		replyMSG.append("<button value=\"Save\" action=\"bypass -h admin_editShopItem " + tradeListID + " " + itemID + " $price\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back\" action=\"bypass -h admin_showShopList " + tradeListID +" 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void delShopItem(L2PcInstance activeChar, String[] args)
	{
		int tradeListID = Integer.parseInt(args[1]);
		int itemID = Integer.parseInt(args[2]);
		L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);

		if (tradeList.getPriceForItemId(itemID) < 0)
			return;

		if (args.length > 3)
		{
			int order =  findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);

			tradeList.removeItem(itemID);
			deleteTradeList(tradeListID, order);

			activeChar.sendAdmResultMessage("Deleted "+ItemTable.getInstance().getTemplate(itemID).getName()+" from Trade List "+tradeListID);
			showShopList(activeChar, tradeListID, 1);
			return;
		}

		NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);

		TextBuilder replyMSG = new TextBuilder();
		replyMSG.append("<html><title>Merchant Shop Item Delete</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Delete entry in merchantList.");
		replyMSG.append("<br>Item to Delete: "+ItemTable.getInstance().getTemplate(itemID).getName());
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=100>Property</td><td width=100>Value</td></tr>");
		replyMSG.append("<tr><td><br></td><td></td></tr>");
		replyMSG.append("<tr><td>Price</td><td>"+tradeList.getPriceForItemId(itemID)+"</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><br><br><br>");
		replyMSG.append("<button value=\"Confirm\" action=\"bypass -h admin_delShopItem " + tradeListID + " " + itemID + " 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back\" action=\"bypass -h admin_showShopList " + tradeListID +" 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void addShopItem(L2PcInstance activeChar, String[] args)
	{
		int tradeListID = Integer.parseInt(args[1]);

		L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		if (tradeList == null)
		{
			activeChar.sendAdmResultMessage("TradeList not found!");
			return;
		}

		if (args.length > 3)
		{
			int order = tradeList.getItems().size() + 1; // last item order + 1
			int itemID = Integer.parseInt(args[2]);
			int price = Integer.parseInt(args[3]);

			L2ItemInstance newItem = ItemTable.getInstance().createDummyItem(itemID);
			newItem.setPriceToSell(price);
			newItem.setCount(-1);
			tradeList.addItem(newItem);
			storeTradeList(itemID, price, tradeListID, order);

			activeChar.sendAdmResultMessage("Added "+newItem.getItem().getName()+" to Trade List "+tradeList.getListId());
			showShopList(activeChar, tradeListID, 1);
			return;
		}

		NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);

		TextBuilder replyMSG = new TextBuilder();
		replyMSG.append("<html><title>Merchant Shop Item Add</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Add a new entry in merchantList.");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=100>Property</td><td>Edit Field</td></tr>");
		replyMSG.append("<tr><td><br></td><td></td></tr>");
		replyMSG.append("<tr><td>ItemID</td><td><edit var=\"itemID\" width=80></td></tr>");
		replyMSG.append("<tr><td>Price</td><td><edit var=\"price\" width=80></td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><br><br><br>");
		replyMSG.append("<button value=\"Save\" action=\"bypass -h admin_addShopItem " + tradeListID + " $itemID $price\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back\" action=\"bypass -h admin_showShopList " + tradeListID +" 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showShopList(L2PcInstance activeChar, int tradeListID, int page)
	{
		L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		if (page > tradeList.getItems().size() / PAGE_LIMIT + 1 || page < 1)
			return;

		NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
		TextBuilder html = itemListHtml(tradeList, page);

		adminReply.setHtml(html.toString());
		activeChar.sendPacket(adminReply);

	}

	private TextBuilder itemListHtml(L2TradeList tradeList, int page)
	{
		TextBuilder replyMSG = new TextBuilder();

		replyMSG.append("<html><title>Merchant Shop List Page: "+page+"</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Edit, add or delete entries in a merchantList.");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=150>Item Name</td><td width=60>Price</td><td width=40>Delete</td></tr>");
		int start = ((page-1) * PAGE_LIMIT);
		int end = Math.min(((page-1) * PAGE_LIMIT) + (PAGE_LIMIT-1), tradeList.getItems().size() - 1);
		for (L2ItemInstance item : tradeList.getItems(start, end+1))
		{
			replyMSG.append("<tr><td><a action=\"bypass -h admin_editShopItem "+tradeList.getListId()+" "+item.getItemId()+"\">"+item.getItem().getName()+"</a></td>");
			replyMSG.append("<td>"+item.getPriceToSell()+"</td>");
			replyMSG.append("<td><button value=\"Del\" action=\"bypass -h admin_delShopItem "+tradeList.getListId()+" "+item.getItemId()+"\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr>");
		}//*/
		replyMSG.append("<tr>");
		int min = 1;
		int max = tradeList.getItems().size() / PAGE_LIMIT + 1;
		if (page > 1)
		{
			replyMSG.append("<td><button value=\"Page"+(page - 1)+"\" action=\"bypass -h admin_showShopList "+tradeList.getListId()+" "+(page - 1)+"\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		}
		if (page < max)
		{
			if (page <= min)
				replyMSG.append("<td></td>");
			replyMSG.append("<td><button value=\"Page"+(page + 1)+"\" action=\"bypass -h admin_showShopList "+tradeList.getListId()+" "+(page + 1)+"\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		}
		replyMSG.append("</tr><tr><td>.</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Add\" action=\"bypass -h admin_addShopItem "+tradeList.getListId()+"\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center></body></html>");

		return replyMSG;
	}

	private void showShop(L2PcInstance activeChar, int merchantID)
	{
		List<L2TradeList> tradeLists = getTradeLists(merchantID);
		if(tradeLists == null)
		{
			activeChar.sendAdmResultMessage("Unknown npc template ID" + merchantID);
			return ;
		}

		NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);

		TextBuilder replyMSG = new TextBuilder("<html><title>Merchant Shop Lists</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Select a list to view");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td>Mecrchant List ID</td></tr>");

		for (L2TradeList tradeList : tradeLists)
		{
			if (tradeList != null)
				replyMSG.append("<tr><td><a action=\"bypass -h admin_showShopList "+tradeList.getListId()+" 1\">Trade List "+tradeList.getListId()+"</a></td></tr>");
		}

		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center></body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void storeTradeList(int itemID, int price, int tradeListID, int order)
	{
		ru.agecold.mysql.Connect con = null;
		try
		{
			con = L2DatabaseFactory.get();
			PreparedStatement stmt = con.prepareStatement("INSERT INTO merchant_buylists (`item_id`,`price`,`shop_id`,`order`) values ("+itemID+","+price+","+tradeListID+","+order+")");
			stmt.execute();
			stmt.close();
		}
		catch (SQLException esql)
		{
			esql.printStackTrace();
		}
		finally
		{
			con.close();
		}
	}

	private void updateTradeList(int itemID, int price, int tradeListID, int order)
	{
		ru.agecold.mysql.Connect con = null;
		try
		{
			con = L2DatabaseFactory.get();
			PreparedStatement stmt = con.prepareStatement("UPDATE merchant_buylists SET `price`='"+price+"' WHERE `shop_id`='"+tradeListID+"' AND `order`='"+order+"'");
			stmt.execute();
			stmt.close();
		}catch (SQLException esql)
		{
			esql.printStackTrace();
		}
		finally
		{
			con.close();
		}
	}

	private void deleteTradeList(int tradeListID, int order)
	{
		ru.agecold.mysql.Connect con = null;
		try
		{
			con = L2DatabaseFactory.get();
			PreparedStatement stmt = con.prepareStatement("DELETE FROM merchant_buylists WHERE `shop_id`='"+tradeListID+"' AND `order`='"+order+"'");
			stmt.execute();
			stmt.close();
		}
		catch (SQLException esql)
		{
			esql.printStackTrace();
		}
		finally
		{
			con.close();
		}
	}

	private int  findOrderTradeList(int itemID, int price, int tradeListID)
	{
		ru.agecold.mysql.Connect con = null;
		int order = 0;
		try
		{
			con = L2DatabaseFactory.get();
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM merchant_buylists WHERE `shop_id`='"+tradeListID+"' AND `item_id` ='"+itemID+"' AND `price` = '"+price+"'");
			ResultSet rs = stmt.executeQuery();
			rs.first();

			order = rs.getInt("order");

			stmt.close();
			rs.close();
		}
		catch (SQLException esql)
		{
			esql.printStackTrace();
		}
		finally
		{
			con.close();
		}
		return order;
	}

	private List<L2TradeList> getTradeLists(int merchantID)
	{
		String target = "npc_%objectId%_Buy";

		String content = HtmCache.getInstance().getHtm("data/html/merchant/"+merchantID+".htm");

		if (content == null)
		{
			content = HtmCache.getInstance().getHtm("data/html/merchant/30001.htm");
			if (content == null)
				return null;
		}

		List<L2TradeList> tradeLists = new FastList<L2TradeList>();

		String[] lines = content.split("\n");
		int pos = 0;

		for (String line : lines)
		{
			pos = line.indexOf(target);
			if (pos >= 0)
			{
				int tradeListID = Integer.decode((line.substring(pos+target.length()+1)).split("\"")[0]);
				tradeLists.add(TradeController.getInstance().getBuyList(tradeListID));
			}
		}
		return tradeLists;
	}

	private boolean checkLevel(int level) {
		return (level >= REQUIRED_LEVEL);
	}
	private boolean checkLevel2(int level) {
		return (level >= REQUIRED_LEVEL2);
	}

	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private void Show_Npc_Property(L2PcInstance activeChar, L2NpcTemplate npc)
	{
		NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
		String content = HtmCache.getInstance().getHtm("data/html/admin/editnpc.htm");

		if (content != null)
		{
			adminReply.setHtml(content);
			adminReply.replace("%npcId%", String.valueOf(npc.npcId));
			adminReply.replace("%templateId%", String.valueOf(npc.idTemplate));
			adminReply.replace("%name%", npc.name);
			adminReply.replace("%serverSideName%", npc.serverSideName == true ? "1" : "0");
			adminReply.replace("%title%", npc.title);
			adminReply.replace("%serverSideTitle%", npc.serverSideTitle == true ? "1" : "0");
			adminReply.replace("%collisionRadius%", String.valueOf(npc.collisionRadius));
			adminReply.replace("%collisionHeight%", String.valueOf(npc.collisionHeight));
			adminReply.replace("%level%", String.valueOf(npc.level));
			adminReply.replace("%sex%", String.valueOf(npc.sex));
			adminReply.replace("%type%", String.valueOf(npc.type));
			adminReply.replace("%attackRange%", String.valueOf(npc.baseAtkRange));
			adminReply.replace("%hp%", String.valueOf(npc.baseHpMax));
			adminReply.replace("%mp%", String.valueOf(npc.baseMpMax));
			adminReply.replace("%hpRegen%", String.valueOf(npc.baseHpReg));
			adminReply.replace("%mpRegen%", String.valueOf(npc.baseMpReg));
			adminReply.replace("%str%", String.valueOf(npc.baseSTR));
			adminReply.replace("%con%", String.valueOf(npc.baseCON));
			adminReply.replace("%dex%", String.valueOf(npc.baseDEX));
			adminReply.replace("%int%", String.valueOf(npc.baseINT));
			adminReply.replace("%wit%", String.valueOf(npc.baseWIT));
			adminReply.replace("%men%", String.valueOf(npc.baseMEN));
			adminReply.replace("%exp%", String.valueOf(npc.rewardExp));
			adminReply.replace("%sp%", String.valueOf(npc.rewardSp));
			adminReply.replace("%pAtk%", String.valueOf(npc.basePAtk));
			adminReply.replace("%pDef%", String.valueOf(npc.basePDef));
			adminReply.replace("%mAtk%", String.valueOf(npc.baseMAtk));
			adminReply.replace("%mDef%", String.valueOf(npc.baseMDef));
			adminReply.replace("%pAtkSpd%", String.valueOf(npc.basePAtkSpd));
			adminReply.replace("%aggro%", String.valueOf(npc.aggroRange));
			adminReply.replace("%mAtkSpd%", String.valueOf(npc.baseMAtkSpd));
			adminReply.replace("%rHand%", String.valueOf(npc.rhand));
			adminReply.replace("%lHand%", String.valueOf(npc.lhand));
			adminReply.replace("%armor%", String.valueOf(npc.armor));
			adminReply.replace("%walkSpd%", String.valueOf(npc.baseWalkSpd));
			adminReply.replace("%runSpd%", String.valueOf(npc.baseRunSpd));
			adminReply.replace("%factionId%", npc.factionId == null ? "" : npc.factionId);
			adminReply.replace("%factionRange%", String.valueOf(npc.factionRange));
			adminReply.replace("%isUndead%", npc.isUndead ? "1" : "0");
			adminReply.replace("%absorbLevel%", String.valueOf(npc.absorbLevel));
		}
		else
			adminReply.setHtml("<html><head><body>File not found: data/html/admin/editnpc.htm</body></html>");
		activeChar.sendPacket(adminReply);
	}

	private void save_npc_property(L2PcInstance activeChar, String command)
	{
		String[] commandSplit = command.split(" ");

		if (commandSplit.length < 4)
			return;

		StatsSet newNpcData = new StatsSet();

		try
		{
			newNpcData.set("npcId", commandSplit[1]);

			String statToSet = commandSplit[2];
			//String value = commandSplit[3];
			TextBuilder tv = new TextBuilder(commandSplit[3]);
			if (commandSplit.length > 4)
			{
				for (int i=0;i<commandSplit.length-3;i++)
					tv.append(" " + commandSplit[i+4]);
			}
			String value = tv.toString();
			tv.clear();
			/*String value = commandSplit[3];
			if (commandSplit.length > 4)
			{
				for (int i=0;i<commandSplit.length-3;i++)
					value += " " + commandSplit[i+4];
			}*/
			
			if (statToSet.equals("templateId"))
				newNpcData.set("idTemplate", Integer.valueOf(value));
			else if (statToSet.equals("name"))
				newNpcData.set("name", value);
			else if (statToSet.equals("serverSideName"))
				newNpcData.set("serverSideName", Integer.valueOf(value));
			else if (statToSet.equals("title"))
				newNpcData.set("title", value);
			else if (statToSet.equals("serverSideTitle"))
				newNpcData.set("serverSideTitle", Integer.valueOf(value) == 1 ? 1 : 0);
			else if (statToSet.equals("collisionRadius"))
				newNpcData.set("collision_radius", Integer.valueOf(value));
			else if (statToSet.equals("collisionHeight"))
				newNpcData.set("collision_height", Integer.valueOf(value));
			else if (statToSet.equals("level"))
				newNpcData.set("level", Integer.valueOf(value));
			else if (statToSet.equals("sex"))
			{
				int intValue = Integer.valueOf(value);
				newNpcData.set("sex", intValue == 0 ? "male" : intValue == 1 ? "female" : "etc");
			}
			else if (statToSet.equals("type"))
			{
				Class.forName("ru.agecold.gameserver.model.actor.instance." + value + "Instance");
				newNpcData.set("type", value);
			}
			else if (statToSet.equals("attackRange"))
				newNpcData.set("attackrange", Integer.valueOf(value));
			else if (statToSet.equals("hp"))
				newNpcData.set("hp", Integer.valueOf(value));
			else if (statToSet.equals("mp"))
				newNpcData.set("mp", Integer.valueOf(value));
			else if (statToSet.equals("hpRegen"))
				newNpcData.set("hpreg", Integer.valueOf(value));
			else if (statToSet.equals("mpRegen"))
				newNpcData.set("mpreg", Integer.valueOf(value));
			else if (statToSet.equals("str"))
				newNpcData.set("str", Integer.valueOf(value));
			else if (statToSet.equals("con"))
				newNpcData.set("con", Integer.valueOf(value));
			else if (statToSet.equals("dex"))
				newNpcData.set("dex", Integer.valueOf(value));
			else if (statToSet.equals("int"))
				newNpcData.set("int", Integer.valueOf(value));
			else if (statToSet.equals("wit"))
				newNpcData.set("wit", Integer.valueOf(value));
			else if (statToSet.equals("men"))
				newNpcData.set("men", Integer.valueOf(value));
			else if (statToSet.equals("exp"))
				newNpcData.set("exp", Integer.valueOf(value));
			else if (statToSet.equals("sp"))
				newNpcData.set("sp", Integer.valueOf(value));
			else if (statToSet.equals("pAtk"))
				newNpcData.set("patk", Integer.valueOf(value));
			else if (statToSet.equals("pDef"))
				newNpcData.set("pdef", Integer.valueOf(value));
			else if (statToSet.equals("mAtk"))
				newNpcData.set("matk", Integer.valueOf(value));
			else if (statToSet.equals("mDef"))
				newNpcData.set("mdef", Integer.valueOf(value));
			else if (statToSet.equals("pAtkSpd"))
				newNpcData.set("atkspd", Integer.valueOf(value));
			else if (statToSet.equals("aggro"))
				newNpcData.set("aggro", Integer.valueOf(value));
			else if (statToSet.equals("mAtkSpd"))
				newNpcData.set("matkspd", Integer.valueOf(value));
			else if (statToSet.equals("rHand"))
				newNpcData.set("rhand", Integer.valueOf(value));
			else if (statToSet.equals("lHand"))
				newNpcData.set("lhand", Integer.valueOf(value));
			else if (statToSet.equals("armor"))
				newNpcData.set("armor", Integer.valueOf(value));
			else if (statToSet.equals("runSpd"))
				newNpcData.set("runspd", Integer.valueOf(value));
			else if (statToSet.equals("factionId"))
				newNpcData.set("faction_id", value);
			else if (statToSet.equals("factionRange"))
				newNpcData.set("faction_range", Integer.valueOf(value));
			else if (statToSet.equals("isUndead"))
				newNpcData.set("isUndead", Integer.valueOf(value) == 1 ? 1 : 0);
			else if (statToSet.equals("absorbLevel"))
			{
				int intVal = Integer.valueOf(value);
				newNpcData.set("absorb_level", intVal < 0 ? 0 : intVal > 12 ? 0 : intVal);
			}
		}
		catch (Exception e)
		{
			_log.warning("Error saving new npc value: " + e);
		}

		NpcTable.getInstance().saveNpc(newNpcData);

		int npcId = newNpcData.getInteger("npcId");

		NpcTable.getInstance().reloadNpc(npcId);
		Show_Npc_Property(activeChar, NpcTable.getInstance().getTemplate(npcId));
	}

	private void showNpcDropList(L2PcInstance activeChar, int npcId)
	{
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if(npcData == null)
		{
			activeChar.sendAdmResultMessage("unknown npc template id" + npcId);
			return ;
		}

		NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);

		TextBuilder replyMSG = new TextBuilder("<html><title>NPC: "+ npcData.name + "("+npcData.npcId+") 's drop manage</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Notes: click[drop_id]to show the detail of drop data,click[del] to delete the drop data!");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td>npc_id itemId category</td><td>item[id]</td><td>type</td><td>del</td></tr>");

		for(L2DropCategory cat:npcData.getDropData())
			for(L2DropData drop : cat.getAllDrops())
			{
				replyMSG.append("<tr><td><a action=\"bypass -h admin_edit_drop " + npcData.npcId + " " + drop.getItemId()+ " " + cat.getCategoryType() + "\">"
						+ npcData.npcId + " " + drop.getItemId() + " " + cat.getCategoryType() + "</a></td>" +
						"<td>" + ItemTable.getInstance().getTemplate(drop.getItemId()).getName() + "[" + drop.getItemId() + "]" + "</td><td>" + (drop.isQuestDrop()?"Q":(cat.isSweep()?"S":"D")) + "</td><td>" +
						"<a action=\"bypass -h admin_del_drop " + npcData.npcId + " " + drop.getItemId() +" "+ cat.getCategoryType() +"\">del</a></td></tr>");
			}

		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Add DropData\" action=\"bypass -h admin_add_drop "+ npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center></body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);

	}

	private void showEditDropData(L2PcInstance activeChar, int npcId, int itemId, int category)
	{
		ru.agecold.mysql.Connect con = null;

		try
		{
			con = L2DatabaseFactory.get();

			PreparedStatement statement = con.prepareStatement("SELECT mobId, itemId, min, max, category, chance FROM droplist WHERE mobId=" + npcId + " AND itemId=" + itemId+ " AND category=" + category);
			ResultSet dropData = statement.executeQuery();

			NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);

			TextBuilder replyMSG = new TextBuilder("<html><title>the detail of dropdata: (" + npcId + " " + itemId + " " + category + ")</title>");
			replyMSG.append("<body>");

			if(dropData.next()){
				replyMSG.append("<table>");
				replyMSG.append("<tr><td>Appertain of NPC</td><td>"+ NpcTable.getInstance().getTemplate(dropData.getInt("mobId")).name + "</td></tr>");
				replyMSG.append("<tr><td>ItemName</td><td>"+ ItemTable.getInstance().getTemplate(dropData.getInt("itemId")).getName() + "(" + dropData.getInt("itemId") + ")</td></tr>");
				replyMSG.append("<tr><td>Category</td><td>"+ ((category==-1)?"sweep":Integer.toString(category)) + "</td></tr>");
				replyMSG.append("<tr><td>MIN(" + dropData.getInt("min") + ")</td><td><edit var=\"min\" width=80></td></tr>");
				replyMSG.append("<tr><td>MAX(" + dropData.getInt("max") + ")</td><td><edit var=\"max\" width=80></td></tr>");
				replyMSG.append("<tr><td>CHANCE("+ dropData.getInt("chance") + ")</td><td><edit var=\"chance\" width=80></td></tr>");
				replyMSG.append("</table>");

				replyMSG.append("<center>");
				replyMSG.append("<button value=\"Save Modify\" action=\"bypass -h admin_edit_drop " + npcId + " " + itemId + " " + category +" $min $max $chance\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
				replyMSG.append("<br><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + dropData.getInt("mobId") +"\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
				replyMSG.append("</center>");
			}

			dropData.close();
			statement.close();

			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());

			activeChar.sendPacket(adminReply);
		}
		catch(Exception e){}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	private void showAddDropData(L2PcInstance activeChar, L2NpcTemplate npcData)
	{
		NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);

		TextBuilder replyMSG = new TextBuilder("<html><title>Add dropdata to " + npcData.name + "(" + npcData.npcId + ")</title>");
		replyMSG.append("<body>");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td>Item-Id</td><td><edit var=\"itemId\" width=80></td></tr>");
		replyMSG.append("<tr><td>MIN</td><td><edit var=\"min\" width=80></td></tr>");
		replyMSG.append("<tr><td>MAX</td><td><edit var=\"max\" width=80></td></tr>");
		replyMSG.append("<tr><td>CATEGORY(sweep=-1)</td><td><edit var=\"category\" width=80></td></tr>");
		replyMSG.append("<tr><td>CHANCE(0-1000000)</td><td><edit var=\"chance\" width=80></td></tr>");
		replyMSG.append("</table>");

		replyMSG.append("<center>");
		replyMSG.append("<button value=\"SAVE\" action=\"bypass -h admin_add_drop " + npcData.npcId + " $itemId $category $min $max $chance\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + npcData.npcId +"\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());

		activeChar.sendPacket(adminReply);
	}

	private void updateDropData(L2PcInstance activeChar, int npcId, int itemId, int min, int max, int category, int chance)
	{
		ru.agecold.mysql.Connect con = null;

		try
		{
			con = L2DatabaseFactory.get();

			PreparedStatement statement = con.prepareStatement("UPDATE droplist SET min=?, max=?, chance=? WHERE mobId=? AND itemId=? AND category=?");
			statement.setInt(1, min);
			statement.setInt(2, max);
			statement.setInt(3, chance);
			statement.setInt(4, npcId);
			statement.setInt(5, itemId);
			statement.setInt(6, category);

			statement.execute();
			statement.close();

			PreparedStatement statement2 = con.prepareStatement("SELECT mobId FROM droplist WHERE mobId=? AND itemId=? AND category=?");
			statement2.setInt(1, npcId);
			statement2.setInt(2, itemId);
			statement2.setInt(3, category);

			ResultSet npcIdRs = statement2.executeQuery();
			if(npcIdRs.next()) npcId = npcIdRs.getInt("mobId");
			npcIdRs.close();
			statement2.close();

			if(npcId > 0)
			{
				reLoadNpcDropList(npcId);

				NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
				TextBuilder replyMSG = new TextBuilder("<html><title>Drop data modify complete!</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist "+ npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");

				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
			else
				activeChar.sendAdmResultMessage("unknown error!");
		}
		catch(Exception e){ e.printStackTrace(); }
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	private void addDropData(L2PcInstance activeChar, int npcId, int itemId, int min, int max, int category, int chance)
	{
		ru.agecold.mysql.Connect con = null;

		try
		{
			con = L2DatabaseFactory.get();

			PreparedStatement statement = con.prepareStatement("INSERT INTO droplist(mobId, itemId, min, max, category, chance) values(?,?,?,?,?,?)");
			statement.setInt(1, npcId);
			statement.setInt(2, itemId);
			statement.setInt(3, min);
			statement.setInt(4, max);
			statement.setInt(5, category);
			statement.setInt(6, chance);
			statement.execute();
			statement.close();

			reLoadNpcDropList(npcId);

			NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
			TextBuilder replyMSG = new TextBuilder("<html><title>Add drop data complete!</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Continue add\" action=\"bypass -h admin_add_drop "+ npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("<br><br><button value=\"DropList\" action=\"bypass -h admin_show_droplist "+ npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("</center></body></html>");

			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		catch(Exception e){}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	private void deleteDropData(L2PcInstance activeChar, int npcId, int itemId, int category)
	{
		ru.agecold.mysql.Connect con = null;
		try
		{
			con = L2DatabaseFactory.get();

			if(npcId > 0)
			{
				PreparedStatement statement2 = con.prepareStatement("DELETE FROM droplist WHERE mobId=? AND itemId=? AND category=?");
				statement2.setInt(1, npcId);
				statement2.setInt(2, itemId);
				statement2.setInt(3, category);
				statement2.execute();
				statement2.close();

				reLoadNpcDropList(npcId);

				NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
				TextBuilder replyMSG = new TextBuilder("<html><title>Delete drop data(" + npcId+", "+ itemId+", "+ category + ")complete</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist "+ npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");

				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);

			}
		}
		catch(Exception e){}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

	}

	private void reLoadNpcDropList(int npcId)
	{
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if (npcData == null)
			return;

		// reset the drop lists
		npcData.clearAllDropData();

		// get the drops
		ru.agecold.mysql.Connect con = null;
		try
		{
			con = L2DatabaseFactory.get();
			L2DropData dropData = null;

			npcData.getDropData().clear();

			PreparedStatement statement = con.prepareStatement("SELECT " + L2DatabaseFactory.safetyString(new String[] {"mobId", "itemId", "min", "max", "category", "chance"}) + " FROM droplist WHERE mobId=?");
			statement.setInt(1, npcId);
			ResultSet dropDataList = statement.executeQuery();

			while(dropDataList.next())
			{
				dropData = new L2DropData();

				dropData.setItemId(dropDataList.getInt("itemId"));
				dropData.setMinDrop(dropDataList.getInt("min"));
				dropData.setMaxDrop(dropDataList.getInt("max"));
				dropData.setChance(dropDataList.getInt("chance"));

				int category = dropDataList.getInt("category");
				npcData.addDropData(dropData, category);
			}
			dropDataList.close();
			statement.close();
		}
		catch(Exception e){}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}
}
