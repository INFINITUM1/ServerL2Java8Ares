package ru.agecold.gameserver.taskmanager.tasks;

import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.model.entity.olympiad.OlympiadDatabase;
import ru.agecold.gameserver.taskmanager.Task;
import ru.agecold.gameserver.taskmanager.TaskManager;
import ru.agecold.gameserver.taskmanager.TaskTypes;
import ru.agecold.gameserver.taskmanager.TaskManager.ExecutedTask;
import ru.agecold.util.log.AbstractLogger;

/**
 * Updates all data of Olympiad nobles in db
 *
 * @author godson
 */
public class TaskOlympiadSave extends Task
{
	private static final Logger _log = AbstractLogger.getLogger(TaskOlympiadSave.class.getName());
	public static final String NAME = "OlympiadSave";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		try
		{
			OlympiadDatabase.save();
			_log.info("Olympiad System: Data updated successfully.");
		}
		catch(Exception e)
		{
			_log.warning("Olympiad System: Failed to save Olympiad configuration: " + e);
		}
	}

	@Override
	public void initializate()
	{
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "0", "600000", "");
	}
}
