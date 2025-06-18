package scripts.ai;

import javolution.util.FastList;

import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.instancemanager.GrandBossManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2GrandBossInstance;
import ru.agecold.gameserver.model.actor.instance.L2MonsterInstance;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.model.quest.jython.QuestJython;
import ru.agecold.gameserver.network.serverpackets.CharMoveToLocation;
import ru.agecold.gameserver.network.serverpackets.SocialAction;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.gameserver.util.Util;
import ru.agecold.util.Rnd;

public class QueenAntLarva extends L2MonsterInstance
{
	public QueenAntLarva(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
    @Override
	public void onSpawn()
	{ 
    	super.onSpawn();
		setIsImobilised(true);
	} 
	
    @Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
    {
        super.reduceCurrentHp(damage, attacker, awake);
		
		if(attacker.isPlayer() && (attacker.getLevel() > getLevel() + 8))
		{
			if (((L2PcInstance)attacker).isMageClass())
				SkillTable.getInstance().getInfo(4215, 1).getEffects(attacker, attacker);
			else
				SkillTable.getInstance().getInfo(4515, 1).getEffects(attacker, attacker);
		}
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
}
