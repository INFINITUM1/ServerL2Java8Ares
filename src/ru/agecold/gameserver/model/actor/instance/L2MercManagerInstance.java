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
package ru.agecold.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import ru.agecold.Config;
import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.TradeController;
import ru.agecold.gameserver.model.L2Clan;
import ru.agecold.gameserver.model.L2TradeList;
import ru.agecold.gameserver.network.serverpackets.BuyList;
import ru.agecold.gameserver.network.serverpackets.MyTargetSelected;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.agecold.gameserver.network.serverpackets.ValidateLocation;
import ru.agecold.gameserver.templates.L2NpcTemplate;

public final class L2MercManagerInstance extends L2FolkInstance
{
    //private static Logger _log = Logger.getLogger(L2MercManagerInstance.class.getName());

    private static final int COND_ALL_FALSE = 0;
    private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
    private static final int COND_OWNER = 2;

    public L2MercManagerInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player)) return;
		player.setLastFolkNPC(this);

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			} 
			else 
			{
				showMessageWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendActionFailed();
	}

    @Override
    public void onBypassFeedback(L2PcInstance player, String command)
    {
        int condition = validateCondition(player);
        if (condition <= COND_ALL_FALSE) return;

        if (condition == COND_BUSY_BECAUSE_OF_SIEGE) return;
        else if (condition == COND_OWNER)
        {
            StringTokenizer st = new StringTokenizer(command, " ");
            String actualCommand = st.nextToken(); // Get actual command

            String val = "";
            if (st.countTokens() >= 1)
            {
                val = st.nextToken();
            }

            if (actualCommand.equalsIgnoreCase("hire"))
            {
                if (val == "") return;

                showBuyWindow(player, Integer.parseInt(val));
                return;
            }
        }

        super.onBypassFeedback(player, command);
    }

    private void showBuyWindow(L2PcInstance player, int val)
    {
        player.tempInvetoryDisable();
       // if (Config.DEBUG) _log.fine("Showing buylist");
        L2TradeList list = TradeController.getInstance().getBuyList(val);
        if ((list != null) && (list.getNpcId().equals(String.valueOf(getNpcId()))))
        {
            BuyList bl = new BuyList(list, player.getAdena(), 0);
            player.sendPacket(bl);
        }
        else
        {
            _log.warning("possible client hacker: " + player.getName()
                + " attempting to buy from GM shop! < Ban him!");
            _log.warning("buylist id:" + val);
        }
    }

    public void showMessageWindow(L2PcInstance player)
    {
        String filename = "data/html/mercmanager/mercmanager-no.htm";

        int condition = validateCondition(player);
        if (condition == COND_BUSY_BECAUSE_OF_SIEGE) filename = "data/html/mercmanager/mercmanager-busy.htm"; // Busy because of siege
        else if (condition == COND_OWNER) // Clan owns castle
            filename = "data/html/mercmanager/mercmanager.htm"; // Owner message window

        NpcHtmlMessage html = NpcHtmlMessage.id(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%npcId%", String.valueOf(getNpcId()));
        html.replace("%npcname%", getName());
        player.sendPacket(html);
    }

    private int validateCondition(L2PcInstance player)
    {
        if (getCastle() != null && getCastle().getCastleId() > 0)
        {
            if (player.getClan() != null)
            {
                if (getCastle().getSiege().getIsInProgress()) return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
                else if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
                {
                    if ((player.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) == L2Clan.CP_CS_MERCENARIES) return COND_OWNER;
                }
            }
        }

        return COND_ALL_FALSE;
    }
}