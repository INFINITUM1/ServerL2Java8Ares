package ru.agecold.gameserver.model.entity.olympiad;

import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.Announcements;
import ru.agecold.gameserver.cache.Static;

class CompStartTask implements Runnable
{
	@Override
	public void run()
	{
		if(Olympiad.isOlympiadEnd())
			return;

		Olympiad._manager = new OlympiadManager();
		Olympiad._inCompPeriod = true;

		new Thread(Olympiad._manager).start();

		ThreadPoolManager.getInstance().scheduleGeneral(new CompEndTask(), Olympiad.getMillisToCompEnd());

		Announcements.getInstance().announceToAll(Static.THE_OLYMPIAD_GAME_HAS_STARTED);
		Olympiad._log.info("Olympiad System: Olympiad Game Started");
	}
}