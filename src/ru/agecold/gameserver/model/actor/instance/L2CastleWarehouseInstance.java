package ru.agecold.gameserver.model.actor.instance;

import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2Clan;
import ru.agecold.gameserver.network.SystemMessageId;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.network.serverpackets.WareHouseDepositList;
import ru.agecold.gameserver.network.serverpackets.WareHouseWithdrawalList;
import ru.agecold.gameserver.templates.L2NpcTemplate;

/**
 * @author  l3x
 */
public class L2CastleWarehouseInstance extends L2FolkInstance
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;

    /**
     * @param template
     */
    public L2CastleWarehouseInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

    private void showRetrieveWindow(L2PcInstance player)
    {
        player.sendActionFailed();
        player.setActiveWarehouse(player.getWarehouse(), getNpcId());

        if (player.getActiveWarehouse().getSize() == 0)
        {
            player.sendPacket(Static.NO_ITEM_DEPOSITED_IN_WH);
            return;
        }

        player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE));
    }

    private void showDepositWindow(L2PcInstance player)
    {
        player.sendActionFailed();
        player.setActiveWarehouse(player.getWarehouse(), getNpcId());
        player.tempInvetoryDisable();

        player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.PRIVATE));
    }

    private void showDepositWindowClan(L2PcInstance player)
    {
        player.sendActionFailed();
        if (player.getClan() != null)
        {
            if (player.getClan().getLevel() == 0)
            {
                player.sendPacket(Static.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
            }
            else
            {
                if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
                {
                    player.sendPacket(Static.ONLY_CLAN_LEADER_CAN_RETRIEVE_ITEMS_FROM_CLAN_WAREHOUSE);
                }
                player.setActiveWarehouse(player.getClan().getWarehouse(), getNpcId());
                player.tempInvetoryDisable();

                WareHouseDepositList dl = new WareHouseDepositList(player, WareHouseDepositList.CLAN);
                player.sendPacket(dl);
            }
        }
    }

    private void showWithdrawWindowClan(L2PcInstance player) {
        player.sendActionFailed();
        if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE) {
        	player.sendPacket(Static.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE);
        	return;
        } else {
            if (player.getClan().getLevel() == 0) {
                player.sendPacket(Static.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
            } else {
                player.setActiveWarehouse(player.getClan().getWarehouse(), getNpcId());
                player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
            }
        }
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (player.getActiveEnchantItem() != null) {
            _log.info("Player " + player.getName() + " trying to use enchant exploit, ban this player!");
            player.closeNetConnection();
            return;
        }

        if (command.startsWith("WithdrawP"))
            showRetrieveWindow(player);
        else if (command.equals("DepositP"))
            showDepositWindow(player);
        else if (command.equals("WithdrawC"))
            showWithdrawWindowClan(player);
        else if (command.equals("DepositC"))
            showDepositWindowClan(player);
        else if (command.startsWith("Chat"))
        {
            int val = 0;
            try
            {
               val = Integer.parseInt(command.substring(5));
            }
            catch (IndexOutOfBoundsException ioobe){}
            catch (NumberFormatException nfe){}
            showChatWindow(player, val);
        }
        else
            super.onBypassFeedback(player, command);
    }

    @Override
    public void showChatWindow(L2PcInstance player, int val) {
        player.sendActionFailed();
        String filename = "data/html/castlewarehouse/castlewarehouse-no.htm";

        int condition = validateCondition(player);
        if (condition > COND_ALL_FALSE) {
            if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
                filename = "data/html/castlewarehouse/castlewarehouse-busy.htm"; // Busy because of siege
            else if (condition == COND_OWNER)                                    // Clan owns castle
            {
                if (val == 0)
                    filename = "data/html/castlewarehouse/castlewarehouse.htm";
                else
                    filename = "data/html/castlewarehouse/castlewarehouse-" + val + ".htm";
            }
        }

        NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%npcname%", getName());
        player.sendPacket(html);
    }

    protected int validateCondition(L2PcInstance player)
    {
        if (player.isGM()) return COND_OWNER;
        if (getCastle() != null && getCastle().getCastleId() > 0)
        {
            if (player.getClan() != null)
            {
                if (getCastle().getSiege().getIsInProgress())
                    return COND_BUSY_BECAUSE_OF_SIEGE;                   // Busy because of siege
                else if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
                    return COND_OWNER;
            }
        }
        return COND_ALL_FALSE;
    }
}
