package scripts.zone.type;

import javolution.util.FastList;
import ru.agecold.gameserver.datatables.CustomServerData;
import ru.agecold.gameserver.datatables.SkillTable;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.L2Skill;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.L2ZoneType;

public class L2EventZone extends L2ZoneType {
    private int _zoneId = 0;
    private int _pvpArena = 0;
    private boolean _noPeace = false;
    private int _maxHennaBonus = 0;
    private int _minHennaBonus = 0;
    private final FastList<Integer> _forbiddenItem = new FastList<Integer>();
    private final FastList<Integer> _forbiddenSkills = new FastList<Integer>();

    public L2EventZone(int id) {
        super(id);
        _zoneId = id;
    }

    public void setParameter(String name, String value)
    {
        if (name.equals("items"))
        {
            String[] token = value.split(",");
            for (String point : token) {
                if (point.equals("")) {
                    continue;
                }
                _forbiddenItem.add(Integer.parseInt(point));
            }
            CustomServerData.getInstance().addTeleItem(_zoneId, _forbiddenItem);
        } else if (name.equals("skills")) {
            String[] token = value.split(",");
            for (String point : token) {
                if (point.equals("")) {
                    continue;
                }
                _forbiddenSkills.add(Integer.parseInt(point));
            }
        } else if (name.equals("maxHennaBonus")) {
            _maxHennaBonus = Integer.parseInt(value);
        } else if (name.equals("minHennaBonus")) {
            _minHennaBonus = Integer.parseInt(value);
        } else if (name.equals("pvpType")) {
            _pvpArena = Integer.parseInt(value);
            if (_pvpArena == 7) {
                _noPeace = true;
            }
        } else {
            super.setParameter(name, value);
        }
    }

    protected void onEnter(L2Character character) {
        manageEnter(character.getPlayer(), true);
    }

    protected void onExit(L2Character character) {
        manageEnter(character.getPlayer(), false);
    }

    public void onDieInside(L2Character character) {
    }

    public void onReviveInside(L2Character character) {
    }

    private void manageEnter(L2PcInstance player, boolean enter) {
        if (player == null) {
            return;
        }
        switch (_pvpArena) {
            case 46:
                player.setFreePvp(enter);
                player.setPVPArena(enter);
                break;
            case 14:
                player.setPVPArena(enter);
                player.setFreeArena(enter);
                break;
            case 7:
                player.setFreePk(enter);
                player.broadcastUserInfo();

                break;
            case 8:
                player.setFreePk(enter);
                if (!enter) {
                    player.startPvPFlag();
                }
                player.broadcastUserInfo();
        }
        if (!_forbiddenItem.isEmpty()) {
            player.setForbItem(enter ? _zoneId : 0);
            player.checkForbiddenEventItems();
        }
        if (!_forbiddenSkills.isEmpty()) {
            for (L2Skill s : player.getAllSkills()) {
                if (s == null) {
                    continue;
                }
                if (_forbiddenSkills.contains(s.getId())) {
                    player.removeSkill(s, false);
                }
            }
        }
        SkillTable st = SkillTable.getInstance();
        for (L2Skill s : player.getAllSkills()) {
            if (s == null) {
                continue;
            }

            int mlvl = st.getMaxLevel(s.getId(), 0);
            if (s.getLevel() > mlvl) {
                player.removeSkill(s, false);
                player.addSkill(st.getInfo(s.getId(), mlvl), false);
            }
        }
        if (_maxHennaBonus > -1) {
            if (enter) {
                player.recalcHennaStats(_minHennaBonus, _maxHennaBonus);
            } else {
                player.recalcHennaStats();
            }
        }
        player.broadcastUserInfo();
    }
}