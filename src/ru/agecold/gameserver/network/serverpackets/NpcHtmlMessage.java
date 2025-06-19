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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.agecold.gameserver.cache.HtmCache;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.clientpackets.RequestBypassToServer;

public class NpcHtmlMessage extends L2GameServerPacket {
    // d S
    // d is usually 0, S is the html text starting with <html> and ending with </html>
    //

    private static final Logger _log = Logger.getLogger(RequestBypassToServer.class.getName());
    private final int _npcObjId;
    private String _html;
    private String _file = null;
    private List<String> _replaces = new ArrayList<String>();

    private static final Pattern objectId = Pattern.compile("%objectId%");
    private static final Pattern playername = Pattern.compile("%playername%");

    public static NpcHtmlMessage id(int npcObjId, String text) {
        return new NpcHtmlMessage(npcObjId, text);
    }

    public NpcHtmlMessage(int npcObjId, String text) {
        _npcObjId = npcObjId;
        setHtml(text);
    }

    public static NpcHtmlMessage id(int npcObjId) {
        return new NpcHtmlMessage(npcObjId);
    }

    public NpcHtmlMessage(int npcObjId) {
        _npcObjId = npcObjId;
    }

    public NpcHtmlMessage setHtml(String text) {
        _html = text; // html code must not exceed 8192 bytes
        return this;
    }

    public NpcHtmlMessage setFile(String file) {
        _file = file;
        return this;
    }

    public NpcHtmlMessage replace(String pattern, String value) {
        if(pattern == null || value == null)
            return this;
        _replaces.add(pattern);
        _replaces.add(value);
        return this;
    }

    public NpcHtmlMessage replace(String pattern, int value) {
        if(pattern == null || value == 0)
            return this;
        _replaces.add(pattern);
        _replaces.add(String.valueOf(value));
        return this;
    }

    @Override
    protected final void writeImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if(player == null)
        {
            return;
        }

        if(_file != null) //TODO может быть не очень хорошо сдесь это делать...
        {
            if(player.isGM())
                player.sendHTMLMessage(_file);

            setHtml(HtmCache.getInstance().getHtm(_file));
        }

        for(int i = 0; i < _replaces.size(); i += 2)
            _html = _html.replace(_replaces.get(i), _replaces.get(i + 1));

        if(_html == null)
        {
            L2NpcInstance npc = (L2NpcInstance) L2World.getInstance().findObject(_npcObjId);
            _log.warning("NpcHtmlMessage, _html == null, npc: " + npc + ", file: " + _file);
            return;
        }

        Matcher m = objectId.matcher(_html);
        if(m != null)
            _html = m.replaceAll(String.valueOf(_npcObjId));

        _html = playername.matcher(_html).replaceAll(player.getName());

        player.getBypassStorage().parseHtml(_html, false);

        if(_html.length() > 8192)
            _html = "<html><body><center>Sorry, to long html.</center></body></html>";

        writeC(0x0f);
        writeD(_npcObjId);
        writeS(_html);
        writeD(0x00);
    }
}
