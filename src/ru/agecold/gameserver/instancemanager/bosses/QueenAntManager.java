package ru.agecold.gameserver.instancemanager.bosses;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.datatables.SpawnTable;
import ru.agecold.gameserver.instancemanager.EventManager;
import ru.agecold.gameserver.instancemanager.GrandBossManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Spawn;
import ru.agecold.gameserver.model.L2Summon;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;
import ru.agecold.util.Location;
import ru.agecold.util.log.AbstractLogger;
import ru.agecold.util.Rnd;
import ru.agecold.util.TimeLogger;
import scripts.ai.QueenAnt;

public class QueenAntManager extends GrandBossManager
{
	private static final Logger _log = AbstractLogger.getLogger(QueenAntManager.class.getName());
	
	private static final int BOSS = 29001;
	private QueenAnt self = null;
	private boolean _enter = false;
	
    private static QueenAntManager _instance;
    public static final QueenAntManager getInstance()
    {
        return _instance;
    }
	
	static class Status
	{
		public int status;
		public long respawn;
		public boolean wait = true;
		public boolean spawned = false;
		
		public Status(int status, long respawn)
		{
			this.status = status;
			this.respawn = respawn;
		}
	}
	private static Status _status;
	
	class ManageBoss implements Runnable
	{
		ManageBoss()
		{
		}

		public void run()
		{	
			long delay = 0;
			if (_status.respawn > 0)
				delay = _status.respawn - System.currentTimeMillis();

			if(delay <= 0)
				spawnBoss();
			else
				ThreadPoolManager.getInstance().scheduleGeneral(new SpawnBoss(), delay);
			_status.wait = false;
		}
	}
	
	class SpawnBoss implements Runnable
	{
		SpawnBoss()
		{
		}

		public void run()
		{	
			spawnBoss();
		}
	}

	public static void init()
	{
        _instance = new QueenAntManager();
        _instance.load();
	}

	public void load()
	{
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT spawn_date, status FROM grandboss_data WHERE boss_id=?");
			st.setInt(1, BOSS);
			rs = st.executeQuery();
			if (rs.next())
			{
				int status = rs.getInt("status");
				long respawn = rs.getLong("spawn_date");
				
				if (status > 1)
					status = 1;
				
				if (respawn > 0)
					status = 0;
				
				_status = new Status(status, respawn);
			}
		}
		catch (SQLException e)
		{
			_log.warning("QueenAntManager, failed to load: " + e);
			e.getMessage();
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
		
		switch(_status.status)
		{
			case 0:
				String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(_status.respawn));
				_log.info("QueenAnt: dead; spawn date: " + date);
				break;
			case 1:
				_log.info("QueenAnt: live; farm delay: " + (Config.AQ_RESTART_DELAY / 60000) + "min.");
				break;
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new ManageBoss(), Config.AQ_RESTART_DELAY);
	}
	
	public void spawnBoss()
	{
		setState(1, 0);
		self = (QueenAnt) createOnePrivateEx(BOSS, -21468, 181638, -5720, 10836);
		self.setRunning();
	}
	
	public void setState(int status, long respawn)
	{
		_status.status = status;
		_status.respawn = respawn;
		
		Connect con = null;
		PreparedStatement statement = null;
        try
        {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("UPDATE `grandboss_data` SET `status`=?, `spawn_date`=? WHERE `boss_id`=?");
            statement.setInt(1, status);
            statement.setLong(2, respawn);
            statement.setInt(3, BOSS);
            statement.executeUpdate();
        } 
		catch (SQLException e) 
		{
			_log.warning("QueenAntManager, could not set QueenAnt status" + e);
			e.getMessage();
        } 
		finally
		{
			Close.CS(con, statement);
		}
		
		switch(status)
		{
			case 0:
				ThreadPoolManager.getInstance().scheduleGeneral(new SpawnBoss(), (respawn - System.currentTimeMillis()));
				String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(respawn));
				_log.info("QueenAntManager, QueenAnt status: 0; killed, respawn date: " + date);
				break;
			case 1:
				if (Config.ANNOUNCE_EPIC_STATES)
					EventManager.getInstance().announce(Static.ANTQUEEN_SPAWNED);
				_log.info("QueenAntManager, QueenAnt status: 1; spawned.");
				break;
		}
	}

	public int getStatus()
	{
		if (_status.wait)
			return 0;

		return _status.status;
	}
	
	public boolean spawned()
	{
		return _status.spawned;
	}
	
	public void setSpawned()
	{
		_status.spawned = true;
	}
	
	public void notifyDie()
	{
		if (self == null)
			return;

		self = null;
		_status.spawned = false;

		long offset = (Config.AQ_MIN_RESPAWN + Config.AQ_MAX_RESPAWN) / 2;
		setState(0, (System.currentTimeMillis() + offset));

		if (Config.ANNOUNCE_EPIC_STATES)
			EventManager.getInstance().announce(Static.ANTQUEEN_DIED);
	}
}