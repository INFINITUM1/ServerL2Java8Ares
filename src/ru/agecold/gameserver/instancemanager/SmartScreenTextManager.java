package ru.agecold.gameserver.instancemanager;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.agecold.gameserver.network.L2GameClient;
import ru.agecold.gameserver.network.serverpackets.SmartSringPacket;
import ru.akumu.smartguard.core.GuardConfig;
import ru.akumu.smartguard.core.log.GuardLog;
import ru.akumu.smartguard.core.model.d3d.Color;
import ru.akumu.smartguard.core.model.d3d.FontStyle;
import ru.akumu.smartguard.core.model.d3d.PredefinedMsg;
import ru.akumu.smartguard.core.model.d3d.ScreenPos;

public class SmartScreenTextManager
{
    private static final File banlistFile = new File(GuardConfig.SMART_GUARD_DIR, "d3dx_custom.xml");
    public static final int DT_TOP = 0;
    public static final int DT_LEFT = 0;
    public static final int DT_CENTER = 1;
    public static final int DT_RIGHT = 2;
    public static final int DT_VCENTER = 4;
    public static final int DT_BOTTOM = 8;
    private static SmartScreenTextManager _instance;
    public static final short DEFAULT_ZERO = 0;
    public static final short DEFAULT_OFFSET = 0;
    public static final int DEFAULT_FONT_SIZE = 12;
    public static final String DEFAULT_FONT_FAMILY = "Tahoma";

    public static SmartScreenTextManager getInstance()
    {
        return _instance;
    }

    private SmartScreenTextManager() {

    }

    public static void init()
    {
        _instance = new SmartScreenTextManager();
        _instance.load();
    }

    private void load()
    {
        synchronized (_generalStrings)
        {
            _packetMap.clear();
            _generalStrings.clear();
            _counter = 1000;
            try
            {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.parse(banlistFile);

                NodeList nList = doc.getElementsByTagName("string");
                for (int temp = 0; temp < nList.getLength(); temp++)
                {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == DT_CENTER)
                    {
                        Element xmle = (Element)nNode;
                        try
                        {
                            String value = xmle.getAttribute("value").trim();
                            ScreenPos pos = ScreenPos.valueOf(xmle.getAttribute("pos").trim());
                            FontStyle style = FontStyle.valueOf(xmle.getAttribute("style").trim());

                            String clrstr = xmle.getAttribute("color").trim().toLowerCase();
                            if (clrstr.length() != DT_BOTTOM) {
                                clrstr = "FF" + clrstr;
                            }
                            if (clrstr.length() != DT_BOTTOM) {
                                throw new Exception("Color length must be " + DT_BOTTOM);
                            }
                            int _fontSize;
                            try
                            {
                                _fontSize = Integer.parseInt(xmle.getAttribute("size").trim());
                            }
                            catch (Exception e)
                            {
                                _fontSize = 12;
                            }
                            String _fontFamily;
                            try
                            {
                                _fontFamily = xmle.getAttribute("family");
                            }
                            catch (Exception e)
                            {
                                _fontFamily = DEFAULT_FONT_FAMILY;
                            }
                            int idx = 0;
                            int[] argb = new int[DT_VCENTER];
                            for (int i = 0; i < DT_BOTTOM; i += DT_RIGHT) {
                                argb[idx++] = Integer.parseInt(clrstr.substring(i, i + DT_RIGHT), 16);
                            }
                            Color color = new Color(argb[0], argb[1], argb[2], argb[3]);
                            int showTime = 0, fadeIn = 0, fadeOut = 0;
                            short offsetX = 0, offsetY = 0;
                            PredefinedMsg msgId = PredefinedMsg.None;
                            int real_id = 0;

                            NodeList aNode = xmle.getElementsByTagName("showtime");
                            for (int j = 0; j < aNode.getLength(); j++)
                            {
                                Node n = aNode.item(j);
                                if (n.getNodeType() == DT_CENTER)
                                {
                                    Element st = (Element)n;
                                    showTime = Integer.parseInt(st.getAttribute("value").trim());
                                }
                            }
                            aNode = xmle.getElementsByTagName("fadein");
                            for (int j = 0; j < aNode.getLength(); j++)
                            {
                                Node n = aNode.item(j);
                                if (n.getNodeType() == DT_CENTER)
                                {
                                    Element st = (Element)n;
                                    fadeIn = Integer.parseInt(st.getAttribute("value").trim());
                                }
                            }
                            aNode = xmle.getElementsByTagName("fadeout");
                            for (int j = 0; j < aNode.getLength(); j++)
                            {
                                Node n = aNode.item(j);
                                if (n.getNodeType() == DT_CENTER)
                                {
                                    Element st = (Element)n;
                                    fadeOut = Integer.parseInt(st.getAttribute("value").trim());
                                }
                            }
                            aNode = xmle.getElementsByTagName("msgId");
                            for (int j = 0; j < aNode.getLength(); j++)
                            {
                                Node n = aNode.item(j);
                                if (n.getNodeType() == DT_CENTER)
                                {
                                    Element st = (Element)n;
                                    if (st.getAttribute("value").matches("^-?\\d+$"))
                                    {
                                        real_id = Integer.parseInt(st.getAttribute("value"));
                                        msgId = PredefinedMsg.valueOf("None");
                                    }
                                }
                            }
                            aNode = xmle.getElementsByTagName("offset");
                            for (int j = 0; j < aNode.getLength(); j++)
                            {
                                Node n = aNode.item(j);
                                if (n.getNodeType() == DT_CENTER)
                                {
                                    Element st = (Element)n;
                                    offsetX = Short.parseShort(st.getAttribute("x").trim());
                                    offsetY = Short.parseShort(st.getAttribute("y").trim());
                                }
                            }
                            if (real_id >= DT_CENTER) {
                                registerCustomString(real_id, value, _fontSize, _fontFamily, msgId, color, pos, style, fadeIn, showTime, fadeOut, offsetX, offsetY);
                            }
                        }
                        catch (Exception e)
                        {
                            GuardLog.getLogger().warning("Can not load D3DX string!");
                            GuardLog.logException(e);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                GuardLog.logException(e);
            }
        }
    }

    private final HashMap<Integer, SmartSringPacket> _generalStrings = new HashMap<Integer, SmartSringPacket>();
    private final HashMap<Integer, SmartSringPacket> _packetMap = new HashMap<Integer, SmartSringPacket>();
    private final int _counterDefaultValue = 1000;
    private int _counter = 1000;
    private final Object _counterLock = new Object();

    public int GetNextStringId()
    {
        synchronized (_counterLock)
        {
            if (_counter == Integer.MAX_VALUE) {
                _counter = 1000;
            }
            return _counter++;
        }
    }

    public void registerCustomString(int Id, String _text, int _fontSize, String _fontFamily, PredefinedMsg _pdMsg, Color _color, ScreenPos _screenPos, FontStyle _style, int _fadeInMs, int _showMs, int _fadeOutMs, short x, short y)
    {
        if (_text == null || _text.length() == 0) {
            throw new InvalidParameterException("SmartScreenTextManager[ERROR]: msgId " + Id + " - Text can not be null or empty.");
        }
        if (_text.length() > 2048) {
            throw new InvalidParameterException("SmartScreenTextManager[ERROR]: msgId " + Id + " - Text too long, please specify up to 2048 characters.");
        }
        if (_fontFamily == null || _fontFamily.length() > 64) {
            throw new InvalidParameterException("SmartScreenTextManager[ERROR]: msgId " + Id + " - Font family too long, please specify up to 64 characters.");
        }
        if (_fadeInMs < 0 || _showMs < 0 || _fadeOutMs < 0) {
            throw new InvalidParameterException("SmartScreenTextManager[ERROR]: msgId " + Id + " - ((_fadeInMs < 0) || (_showMs < 0) || (_fadeOutMs < 0))");
        }
        if (_screenPos == ScreenPos.TopRightRelative) {
            throw new InvalidParameterException("SmartScreenTextManager[ERROR]: msgId " + Id + " - TopRightRelative is not using anymore! Use: TopRight");
        }
        _packetMap.put(Id, new SmartSringPacket(Id, _text, _fontSize, _fontFamily, _pdMsg, _color, _screenPos, _style, x, y, _fadeInMs, _showMs, _fadeOutMs));
    }

    public void onPlayerLogin(L2GameClient client)
    {
        if (client == null) {
            return;
        }
        for (SmartSringPacket rsp : _generalStrings.values()) {
            if (rsp == null) {
                continue;
            }

            client.sendPacket(rsp);
        }
    }

    public SmartSringPacket getStringPacket(int id)
    {
        return _packetMap.get(id);
    }

    public void reload() {
        init();
    }
}