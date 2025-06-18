package ru.agecold.gameserver.skills.effects;

import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Effect;
import ru.agecold.gameserver.skills.Env;
import javolution.util.FastTable;

final class EffectSignetNoise extends L2Effect
{
	public EffectSignetNoise(Env env, EffectTemplate template)
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
		FastTable<L2Effect> effects = getEffected().getAllEffectsTable();
		for (int i = 0, n = effects.size(); i < n; i++) 
		{
			L2Effect e = effects.get(i);
			if (e == null)
				continue;
				
            if (e.getSkill().isDance())
				e.exit();
        }
	}
	
	/**
	 * 
	 * @see ru.agecold.gameserver.model.L2Effect#onActionTime()
	 */
	@Override
	public boolean onActionTime()
	{
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
