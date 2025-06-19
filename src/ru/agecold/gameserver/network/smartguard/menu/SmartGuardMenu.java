package ru.agecold.gameserver.network.smartguard.menu;

import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.smartguard.integration.SmartPlayer;
import scripts.commands.IAdminCommandHandler;
import smartguard.core.manager.admin.GmCommandHandler;

public class SmartGuardMenu extends GmCommandHandler implements IAdminCommandHandler
{
    @Override
	public String[] getAdminCommandList()
	{
		return getCommands();
	}

	@Override
	public boolean useAdminCommand(String commands, L2PcInstance player)
	{
		try
		{
			String[] strings = commands.split(" ");
			handle(new SmartPlayer(player), strings[0], strings);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
}
