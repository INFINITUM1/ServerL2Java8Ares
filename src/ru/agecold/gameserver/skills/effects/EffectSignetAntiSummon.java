package ru.agecold.gameserver.skills.effects;

import ru.agecold.gameserver.model.L2Effect;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.skills.Env;

final class EffectSignetAntiSummon extends L2Effect
{
	public EffectSignetAntiSummon(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#getEffectType()
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SIGNET_GROUND;
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onStart()
	 */
	@Override
	public void onStart()
	{
		if (!getEffected().isPlayer())
			return;
			
		L2PcInstance pc = (L2PcInstance) getEffected();	
		pc.setNoSummon(true);
		if (pc.getPet() != null)
			pc.getPet().unSummon(pc);
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onActionTime()
	 */
	@Override
	public boolean onActionTime()
	{
		if (!getEffected().isPlayer())
			return false;
			
		((L2PcInstance) getEffected()).setNoSummon(false);
		return false;
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onExit()
	 */
	@Override
	public void onExit()
	{
	}
}
