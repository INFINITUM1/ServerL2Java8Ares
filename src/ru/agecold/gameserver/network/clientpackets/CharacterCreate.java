/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.agecold.gameserver.network.clientpackets;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import javolution.util.FastMap;
import ru.agecold.Config;
import ru.agecold.gameserver.datatables.*;
//import ru.agecold.gameserver.datatables.ItemTable;
import ru.agecold.gameserver.idfactory.IdFactory;
import ru.agecold.gameserver.model.L2ItemInstance;
import ru.agecold.gameserver.model.L2ShortCut;
import ru.agecold.gameserver.model.L2SkillLearn;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.L2GameClient;
import ru.agecold.gameserver.network.serverpackets.CharCreateFail;
import ru.agecold.gameserver.network.serverpackets.CharCreateOk;
import ru.agecold.gameserver.network.serverpackets.CharSelectInfo;
import ru.agecold.gameserver.templates.L2PcTemplate;
import ru.agecold.gameserver.util.Util;
import ru.agecold.util.Location;

/**
 * This class ...
 *
 * @version $Revision: 1.9.2.3.2.8 $ $Date: 2005/03/27 15:29:30 $
 */
@SuppressWarnings("unused")
public final class CharacterCreate extends L2GameClientPacket {

    //private static final Logger _log = Logger.getLogger(CharacterCreate.class.getName());
    // cSdddddddddddd
    private String _name;
    private int _race;
    private byte _sex;
    private int _classId;
    private int _int;
    private int _str;
    private int _con;
    private int _men;
    private int _dex;
    private int _wit;
    private byte _hairStyle;
    private byte _hairColor;
    private byte _face;
    private long _exp;

    @Override
    protected void readImpl() {
        _name = readS();
        _race = readD();
        _sex = (byte) readD();
        _classId = readD();
        _int = readD();
        _str = readD();
        _con = readD();
        _men = readD();
        _dex = readD();
        _wit = readD();
        _hairStyle = (byte) readD();
        _hairColor = (byte) readD();
        _face = (byte) readD();
        _exp = 6299994999L;
    }

    @Override
    protected void runImpl() {
        if (CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) >= Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT && Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS));
            return;
        } else if (CharNameTable.getInstance().doesCharNameExist(_name)) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS));
            return;
        } else if ((_name.length() < 3) || (_name.length() > 16) || !Util.isAlphaNumeric(_name) || !isValidName(_name)) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS));
            return;
        }

        //if (Config.DEBUG)
        //	_log.fine("charname: " + _name + " classId: " + _classId);
        L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(_classId);
        if (template == null || template.classBaseLevel > 1) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
            return;
        }

        CharNameTable.getInstance().storeCharName(_name);

        int objectId = IdFactory.getInstance().getNextId();
        L2PcInstance newChar = L2PcInstance.create(objectId, template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, _sex != 0);
        //newChar.setMaxLoad(template.baseLoad);
        if (Config.ALT_START_LEVEL > 0) {
            newChar.fullRestore();
        } else {
            newChar.setCurrentHp(template.baseHpMax);
            newChar.setCurrentCp(template.baseCpMax);
            newChar.setCurrentMp(template.baseMpMax);
        }

        // send acknowledgement
        sendPacket(new CharCreateOk());

        initNewChar(getClient(), newChar);
    }
    private static final Pattern cnamePattern = Pattern.compile(Config.CNAME_TEMPLATE);//Pattern.compile("[\\w\\u005F\\u002E]+", Pattern.UNICODE_CASE);

    private boolean isValidName(String text) {
        /* Pattern pattern;
         try {
         pattern = Pattern.compile(Config.CNAME_TEMPLATE);
         } catch (PatternSyntaxException e) // case of illegal pattern
         {
         _log.warning("ERROR : Character name pattern of config is wrong!");
         pattern = Pattern.compile(".*");
         }*/

        return cnamePattern.matcher(text).matches();
    }

    private void initNewChar(L2GameClient client, L2PcInstance newChar) {
        //if (Config.DEBUG) _log.fine("Character init start");
        L2World.getInstance().storeObject(newChar);

        L2PcTemplate template = newChar.getTemplate();

        newChar.addAdena("Init", Config.STARTING_ADENA, null, false);

        Location loc = template.getRandomSpawnPoint();
        newChar.setXYZInvisible(loc.x, loc.y, loc.z);

        if (Config.CUSTOM_SHORTCUTS) {
            for (Integer itemId : template.getItems()) {
                L2ItemInstance item = newChar.getInventory().addItem("Init", itemId, 1, newChar, null);
                if (Config.CHAR_CREATE_ENCHANT > 0) {
                    item.setEnchantLevel(Config.CHAR_CREATE_ENCHANT);
                }
                if (item.isEquipable()) {
                    newChar.getInventory().equipItemAndRecord(item);
                }
            }
            L2SkillLearn[] startSkills = SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId());
            for (int i = 0; i < startSkills.length; i++) {
                newChar.addSkill(SkillTable.getInstance().getInfo(startSkills[i].getId(), startSkills[i].getLevel()), true);
            }
            CustomServerData.getInstance().registerShortCuts(newChar);
        }
        else
        {
            L2ShortCut shortcut;

            //add attack shortcut
            shortcut = new L2ShortCut(0, 0, 3, 2, -1, 1);
            newChar.registerShortCut(shortcut);

            //add take shortcut
            shortcut = new L2ShortCut(3, 0, 3, 5, -1, 1);
            newChar.registerShortCut(shortcut);

            //add sit shortcut
            shortcut = new L2ShortCut(10, 0, 3, 0, -1, 1);
            newChar.registerShortCut(shortcut);

            for (Integer itemId : template.getItems()) {
                L2ItemInstance item = newChar.getInventory().addItem("Init", itemId, 1, newChar, null);
                if (item.getItemId() == 5588) {
                    //add tutbook shortcut
                    shortcut = new L2ShortCut(11, 0, 1, item.getObjectId(), -1, 1);
                    newChar.registerShortCut(shortcut);
                }
                if (item.isEquipable()) {
                    newChar.getInventory().equipItemAndRecord(item);
                }
            }


            L2SkillLearn[] startSkills = SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId());
            for (int i = 0; i < startSkills.length; i++) {
                newChar.addSkill(SkillTable.getInstance().getInfo(startSkills[i].getId(), startSkills[i].getLevel()), true);
                if (startSkills[i].getId() == 1001 || startSkills[i].getId() == 1177) {
                    shortcut = new L2ShortCut(1, 0, 2, startSkills[i].getId(), 1, 1);
                    newChar.registerShortCut(shortcut);
                }
                if (startSkills[i].getId() == 1216) {
                    shortcut = new L2ShortCut(10, 0, 2, startSkills[i].getId(), 1, 1);
                    newChar.registerShortCut(shortcut);
                }
                //if (Config.DEBUG)
                //	_log.fine("adding starter skill:" + startSkills[i].getId()+ " / "+ startSkills[i].getLevel());
            }
        }
        L2GameClient.saveCharToDisk(newChar);
        newChar.deleteMe(); // release the world of this character and it's inventory

        // send char list
        CharSelectInfo cl = new CharSelectInfo(client.getAccountName(), client.getSessionId().playOkID1);
        client.getConnection().sendPacket(cl);
        client.setCharSelection(cl.getCharInfo());
        //if (Config.DEBUG) _log.fine("Character init end");
    }
}
