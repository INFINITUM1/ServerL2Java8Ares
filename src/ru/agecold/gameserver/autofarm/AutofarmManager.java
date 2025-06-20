package ru.agecold.gameserver.autofarm;

import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class AutofarmManager {

	private final Long iterationSpeedMs = 450L;

	private final ConcurrentHashMap<Integer, AutofarmPlayerRoutine> activeFarmers = new ConcurrentHashMap<>();
	private ScheduledFuture<?> onUpdateTask;

	private Runnable onUpdate() {
		return () -> activeFarmers.forEach((integer, autofarmPlayerRoutine) -> autofarmPlayerRoutine.executeRoutine());
	}

	public void startFarm(L2PcInstance player){
		if(isAutofarming(player)) {
			player.sendMessage("You are already autofarming");
			return;
		}

		activeFarmers.put(player.getObjectId(), new AutofarmPlayerRoutine(player));
		onUpdateTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(onUpdate(), 1000, iterationSpeedMs);
		player.sendMessage("Autofarming activated");
	}

	public void stopFarm(L2PcInstance player){
		if(!isAutofarming(player)) {
			player.sendMessage("You are not autofarming");
			return;
		}

		activeFarmers.remove(player.getObjectId());
		cancelUpdateTask();
		player.sendMessage("Autofarming deactivated");
	}

	public void toggleFarm(L2PcInstance player){
		if(isAutofarming(player)) {
			stopFarm(player);
			return;
		}
		startFarm(player);
	}

	public Boolean isAutofarming(L2PcInstance player){
		return activeFarmers.containsKey(player.getObjectId());
	}

	public void onPlayerLogout(L2PcInstance player){
		stopFarm(player);
	}

	public void onDeath(L2PcInstance player) {
		if(isAutofarming(player)) {
			cancelUpdateTask();
			activeFarmers.remove(player.getObjectId());
		}
	}

	public void cancelUpdateTask()
	{
		if (onUpdateTask != null) {
			onUpdateTask.cancel(false);
			onUpdateTask = null;
		}
	}

	public static final AutofarmManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final AutofarmManager INSTANCE = new AutofarmManager();
	}
}