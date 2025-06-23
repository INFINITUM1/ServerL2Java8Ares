package ru.agecold.gameserver.network.smartguard.integration;

import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import smartguard.api.integration.AbstractSmartClient;
import smartguard.api.integration.ISmartPlayer;

public class SmartPlayer implements ISmartPlayer
{
	private L2PcInstance player;
	private SmartClient smartClient;

	public SmartPlayer(L2PcInstance player, SmartClient smartClient)
	{
		this.player = player;
		this.smartClient = smartClient;
	}

	public SmartPlayer(L2PcInstance player)
	{
		this.player = player;
		this.smartClient = new SmartClient(player.getClient());
	}

	@Override
	public String getName()
	{
		return player.getName();
	}

	@Override
	public int getObjectId()
	{
		return player.getObjectId();
	}

	@Override
	public boolean isAdmin()
	{
		return player.isGM();
	}

	@Override
	public AbstractSmartClient getClient()
	{
		return smartClient;
	}
}
