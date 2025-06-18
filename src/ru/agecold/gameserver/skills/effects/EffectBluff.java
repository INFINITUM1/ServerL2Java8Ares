
package ru.agecold.gameserver.skills.effects;

import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.model.L2CharPosition;
import ru.agecold.gameserver.model.L2Effect;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.serverpackets.BeginRotation;
import ru.agecold.gameserver.network.serverpackets.StopRotation;
import ru.agecold.gameserver.skills.Env;
import ru.agecold.gameserver.skills.Stats;
import ru.agecold.util.Rnd;

/**
 * @author decad
 *
 * Implementation of the Bluff Effect
 */
final class EffectBluff extends L2Effect {

    public EffectBluff(Env env, EffectTemplate template)
    {
        super(env, template);
    }

    @Override
	public EffectType getEffectType()
    {
        return EffectType.BLUFF; //test for bluff effect
    }

    /** Notify started */
    @Override
	public void onStart()
    {
    	if(getEffected().isRaid())
			return;

		getEffected().broadcastPacket(new BeginRotation(getEffected(), getEffected().getHeading(), 1, 65535));
		getEffected().broadcastPacket(new StopRotation(getEffected(), getEffector().getHeading(), 65535));
		getEffected().setHeading(getEffector().getHeading());
		getEffected().startStunning();

		/* жалобы от игроков
		boolean eff = true;
		// разворот и сбивка таргета
		if (Rnd.get(100) < (getEffected().calcStat(Stats.SLEEP_VULN, getEffected().getTemplate().baseSleepVuln, getEffected(), null) * 100))
		{
			getEffected().broadcastPacket(new BeginRotation(getEffected(), getEffected().getHeading(), 1, 65535));
			getEffected().broadcastPacket(new StopRotation(getEffected(), getEffector().getHeading(), 65535));
			getEffected().setHeading(getEffector().getHeading());
			if (Rnd.get(100) < 50)
				getEffected().setTarget(null);
		}

		//стун
		if (Rnd.get(100) < (getEffected().calcStat(Stats.STUN_VULN, getEffected().getTemplate().baseStunVuln, getEffected(), null) * 100))
		{
			eff = false;
			getEffected().startStunning();
		}

		if (eff)
			onActionTime();*/
    }

    @Override
	public void onExit()
    {
		getEffected().stopStunning(this);
    }

    @Override
	public boolean onActionTime()
    {
		onExit();
        return false;
    }
}
