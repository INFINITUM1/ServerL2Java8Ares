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
package net.sf.l2j.loginserver.gameserverpackets;

import java.util.logging.Logger;

import net.sf.l2j.loginserver.clientpackets.ClientBasePacket;

/**
 * Format: cccddb
 * c desired ID
 * c accept alternative ID
 * c reserve Host
 * s ExternalHostName
 * s InetranlHostName
 * d max players
 * d hexid size
 * b hexid
 * @author -Wooden-
 *
 */
public class GameServerAuth extends ClientBasePacket
{
	protected static Logger _log = Logger.getLogger(GameServerAuth.class.getName());
	private byte[] _hexId;
	private int _desiredId;
	private boolean _hostReserved;
	private boolean _acceptAlternativeId;
	private int _maxPlayers;
	private int _port;
	private int _cats;
	private String _externalHost;
	private String _internalHost;
	private String _key;
	private int[] ports;
	
	/**
	 * @param decrypt
	 */
	public GameServerAuth(byte[] decrypt)
	{
		super(decrypt);
		_desiredId = readC();
		_acceptAlternativeId = (readC() == 0 ? false : true); 
		_hostReserved = (readC() == 0 ? false : true);
		_port = readH();
		_maxPlayers = readD();
		_key = readS();
		int size = readD();
		_hexId = readB(size);
		_externalHost = readS();
		_internalHost = readS();
		_cats = readC();
		ports = new int[readH()];
		for (int i = 0; i < ports.length; i++)
		{
			ports[i] = readH();
		}
	}

	public int[] getPorts()
	{
		return ports;
	}

	public int getCatsGuard()
	{
		return _cats;
	}

	/**
	 * @return
	 */
	public byte[] getHexID()
	{
		return _hexId;
	}
	
	public boolean getHostReserved()
	{
		return _hostReserved;
	}
	
	public int getDesiredID()
	{
		return _desiredId;
	}
	
	public boolean acceptAlternateID()
	{
		return _acceptAlternativeId;
	}

	/**
	 * @return Returns the max players.
	 */
	public int getMaxPlayers()
	{
		return _maxPlayers;
	}

	/**
	 * @return Returns the externalHost.
	 */
	public String getExternalHost()
	{
		return _externalHost;
	}

	/**
	 * @return Returns the internalHost.
	 */
	public String getInternalHost()
	{
		return _internalHost;
	}
	
	/**
	 * @return Returns the port.
	 */
	public int getPort()
	{
		return _port;
	}

	public String getSerialKey()
	{
		return _key;
	}
}
