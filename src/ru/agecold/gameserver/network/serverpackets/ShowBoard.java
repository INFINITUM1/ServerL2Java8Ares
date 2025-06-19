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

import ru.agecold.gameserver.util.Util;

import java.util.List;

public class ShowBoard extends L2GameServerPacket {

    public static L2GameServerPacket CLOSE = new ShowBoard();
    private int _show = 1;

    private static final String TOP = "bypass _bbshome";
    private static final String FAV = "bypass _bbsgetfav";
    private static final String REGION = "bypass _bbsloc";
    private static final String CLAN = "bypass _bbsclan";
    private static final String MEMO = "bypass _bbsmemo";
    private static final String MAIL = "bypass _maillist_0_1_0_";
    private static final String FRIENDS = "bypass _friendlist_0_";
    private static final String ADDFAV = "bypass bbs_add_fav";

    private final StringBuilder _htmlCode = new StringBuilder();

    public ShowBoard(String htmlCode, String id)
    {
        Util.append(_htmlCode, id, "\b", htmlCode);
    }

    public ShowBoard(List<String> arg)
    {
        _htmlCode.append("1002\b");
        for(String str : arg)
            Util.append(_htmlCode, str, " \b");
    }

    private ShowBoard()
    {
        _show = 0;
        _htmlCode.append("");
    }

    @Override
    protected final void writeImpl() {
        writeC(0x6E);
        writeC(_show); //c4 1 to show community 00 to hide
        writeS(TOP);
        writeS(FAV);
        writeS(REGION);
        writeS(CLAN);
        writeS(MEMO);
        writeS(MAIL);
        writeS(FRIENDS);
        writeS(ADDFAV);
        writeS(_htmlCode.toString());
    }

    @Override
    public String getType() {
        return "S.ShowBoard";
    }
}
