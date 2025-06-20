package ru.agecold.gameserver.autofarm;

import ru.agecold.Config;

import java.util.Arrays;
import java.util.List;

public class AutofarmConstants {
	public final static int shortcutsPageIndex = Config.SHORTCUTS_PAGE_INDEX;
	public final static int targetingRadius = Config.TARGETING_RADAIUS;
	public final static int lowLifePercentageThreshold = Config.USE_LIFE_PERCENTAGE_THRESHOLD;
	public final static int useMpPotsPercentageThreshold = Config.USE_MP_PERCENTAGE_THRESHOLD;
	public final static int useHpPotsPercentageThreshold = Config.USE_HP_PERCENTAGE_THRESHOLD;
	public final static int mpPotItemId = Config.MP_POT_ITEM_ID;
	public final static int hpPotItemId = Config.HP_POT_ITEM_ID;
	public final static int hpPotSkillId = Config.HP_POT_SKILL_ID;

	public final static List<Integer> attackSlots = Arrays.asList(Config.ATTACK_SLOTS);
	public final static List<Integer> chanceSlots = Arrays.asList(Config.CHANCE_SLOTS);
	public final static List<Integer> selfSlots = Arrays.asList(Config.SELF_SLOTS);
	public final static List<Integer> lowLifeSlots = Arrays.asList(Config.LOW_LIFE_SLOTS);
}