package scripts;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import ru.agecold.Config;
import ru.agecold.gameserver.datatables.ItemTable;
import ru.agecold.gameserver.model.Inventory;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.PcInventory;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.skills.Env;
import ru.agecold.gameserver.skills.Stats;
import ru.agecold.gameserver.skills.funcs.Func;
import ru.agecold.gameserver.templates.L2Item;
import ru.agecold.util.ArrayUtils;

public class StatModifier
{
	/*private static final Logger LOGGER = Logger.getLogger(StatMod.class.getName());
	private static final StatModifier INSTANCE = new StatModifier();
	private static final File file = new File(Config.DATAPACK_ROOT, "data/stats_custom_mod.xml");
	private static Map<Stats, Map<Integer, List<StatMod>>> _stats = new HashMap<Stats, Map<Integer, List<StatMod>>>();

	private static class StatMod
	{
		private final ModCond[] _conds;
		private final Stats _stat;
		private double mul = 1.;
		private double add = 0.;

		private StatMod(ModCond[] conds, Stats stat)
		{
			_conds = conds.clone();
			_stat = stat;
		}

		public boolean test(L2PcInstance player, L2Character target)
		{
			for(ModCond cond : _conds)
			{
				if(!cond.test(player, target))
					return false;
			}
			return true;
		}

		public Stats getStat()
		{
			return _stat;
		}

		public double getMul()
		{
			return mul;
		}

		public void addMul(double value)
		{
			mul += value;
		}

		public double getAdd()
		{
			return add;
		}

		public void setAdd(double value)
		{
			add = value;
		}
	}

	private List<StatMod> parseStats(Element element, ModCond[] conds)
	{
		List<StatMod> statMods = new ArrayList<StatMod>();
		for(Iterator localIterator = element.elementIterator(); localIterator.hasNext(); )
		{
			Element localElement = (Element) localIterator.next();
			StatMod statMod;
			if("mul".equalsIgnoreCase(localElement.getName()))
			{
				statMod = new StatMod(conds, Stats.valueOfXml(localElement.attributeValue("stat")), null);
				statMod.addMul(Double.parseDouble(localElement.attributeValue("val")) - 1.);
			}
			else if("add".equalsIgnoreCase(localElement.getName()))
			{
				statMod = new StatMod(conds, Stats.valueOfXml(localElement.attributeValue("stat")), null);
				statMod.setAdd(Double.parseDouble(localElement.attributeValue("val")));
			}
		}
		return statMods;
	}

	private List<StatMod> parseTargetStats(Element element, final int classId, ModCond[] conds)
	{
		List<StatMod> statMods = new ArrayList<StatMod>();
		conds = ArrayUtils.add(conds, new ModCond()
		{
			@Override
			public boolean test(L2PcInstance player, L2Character target)
			{
				return target != null && target.isPlayer() && target.getPlayer().getActiveClass() == classId;
			}
		});
		statMods.addAll(parseStats(element, conds));
		return statMods;
	}

	private List<StatMod> parseItems(Element element, ModCond[] conds)
	{
		List<StatMod> statMods = new ArrayList<StatMod>();
		final int itemId = Integer.parseInt(element.attributeValue("itemId"));
		final L2Item item = ItemTable.getInstance().getTemplate(itemId);
		conds = ArrayUtils.add(conds, new ModCond()
		{
			@Override
			public boolean test(L2PcInstance player, L2Character target)
			{
				if(player == null)
				{
					return false;
				}
				PcInventory inventory = player.getInventory();
				int slot = Inventory.getPaperdollIndex(item.getBodyPart());
				if(slot < 0)
				{
					return false;
				}
				return inventory.getPaperdollItemId(slot) == slot;
			}
		});
		statMods.addAll(parseStats(element, conds));
		return statMods;
	}

	private List<StatMod> parseListStats(Element element, final int classId, ModCond[] conds)
	{
		List<StatMod> statMods = new ArrayList<StatMod>();
		conds = ArrayUtils.add(conds, new ModCond()
		{
			@Override
			public boolean test(L2PcInstance player, L2Character target)
			{
				return player != null && player.getActiveClass() == classId;
			}
		});
		for(Iterator secondElementIterator = element.elementIterator(); secondElementIterator.hasNext(); )
		{
			Element secondElement = (Element) secondElementIterator.next();
			if("targetPlayer".equalsIgnoreCase(secondElement.getName()))
			{
				int targetClassId = Integer.parseInt(secondElement.attributeValue("classId"));
				statMods.addAll(parseTargetStats(secondElement, targetClassId, conds));
			}
			else if("equipedWith".equalsIgnoreCase(secondElement.getName()))
			{
				statMods.addAll(parseItems(secondElement, conds));
			}
		}
		statMods.addAll(parseStats(element, conds.clone()));
		return statMods;
	}

	private Map<Stats, Map<Integer, List<StatMod>>> load()
	{
		try
		{
			SAXReader localSAXReader = new SAXReader(true);
			Document localDocument = localSAXReader.read(file);
			Element localElement1 = localDocument.getRootElement();
			if(!"list".equalsIgnoreCase(localElement1.getName()))
			{
				throw new RuntimeException();
			}

			Map<Stats, Map<Integer, List<StatMod>>> listStats = new HashMap<Stats, Map<Integer, List<StatMod>>>();
			for(Iterator elementIterator = localElement1.elementIterator(); elementIterator.hasNext(); )
			{
				Element element = (Element) elementIterator.next();
				if("player".equalsIgnoreCase(element.getName()))
				{
					int classId = Integer.parseInt(element.attributeValue("classId"));
					for(Element secondElementIterator = element.elementIterator(); secondElementIterator.hasNext(); )
					{
						Element secondElement = (Element) secondElementIterator.next();
						for(StatMod statMod : parseListStats(secondElement, classId, new ModCond[0]))
						{
							Map<Integer, List<StatMod>> statsForClasess = listStats.get(statMod.getStat());
							if(statsForClasess == null)
							{
								listStats.put(statMod.getStat(), statsForClasess = new HashMap<Integer, List<StatMod>>());
							}
							List<StatMod> statMods = statsForClasess.get(classId);
							if(statMods == null)
							{
								statsForClasess.put(classId, statMods = new ArrayList<StatMod>());
							}
							statMods.add(statMod);
						}
					}
				}
			}

			return listStats;
		}
		catch(Exception e)
		{
			LOGGER.warning(e.getMessage() + e);
		}
		return Collections.emptyMap();
	}

	public void onLoad()
	{
		_stats.clear();
		Map<Stats, Map<Integer, List<StatMod>>> stats = load();

		PlayerListenerList.addGlobal(INSTANCE);
		_stats.putAll(stats);
		LOGGER.info("StatModifier: Enabled. Loaded mods for " + _stats.size() + " class id(s).");
	}

	public void onReload()
	{
		onShutdown();
		onLoad();
	}

	public void onShutdown()
	{
		PlayerListenerList.removeGlobal(INSTANCE);
		if(!_stats.isEmpty())
		{
			for(L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				INSTANCE.removeStats(player);
			}
		}
	}

	private void removeStats(L2PcInstance player)
	{
		if(player == null)
		{
			return;
		}
		player.removeStatsOwner(this);
	}

	private void addStats(L2PcInstance player)
	{
		for(Map.Entry<Stats, Map<Integer, List<StatMod>>> entryStat : _stats.entrySet())
		{
			Stats stat = entryStat.getKey();
			Map<Integer, List<StatMod>> listStats = entryStat.getValue();
			player.addStatFunc(new Func(stat, 80, this)
			{
				@Override
				public void calc(Env env)
				{
					if(env.cha == null || !env.cha.isPlayer())
					{
						return;
					}
					L2PcInstance player = (L2PcInstance) env.cha;

					List<StatMod> statMods = listStats.get(player.getActiveClass());
					if(statMods == null || statMods.isEmpty())
					{
						return;
					}
					double mul = 1.;
					double add = 0.;
					for(StatMod statMod : statMods)
					{
						if(statMod.test(player, env.target))
						{
							mul += statMod.getMul() - 1.;
							add += statMod.getAdd();
						}
					}
					env.value *= mul;
					env.value += add;
				}
			});
		}
	}

	@Override
	public void onPlayerEnter(L2PcInstance player)
	{
		if(player == null)
		{
			return;
		}
		if(!_stats.isEmpty())
		{
			INSTANCE.addStats(player);
		}
	}

	private static abstract class ModCond
	{
		public abstract boolean test(L2PcInstance player, L2Character target);
	}*/
}
