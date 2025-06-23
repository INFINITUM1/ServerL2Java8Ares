package ru.agecold.gameserver.network.smartguard.integration;

import ru.agecold.gameserver.network.L2GameClient;
import ru.agecold.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.agecold.gameserver.network.serverpackets.ServerClose;
import ru.agecold.gameserver.network.serverpackets.SystemMessage;
import ru.agecold.gameserver.network.smartguard.packet.RawPacket;
import smartguard.api.entity.IHwidEntity;
import smartguard.api.integration.AbstractSmartClient;
import smartguard.api.integration.ISmartPlayer;
import smartguard.api.network.NetworkStatus;

import java.nio.ByteBuffer;

public class SmartClient extends AbstractSmartClient
{
	private L2GameClient client;

	public SmartClient(final L2GameClient client)
	{
		this.client = client;
	}

	@Override
	public String getAccountName()
	{
		return client.getAccountName();
	}

	@Override
	public void closeConnection(final boolean sendServerClose, final boolean defer)
	{
		if(sendServerClose)
		{
			client.close(new ServerClose());
		}
		else if(defer)
		{
			client.closeLater();
		}
		else
		{
			client.closeNow();
		}
	}

	@Override
	public NetworkStatus getConnectionStatus()
	{
		if(client.getConnection() == null || client.getConnection().getSocket() == null)
		{
			return NetworkStatus.DISCONNECTED;
		}
		switch(client.getState())
		{
			case CONNECTED:
			case AUTHED:
			{
				return NetworkStatus.CONNECTED;
			}
			case IN_GAME:
			{
				return NetworkStatus.IN_GAME;
			}
			default:
			{
				return NetworkStatus.DISCONNECTED;
			}
		}
	}

	@Override
	public void setHwid(final IHwidEntity iHwidEntity)
	{
		client.setHWID(iHwidEntity.getPlain());
	}

	@Override
	public void sendRawPacket(final ByteBuffer byteBuffer)
	{
		client.sendPacket(new RawPacket(byteBuffer));
	}

	@Override
	public void closeWithRawPacket(final ByteBuffer byteBuffer)
	{
		client.close(new RawPacket(byteBuffer));
	}

	@Override
	public void sendHtml(final String s)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(5, s);
		client.sendPacket(html);
	}

	@Override
	public void sendMessage(final String s)
	{
		client.sendPacket(SystemMessage.sendString(s));
	}

	@Override
	public String getIpAddr()
	{
		return client.getConnection().getSocket().getInetAddress().getHostAddress();
	}

	@Override
	public Object getNativeClient()
	{
		return client;
	}

	@Override
	public ISmartPlayer getPlayer()
	{
		if(client.getActiveChar() == null)
		{
			return null;
		}
		return new SmartPlayer(client.getActiveChar(), this);
	}
}