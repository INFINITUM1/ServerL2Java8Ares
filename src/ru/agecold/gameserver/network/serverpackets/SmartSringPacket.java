package ru.agecold.gameserver.network.serverpackets;

import ru.agecold.gameserver.instancemanager.SmartScreenTextManager;
import ru.akumu.smartguard.core.model.d3d.Color;
import ru.akumu.smartguard.core.model.d3d.FontStyle;
import ru.akumu.smartguard.core.model.d3d.PredefinedMsg;
import ru.akumu.smartguard.core.model.d3d.ScreenPos;
import ru.akumu.smartguard.core.network.packets.ISmartPacket;

public class SmartSringPacket extends L2GameServerPacket implements ISmartPacket
{
	private final int _Id;
	private String _text;
	private final PredefinedMsg _pdMsg;
	private final Color _color;
	private final ScreenPos _screenPos;
	private final FontStyle _style;
	private short offsetX;
	private short offsetY;
	private int _showMs;
	private int _fadeInMs;
	private int _fadeOutMs;
	private final int _fontSize;
	private String _fontFamily;

	public SmartSringPacket(int Id, String _text, int _fontSize, String _fontFamily, PredefinedMsg _pdMsg, Color _color, ScreenPos _screenPos, FontStyle _style, short offsetX, short offsetY, int _fadeInMs, int _showMs, int _fadeOutMs)
	{
		_Id = Id;

		this._text = _text;
		this._pdMsg = _pdMsg;
		this._color = _color;
		this._screenPos = _screenPos;
		this._style = _style;
		this._fadeInMs = _fadeInMs;
		this._showMs = _showMs;
		this._fadeOutMs = _fadeOutMs;

		this._fontSize = _fontSize;
		this._fontFamily = _fontFamily;

		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	public SmartSringPacket(int id, String text, int _fontSize, String _fontFamily, PredefinedMsg pdMsg, Color color, ScreenPos screenPos, FontStyle style)
	{
		this(id, text, _fontSize, _fontFamily, pdMsg, color, screenPos, style, (short) 0, (short) 0, 0, 0, 0);
	}

	public SmartSringPacket(int id, String text, PredefinedMsg pdMsg, Color color, ScreenPos screenPos, FontStyle style, int fadeInMs, int showMs, int fadeOutMs)
	{
		this(id, text, SmartScreenTextManager.DEFAULT_FONT_SIZE, SmartScreenTextManager.DEFAULT_FONT_FAMILY, pdMsg, color, screenPos, style, (short) 0, (short) 0, fadeInMs, showMs, fadeOutMs);
	}

	public SmartSringPacket copy()
	{
		return new SmartSringPacket(_Id, _text, _fontSize, _fontFamily, _pdMsg, _color, _screenPos, _style, offsetX, offsetY, _showMs, _fadeInMs, _fadeOutMs);
	}

	public void replaceText(String preg, String rep)
	{
		_text = _text.replaceAll(preg, rep);
	}

	public String getText()
	{
		return this._text;
	}

	public void setShowMs(int showMs)
	{
		this._showMs = showMs;
	}

	public void setfadeInMs(int fadeInMs)
	{
		this._fadeInMs = fadeInMs;
	}

	public void setfadeOutMs(int fadeOutMs)
	{
		this._fadeOutMs = fadeOutMs;
	}

	public short getOffsetY()
	{
		return this.offsetY;
	}

	public void setOffsetY(short offsetY)
	{
		this.offsetY = offsetY;
	}

	public short getOffsetX()
	{
		return this.offsetX;
	}

	public void setOffsetX(short offsetX)
	{
		this.offsetX = offsetX;
	}

	public void incOffsetX(short offsetX)
	{
		this.offsetX = ((short)(this.offsetX + offsetX));
	}

	public void decOffsetX(short offsetX)
	{
		this.offsetX = ((short)(this.offsetX - offsetX));
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xff);
		writeC(0x05);
		writeC(0x01);
		writeD(_Id);
		writeS(_text);
		writeH(_fontSize);
		writeS(_fontFamily);
		writeH(_pdMsg.ordinal());
		writeC(_color.a);
		writeC(_color.r);
		writeC(_color.g);
		writeC(_color.b);
		writeD(_screenPos.mask);
		writeD(_style.ordinal());
		writeD(_fadeInMs);
		writeD(_showMs);
		writeD(_fadeOutMs);
		writeH(offsetX);
		writeH(offsetY);
	}
}