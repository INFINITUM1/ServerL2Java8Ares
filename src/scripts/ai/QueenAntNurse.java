package scripts.ai;

import javolution.util.FastList;

import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.instancemanager.GrandBossManager;
import ru.agecold.gameserver.instancemanager.bosses.QueenAntManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.actor.instance.L2MonsterInstance;
import ru.agecold.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.quest.jython.QuestJython;
import ru.agecold.gameserver.network.serverpackets.CharMoveToLocation;
import ru.agecold.gameserver.network.serverpackets.SocialAction;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.gameserver.util.Util;
import ru.agecold.util.Rnd;

public class QueenAntNurse extends L2MonsterInstance
{
	private static QueenAnt aq = null;
	private static QueenAntLarva larva = null;
	
	private boolean process = false;
	
	public QueenAntNurse(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
    @Override
	public void onSpawn()
	{
		setRunning();
		ThreadPoolManager.getInstance().scheduleAi(new Heal(), 10000, false);
	} 
	
	public void setAq(QueenAnt aq)
	{
		this.aq = aq;
	}
	
	public void setLarva(QueenAntLarva larva)
	{
		this.larva = larva;
	}
	
    @Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
    {
        super.reduceCurrentHp(damage, attacker, awake);
	} 
	
    @Override
	public boolean doDie(L2Character killer)
    {
    	super.doDie(killer);
		if (getSpawn() != null)
			getSpawn().setLastKill(System.currentTimeMillis());
		return true;
	}
	
    @Override
	public void deleteMe()
    {
        super.deleteMe();
    }
	
	class Heal implements Runnable
	{
		Heal()
		{
		}

		public void run()
		{
			if (aq == null || aq.isDead())
			{
				deleteMe();
				return;
			}
			if (process)
			{
				ThreadPoolManager.getInstance().scheduleAi(new Heal(), 10000, false);
				return;
			}
			
			if(larva != null && larva.getCurrentHp() < larva.getMaxHp())
			{
				process = true;
				doHeal(larva);
			}
			else if(aq.getCurrentHp() < aq.getMaxHp())
			{
				process = true;
				doHeal(aq);
			}
			ThreadPoolManager.getInstance().scheduleAi(new Heal(), 10000, false);
		}
	}

	private void doHeal(L2Object trg)
	{		
		if (aq == null || aq.isDead())
		{
			deleteMe();
			return;
		}

		if (!Util.checkIfInRange(800, this, trg, false)) 
		{
			moveToLocationm(trg.getX() + Rnd.get(150), trg.getY() + Rnd.get(150), trg.getZ(), 0);
			broadcastPacket(new CharMoveToLocation(this));
		}
		else
		{
			setTarget(trg);
			addUseSkillDesire(4020, 1);
		}
		process = false;
	}
}
