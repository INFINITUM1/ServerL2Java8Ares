package ru.agecold.gameserver.autofarm;

import ru.agecold.Config;
import ru.agecold.gameserver.GeoEngine;
import ru.agecold.gameserver.ai.CtrlEvent;
import ru.agecold.gameserver.ai.CtrlIntention;
import ru.agecold.gameserver.cache.Static;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2Object;
import ru.agecold.gameserver.model.L2ShortCut;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.L2WorldRegion;
import ru.agecold.gameserver.model.actor.instance.L2MonsterInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.clientpackets.UseItem;
import ru.agecold.gameserver.skills.Formulas;
import ru.agecold.gameserver.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AutofarmPlayerRoutine {

	protected static final Logger _log = Logger.getLogger(Config.class.getName());

	private final L2PcInstance player;
	private L2Character committedTarget = null;

	public AutofarmPlayerRoutine(L2PcInstance player){
		this.player = player;
	}

	public void executeRoutine(){
		checkSpoil();
		targetEligibleCreature();
		checkManaPots();
		checkHealthPots();
		attack();
		checkSpoil();
	}

	private void attack() {
		//_log.warning("AutofarmPlayerRoutine.attack#1");
		boolean shortcutsContainAttack = shotcutsContainAttack();
		if(shortcutsContainAttack) {
			//_log.warning("AutofarmPlayerRoutine.attack#2");
			physicalAttack();
		}

		//_log.warning("AutofarmPlayerRoutine.attack#3");
		useAppropriateSpell();
		//_log.warning("AutofarmPlayerRoutine.attack#4");

		if(shortcutsContainAttack) {
			//_log.warning("AutofarmPlayerRoutine.attack#5");
			physicalAttack();
		}
		//_log.warning("AutofarmPlayerRoutine.attack#6");
	}

	private void useAppropriateSpell() {
		L2Skill chanceSkill = nextAvailableSkill(getChanceSpells(), AutofarmSpellType.Chance);
		//_log.warning("AutofarmPlayerRoutine.useAppropriateSpell#1");
		if(chanceSkill != null) {
			//_log.warning("AutofarmPlayerRoutine.useAppropriateSpell#2 " + chanceSkill);
			useMagicSkill(chanceSkill, false);
			return;
		}
		L2Skill lowLifeSkill = nextAvailableSkill(getLowLifeSpells(), AutofarmSpellType.LowLife);
		//_log.warning("AutofarmPlayerRoutine.useAppropriateSpell#3");

		if(lowLifeSkill != null) {
			//_log.warning("AutofarmPlayerRoutine.useAppropriateSpell#4 " + lowLifeSkill);
			useMagicSkill(lowLifeSkill, false);
			return;
		}

		L2Skill selfSkills = nextAvailableSkill(getSelfSpells(), AutofarmSpellType.Self);
		//_log.warning("AutofarmPlayerRoutine.useAppropriateSpell#5");

		if(selfSkills != null) {
			//_log.warning("AutofarmPlayerRoutine.useAppropriateSpell#6 " + selfSkills);
			useMagicSkill(selfSkills, true);
			return;
		}

		L2Skill attackSkill = nextAvailableSkill(getAttackSpells(), AutofarmSpellType.Attack);
		//_log.warning("AutofarmPlayerRoutine.useAppropriateSpell#7 " + getAttackSpells().toString());

		if(attackSkill != null) {
			//_log.warning("AutofarmPlayerRoutine.useAppropriateSpell#8 " + attackSkill);
			useMagicSkill(attackSkill, false);
			return;
		}
	}

	public L2Skill nextAvailableSkill(List<Integer> skillIds, AutofarmSpellType spellType) {
		//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#1");
		for (Integer skillId : skillIds) {
			//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#2 " + skillId);
			L2Skill skill = player.getKnownSkill(skillId);

			if(skill == null)
				continue;
			//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#3");
			// TODO [V] - что тут должно быть?
			if(!checkDoCastConditions(skill))
			{
				//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#4");
				continue;
			}
			//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#5");
			if(spellType == AutofarmSpellType.Chance && getMonsterTarget() != null) {
				//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#6");
				if(isSpoil(skillId)) {
					//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#7");
					if(monsterIsAlreadySpoiled()) {
						//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#8");
						continue;
					} else {
						//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#9");
						return skill;
					}
				}
				//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#10");
				if(getMonsterTarget().getFirstEffect(skillId) == null) {
					//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#11");
					return skill;
				} else {
					//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#12");
					continue;
				}
			}
			//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#13");
			if(spellType == AutofarmSpellType.LowLife && getMonsterTarget() != null && getHpPercentage() > AutofarmConstants.lowLifePercentageThreshold) {
				//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#14");
				break;
			}
			//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#15");
			if(spellType == AutofarmSpellType.Self) {
				//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#16");
				if(skill.isToggle() && player.getFirstEffect(skillId) == null)
				{
					//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#17");
					return skill;
				}
				//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#18");
				if(player.getFirstEffect(skillId) == null) {
					//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#19");
					return skill;
				}
				//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#20");

				continue;
			}
			//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#21");
			return skill;
		}
		//_log.warning("AutofarmPlayerRoutine.nextAvailableSkill#22");
		return null;
	}

	private boolean checkDoCastConditions(L2Skill skill)
	{
		//_log.warning("AutofarmPlayerRoutine.checkDoCastConditions");
		if (player.isHippy() || skill == null) {
			player.getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return false;
		}

		if (skill.isMagic() && player.isMuted() && !skill.isPotion()) {
			player.getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return false;
		}
		if (!skill.isMagic() && player.isPsychicalMuted() && !skill.isPotion()) {
			player.getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return false;
		}

		return true;
	}

	private void checkHealthPots() {
		if(getHpPercentage() <= AutofarmConstants.useHpPotsPercentageThreshold) {
			if(player.getFirstEffect(AutofarmConstants.hpPotSkillId) != null) {
				return;
			}
			L2ItemInstance hpPots = player.getInventory().getItemByItemId(AutofarmConstants.hpPotItemId);
			if(hpPots != null) {
				useItem(hpPots);
			}
		}
	}

	private void checkManaPots() {
		if(getMpPercentage() <= AutofarmConstants.useMpPotsPercentageThreshold) {
			L2ItemInstance mpPots = player.getInventory().getItemByItemId(AutofarmConstants.mpPotItemId);
			if(mpPots != null) {
				useItem(mpPots);
			}
		}
	}

	private void checkSpoil() {
		if(canBeSweepedByMe() && getMonsterTarget().isDead()) {
			L2Skill sweeper = player.getKnownSkill(42);
			if(sweeper == null)
				return;

			useMagicSkill(sweeper, false);
		}
	}

	private Double getHpPercentage() {
		return player.getCurrentHp() * 100.0f / player.getMaxHp();
	}

	private Double getMpPercentage() {
		return player.getCurrentMp() * 100.0f / player.getMaxMp();
	}

	private boolean canBeSweepedByMe() {
		return getMonsterTarget() != null && getMonsterTarget().isDead() && getMonsterTarget().getIsSpoiledBy() == player.getObjectId();
	}

	private boolean monsterIsAlreadySpoiled() {
		return getMonsterTarget() != null && getMonsterTarget().getIsSpoiledBy() != 0;
	}

	private boolean isSpoil(Integer skillId) {
		return skillId == 254 || skillId == 302;
	}

	private List<Integer> getAttackSpells(){
		//_log.warning("AutofarmPlayerRoutine.getAttackSpells");
		return getSpellsInSlots(AutofarmConstants.attackSlots);
	}

	private List<Integer> getSpellsInSlots(List<Integer> attackSlots) {
		return Arrays.stream(player.getShortCuts().getShortcuts())
				.filter(shortcut -> shortcut.getPage() == AutofarmConstants.shortcutsPageIndex && shortcut.getType() == L2ShortCut.TYPE_SKILL && attackSlots.contains(shortcut.getSlot()))
				.map(L2ShortCut::getId)
				.collect(Collectors.toList());
	}

	private List<Integer> getChanceSpells(){
		return getSpellsInSlots(AutofarmConstants.chanceSlots);
	}

	private List<Integer> getSelfSpells(){
		return getSpellsInSlots(AutofarmConstants.selfSlots);
	}

	private List<Integer> getLowLifeSpells(){
		return getSpellsInSlots(AutofarmConstants.lowLifeSlots);
	}

	private boolean shotcutsContainAttack() {
		return Arrays.stream(player.getShortCuts().getShortcuts()).anyMatch(shortcut ->
				/*shortcut.getPage() == 0 && */shortcut.getType() == L2ShortCut.TYPE_ACTION && shortcut.getId() == 2);
	}

	private void castSpellWithAppropriateTarget(L2Skill skill, Boolean forceOnSelf) {
		//_log.warning("AutofarmPlayerRoutine.castSpellWithAppropriateTarget#1");
		if (forceOnSelf) {
			//_log.warning("AutofarmPlayerRoutine.castSpellWithAppropriateTarget#2");
			L2Object oldTarget = player.getTarget();
			player.setTarget(player);
			player.useMagic(skill, false, false);
			player.setTarget(oldTarget);
			return;
		}
		//_log.warning("AutofarmPlayerRoutine.castSpellWithAppropriateTarget#3");
		player.useMagic(skill, false, false);
	}

	private void physicalAttack() {

		if(!(player.getTarget() instanceof L2MonsterInstance)) {
			return;
		}

		L2MonsterInstance target = (L2MonsterInstance)player.getTarget();

		if (target.isAutoAttackable(player))
		{
			if (GeoEngine.getInstance().canSeeTarget(player, target))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
				player.clearNextLoc();
			}
		}
		else
		{
			player.sendActionFailed();

			if (GeoEngine.getInstance().canSeeTarget(player, target))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
		}
	}

	// TODO [V] - может взять то что выше?
	/*private void physicalAttack() {

		//_log.warning("AutofarmPlayerRoutine.physicalAttack#1");
		if(!(player.getTarget() instanceof L2MonsterInstance)) {
			//_log.warning("AutofarmPlayerRoutine.physicalAttack#2");
			return;
		}
		//_log.warning("AutofarmPlayerRoutine.physicalAttack#3");

		L2MonsterInstance target = (L2MonsterInstance)player.getTarget();
		//_log.warning("AutofarmPlayerRoutine.physicalAttack#4 " + target);

		if (target.isAutoAttackable(player)) {
			//_log.warning("AutofarmPlayerRoutine.physicalAttack#5");
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			player.clearNextLoc();
		} else {
			//_log.warning("AutofarmPlayerRoutine.physicalAttack#6");
			if (target.canSeeTarget(player)) {
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
			}
		}
		//_log.warning("AutofarmPlayerRoutine.physicalAttack#7");
		//else
		player.sendActionFailed();
	}*/

	public void targetEligibleCreature() {
		//_log.warning("AutofarmPlayerRoutine.targetEligibleCreature#1");
		if(committedTarget != null) {
			//_log.warning("AutofarmPlayerRoutine.targetEligibleCreature#2");
			if(!committedTarget.isDead() && GeoEngine.getInstance().canSeeTarget(player, committedTarget)/* && !player.isMoving()*/) {
				//_log.warning("AutofarmPlayerRoutine.targetEligibleCreature#3");
				return;
			} else {
				//_log.warning("AutofarmPlayerRoutine.targetEligibleCreature#4");
				committedTarget = null;
				player.setTarget(null);
			}
		}
		//_log.warning("AutofarmPlayerRoutine.targetEligibleCreature#5");
		List<L2MonsterInstance> targets = getKnownMonstersInRadius(player, AutofarmConstants.targetingRadius,
				creature -> GeoEngine.getInstance().canMoveFromToTarget(player.getX(), player.getY(), player.getZ(), creature.getX(), creature.getY(), creature.getZ(), creature.getInstanceId())
						&& !creature.isDead());

		if(targets.isEmpty()) {
			//_log.warning("AutofarmPlayerRoutine.targetEligibleCreature#6");
			return;
		}

		L2MonsterInstance closestTarget = targets.stream().min((o1, o2) -> (int) Util.calculateDistance(o1, o2, false)).get();
		//_log.warning("AutofarmPlayerRoutine.targetEligibleCreature#7 " + closestTarget);
		committedTarget = closestTarget;
		player.setTarget(closestTarget);
	}

	public final List<L2MonsterInstance> getKnownMonstersInRadius(L2PcInstance player, int radius, Function<L2MonsterInstance, Boolean> condition)
	{
		final L2WorldRegion region = player.getWorldRegion();
		if (region == null)
			return Collections.emptyList();

		final List<L2MonsterInstance> result = new ArrayList<>();

		for (L2WorldRegion reg : region.getSurroundingRegions())
		{
			for (L2Object obj : reg.getVisibleObjects())
			{
				if (!(obj instanceof L2MonsterInstance) || !Util.checkIfInRange(radius, player, obj, true) || !condition.apply((L2MonsterInstance) obj))
					continue;

				//_log.warning("AutofarmPlayerRoutine.getKnownMonstersInRadius " + obj.getNpcId());
				result.add((L2MonsterInstance) obj);
			}
		}
		return result;
	}

	// TODO [V] - может это лучше использовать, вместо того что выше?
	/*public FastList<L2MonsterInstance> getVisiblePlayable(L2PcInstance player, int radius, Function<L2MonsterInstance, Boolean> condition) {
		L2WorldRegion reg = player.getWorldRegion();

		if (reg == null) {
			return null;
		}

		// Create an FastList in order to contain all visible L2Object
		FastList<L2MonsterInstance> result = new FastList<L2MonsterInstance>();

		// Create a FastList containing all regions around the current region
		FastList<L2WorldRegion> _regions = reg.getSurroundingRegions();
		for (FastList.Node<L2WorldRegion> n = _regions.head(), end = _regions.tail(); (n = n.getNext()) != end;) {
			L2WorldRegion value = n.getValue(); // No typecast necessary.
			if (value == null) {
				continue;
			}

			// Go through visible object of the selected region
			for(L2Object _object : value.getVisibleObjects()) {
				if (_object == null) {
					continue;
				}

				if (!(_object instanceof L2MonsterInstance) || !Util.checkIfInRange(radius, player, _object, true) || !condition.apply((L2MonsterInstance) _object))
					continue;

				if (_object.equals(player)) {
					continue;   // skip our own character
				}
				if (!_object.isVisible()) // GM invisible is different than this...
				{
					continue;   // skip dying objects
				}
				result.add((L2MonsterInstance) _object);
			}
		}

		return result;
	}*/

	public L2MonsterInstance getMonsterTarget(){
		if(!(player.getTarget() instanceof L2MonsterInstance)) {
			return null;
		}
		return (L2MonsterInstance) player.getTarget();
	}

	// TODO [V] - возможно пересмотреть немного
	private void useMagicSkill(L2Skill skill, Boolean forceOnSelf){
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#1");
		if (System.currentTimeMillis() - player.getCPC() < 100) {
			//_log.warning("AutofarmPlayerRoutine.useMagicSkill#2");
			player.sendActionFailed();
			return;
		}
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#3");
		player.setCPC();
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#4");
		if (player.isOutOfControl() || player.isAllSkillsDisabled()) {
			//_log.warning("AutofarmPlayerRoutine.useMagicSkill#5");
			player.sendActionFailed();
			return;
		}
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#6");
		if (player.isDead()) {
			//_log.warning("AutofarmPlayerRoutine.useMagicSkill#7");
			player.sendActionFailed();
			return;
		}
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#8");
		if (player.isFakeDeath()) {
			//_log.warning("AutofarmPlayerRoutine.useMagicSkill#9");
			player.stopFakeDeath(null);
			return;
		}
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#10");
		// Get the L2Skill template corresponding to the skillID received from the client
		// Check the validity of the skill
		if (skill == null) {
			//_log.warning("AutofarmPlayerRoutine.useMagicSkill#11");
			player.sendActionFailed();
			return;
		}
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#12");
		if (skill.isPassive() || skill.isChance()) {
			return;
		}
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#13");
		// Check if the skill type is TOGGLE
		if (skill.isToggle() && player.getFirstEffect(skill) != null) {
			//_log.warning("AutofarmPlayerRoutine.useMagicSkill#14");
			player.stopSkillEffects(skill.getId());
			return;
		}
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#15");
		skill = Formulas.checkForOlySkill(player, skill);
		//_log.warning("AutofarmPlayerRoutine.useMagicSkill#16");
		try {
			//_log.warning("AutofarmPlayerRoutine.useMagicSkill#17");
			// Check if all casting conditions are completed
			if (skill.isBattleForceSkill() || skill.isSpellForceSkill()) {
				//_log.warning("AutofarmPlayerRoutine.useMagicSkill#18");
				player.setGroundSkillLoc(null);
				if (skill.checkForceCondition(player, skill.getId())) {
					//_log.warning("AutofarmPlayerRoutine.useMagicSkill#19");
					castSpellWithAppropriateTarget(skill, forceOnSelf);
				} else {
					//_log.warning("AutofarmPlayerRoutine.useMagicSkill#20");
					player.sendPacket(Static.NOT_ENOUGH_FORCES);
					player.sendActionFailed();
				}
			} else if (skill.checkCondition(player, player, false)) {
				//_log.warning("AutofarmPlayerRoutine.useMagicSkill#21");
				castSpellWithAppropriateTarget(skill, forceOnSelf);
			} else {
				//_log.warning("AutofarmPlayerRoutine.useMagicSkill#22");
				player.sendActionFailed();
			}
		} catch (Exception e) {
			//_log.warning("AutofarmPlayerRoutine.useMagicSkill#23");
			player.sendActionFailed();
		}
	}

	public void useItem(L2ItemInstance item)
	{
		UseItem.doAction(player, item);
	}
}