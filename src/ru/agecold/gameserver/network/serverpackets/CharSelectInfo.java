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
package ru.agecold.gameserver.network.serverpackets;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.model.CharSelectInfoPackage;
import ru.agecold.gameserver.model.Inventory;
import ru.agecold.gameserver.model.L2Clan;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.L2GameClient;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;

/**
 * This class ...
 *
 * @version $Revision: 1.8.2.4.2.6 $ $Date: 2005/04/06 16:13:46 $
 */
public class CharSelectInfo extends L2GameServerPacket {

    // d SdSddddddddddffddddddddddddddddddddddddddddddddddddddddddddddffd
    private static Logger _log = Logger.getLogger(CharSelectInfo.class.getName());

    private String _loginName;

    private int _sessionId, _activeId;

    private CharSelectInfoPackage[] _characterPackages;

    /**
     * @param _characters
     */
    public CharSelectInfo(String loginName, int sessionId) {
        _sessionId = sessionId;
        _loginName = loginName;
        _characterPackages = loadCharacterSelectInfo();
        _activeId = -1;
    }

    public CharSelectInfo(String loginName, int sessionId, int activeId) {
        _sessionId = sessionId;
        _loginName = loginName;
        _characterPackages = loadCharacterSelectInfo();
        _activeId = activeId;
    }

    public CharSelectInfoPackage[] getCharInfo() {
        return _characterPackages;
    }

    @Override
    protected final void writeImpl() {
        int size = (_characterPackages.length);

        writeC(0x13);
        writeD(size);

        long lastAccess = 0L;

        if (_activeId == -1) {
            for (int i = 0; i < size; i++) {
                if (lastAccess < _characterPackages[i].getLastAccess()) {
                    lastAccess = _characterPackages[i].getLastAccess();
                    _activeId = i;
                }
            }
        }

        for (int i = 0; i < size; i++) {
            CharSelectInfoPackage charInfoPackage = _characterPackages[i];

            writeS(charInfoPackage.getName());
            writeD(charInfoPackage.getCharId());
            writeS(_loginName);
            writeD(_sessionId);
            writeD(charInfoPackage.getClanId());
            writeD(0x00); // ??

            writeD(charInfoPackage.getSex());
            writeD(charInfoPackage.getRace());

            if (charInfoPackage.getClassId() == charInfoPackage.getBaseClassId()) {
                writeD(charInfoPackage.getClassId());
            } else {
                writeD(charInfoPackage.getBaseClassId());
            }

            writeD(0x01); // active ??

            writeD(0x00); // x
            writeD(0x00); // y
            writeD(0x00); // z

            writeF(charInfoPackage.getCurrentHp()); // hp cur
            writeF(charInfoPackage.getCurrentMp()); // mp cur

            writeD(charInfoPackage.getSp());
            writeQ(charInfoPackage.getExp());
            writeD(charInfoPackage.getLevel());
            writeD(charInfoPackage.getKarma()); // karma

            writeD(0x00);
            writeD(0x00);
            writeD(0x00);
            writeD(0x00);
            writeD(0x00);
            writeD(0x00);
            writeD(0x00);
            writeD(0x00);
            writeD(0x00);

            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_DHAIR));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
            writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FACE));

            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_REAR));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_NECK));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
            writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FACE));

            writeD(charInfoPackage.getHairStyle());
            writeD(charInfoPackage.getHairColor());
            writeD(charInfoPackage.getFace());

            writeF(charInfoPackage.getMaxHp()); // hp max
            writeF(charInfoPackage.getMaxMp()); // mp max

            long deleteTime = charInfoPackage.getDeleteTimer();
            int deletedays = 0;
            if (deleteTime > 0) {
                deletedays = (int) ((deleteTime - System.currentTimeMillis()) / 1000);
            }
            writeD(deletedays); // days left before
            // delete .. if != 0
            // then char is inactive
            writeD(charInfoPackage.getClassId());
            if (i == _activeId) {
                writeD(0x01);
            } else {
                writeD(0x00); //c3 auto-select char
            }
            writeC(charInfoPackage.getEnchantEffect() > 127 ? 127 : charInfoPackage.getEnchantEffect());
            writeD(charInfoPackage.getAugmentationId());
        }
    }

    private CharSelectInfoPackage[] loadCharacterSelectInfo() {
        CharSelectInfoPackage charInfopackage;
        List<CharSelectInfoPackage> characterList = new FastList<>();

        FastList<String> loadedChars = new FastList<>();

        Connect con = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, base_class FROM characters WHERE account_name=?");
            statement.setString(1, _loginName);
            result = statement.executeQuery();

            while (result.next())// fills the package
            {
                if (loadedChars.contains(result.getString("char_name"))) {
                    continue;
                }
                charInfopackage = restoreChar(result);
                if (charInfopackage != null) {
                    characterList.add(charInfopackage);
                    loadedChars.add(result.getString("char_name"));
                }
            }
            return characterList.toArray(new CharSelectInfoPackage[characterList.size()]);
        } catch (Exception e) {
            _log.warning("Could not restore char info: " + e);
        } finally {
            Close.CSR(con, statement, result);
        }

        return new CharSelectInfoPackage[0];
    }

    private void loadCharacterSubclassInfo(CharSelectInfoPackage charInfopackage, int ObjectId, int activeClassId) {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            statement = con.prepareStatement("SELECT exp, sp, level FROM character_subclasses WHERE char_obj_id=? && class_id=? ORDER BY char_obj_id");
            statement.setInt(1, ObjectId);
            statement.setInt(2, activeClassId);
            result = statement.executeQuery();
            if (result.next()) {
                charInfopackage.setExp(result.getLong("exp"));
                charInfopackage.setSp(result.getInt("sp"));
                charInfopackage.setLevel(result.getInt("level"));
            }
        } catch (Exception e) {
            _log.warning("Could not restore char subclass info: " + e);
        } finally {
            Close.CSR(con, statement, result);
        }

    }

    private CharSelectInfoPackage restoreChar(ResultSet chardata) throws Exception {
        int objectId = chardata.getInt("obj_id");

        // See if the char must be deleted
        long deletetime = chardata.getLong("deletetime");
        if (deletetime > 0) {
            if (System.currentTimeMillis() > deletetime) {
                L2PcInstance cha = L2PcInstance.load(objectId);
                L2Clan clan = cha.getClan();
                if (clan != null) {
                    clan.removeClanMember(cha.getName(), 0);
                }

                L2GameClient.deleteCharByObjId(objectId);
                return null;
            }
        }

        String name = chardata.getString("char_name");

        CharSelectInfoPackage charInfopackage = new CharSelectInfoPackage(objectId, name);
        charInfopackage.setLevel(chardata.getInt("level"));
        charInfopackage.setMaxHp(chardata.getInt("maxhp"));
        charInfopackage.setCurrentHp(chardata.getDouble("curhp"));
        charInfopackage.setMaxMp(chardata.getInt("maxmp"));
        charInfopackage.setCurrentMp(chardata.getDouble("curmp"));
        charInfopackage.setKarma(chardata.getInt("karma"));

        charInfopackage.setFace(chardata.getInt("face"));
        charInfopackage.setHairStyle(chardata.getInt("hairstyle"));
        charInfopackage.setHairColor(chardata.getInt("haircolor"));
        charInfopackage.setSex(chardata.getInt("sex"));

        charInfopackage.setExp(chardata.getLong("exp"));
        charInfopackage.setSp(chardata.getInt("sp"));
        charInfopackage.setClanId(chardata.getInt("clanid"));

        charInfopackage.setRace(chardata.getInt("race"));

        final int baseClassId = chardata.getInt("base_class");
        final int activeClassId = chardata.getInt("classid");

        // if is in subclass, load subclass exp, sp, lvl info
        if (baseClassId != activeClassId) {
            loadCharacterSubclassInfo(charInfopackage, objectId, activeClassId);
        }

        charInfopackage.setClassId(activeClassId);

        // Get the augmentation id for equipped weapon
        int weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND);
        if (weaponObjId < 1) {
            weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND);
        }

        if (weaponObjId > 0) {
            Connect con = null;
            PreparedStatement statement = null;
            ResultSet result = null;
            try {
                con = L2DatabaseFactory.get();
                con.setTransactionIsolation(1);
                statement = con.prepareStatement("SELECT aug_id FROM items WHERE object_id=?");
                statement.setInt(1, weaponObjId);
                result = statement.executeQuery();

                if (result.next()) {
                    charInfopackage.setAugmentationId(result.getInt("aug_id"));
                }
            } catch (Exception e) {
                _log.warning("Could not restore augmentation info: " + e);
            } finally {
                Close.CSR(con, statement, result);
            }
        }

        /*
         * Check if the base class is set to zero and alse doesn't match
         * with the current active class, otherwise send the base class ID.
         *
         * This prevents chars created before base class was introduced
         * from being displayed incorrectly.
         */
        if (baseClassId == 0 && activeClassId > 0) {
            charInfopackage.setBaseClassId(activeClassId);
        } else {
            charInfopackage.setBaseClassId(baseClassId);
        }

        charInfopackage.setDeleteTimer(deletetime);
        charInfopackage.setLastAccess(chardata.getLong("lastAccess"));

        return charInfopackage;
    }
}
