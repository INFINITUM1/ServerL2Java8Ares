package ru.agecold.gameserver.model.entity.olympiad;

import java.util.Calendar;

import ru.agecold.Config;

public class WeeklyTask implements Runnable
{
	@Override
	public void run()
	{
		Olympiad.addWeeklyPoints();
		Olympiad._log.info("Olympiad System: Added weekly points to nobles");

		Calendar nextChange = Calendar.getInstance();
		Olympiad._nextWeeklyChange = nextChange.getTimeInMillis() + Config.ALT_OLY_WPERIOD;
	}
}