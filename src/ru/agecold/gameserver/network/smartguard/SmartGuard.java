package ru.agecold.gameserver.network.smartguard;

import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.network.GameCrypt;
import ru.agecold.gameserver.network.smartguard.integration.PWDatabaseConnection;
import ru.agecold.gameserver.network.smartguard.integration.SmartPlayer;
import ru.agecold.gameserver.network.smartguard.menu.SmartGuardMenu;
import scripts.commands.AdminCommandHandler;
import smartguard.api.ISmartGuardService;
import smartguard.api.integration.ISmartPlayer;
import smartguard.api.integration.IWorldService;
import smartguard.core.properties.GuardProperties;
import smartguard.core.utils.LogUtils;
import smartguard.spi.SmartGuardSPI;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class SmartGuard
{
	public static void load()
	{
		try
		{
			if(!SmartGuard.class.getProtectionDomain().getCodeSource().equals(GameCrypt.class.getProtectionDomain().getCodeSource()))
			{
				System.out.println("Error! Class [SmartGuard] is not first in your classpath list, SmartGuard will not work properly!");
				return;
			}
		}
		catch(Exception e)
		{}

		SmartGuardSPI.setDatabaseService(() ->
		{
			try
			{
				return new PWDatabaseConnection(L2DatabaseFactory.get());
			}
			catch(SQLException e)
			{
				LogUtils.log("Failed to obtain a new DB connection for smartguard! ", e);
				return null;
			}
		});
		SmartGuardSPI.setWorldService(new IWorldService()
		{
			public ISmartPlayer getPlayerByObjectId(final int i)
			{
				return new SmartPlayer(L2World.getInstance().getPlayer(i));
			}

			@Override
			public List<ISmartPlayer> getAllPlayers()
			{
				return L2World.getInstance().getAllPlayers().stream().map(SmartPlayer::new).collect(Collectors.toList());
			}
		});
		final ISmartGuardService svc = SmartGuardSPI.getSmartGuardService();
		if(!svc.init())
		{
			System.out.println("Failed to init SmartGuard!");
			return;
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> SmartGuardSPI.getSmartGuardService().getSmartGuardBus().onShutdown()));

		SmartGuardSPI.getSmartGuardService().getSmartGuardBus().onStartup();
		try
		{
			AdminCommandHandler.getInstance().registerAdminCommandHandler(new SmartGuardMenu());
		}
		catch(Exception e)
		{
			LogUtils.log("Error initializing SmartGuard AdminCommandHandler!");
			LogUtils.log(e);
		}
	}

	public static boolean isActive()
	{
		return false /*GuardProperties.ProtectionEnabled*/;
	}
}
