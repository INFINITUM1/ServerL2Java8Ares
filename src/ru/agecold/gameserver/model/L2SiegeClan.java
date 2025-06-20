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
package ru.agecold.gameserver.model;

import java.util.List;

import javolution.util.FastList;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;

public class L2SiegeClan
{
	// ==========================================================================================
	// Instance
	// ===============================================================
	// Data Field
	private int _clanId                = 0;
	private L2NpcInstance _flag = null;
	private int _numFlagsAdded = 0;
	private SiegeClanType _type;

	public enum SiegeClanType
	{
		OWNER,
		DEFENDER,
		ATTACKER,
		DEFENDER_PENDING
	}

	// =========================================================
	// Constructor

	public L2SiegeClan(int clanId, SiegeClanType type)
	{
		_clanId = clanId;
		_type = type;
	}

	// =========================================================
	// Method - Public
	public int getNumFlags()
	{
		if (_flag != null) 
			return 1;
			
		return 0;
	}

	public void addFlag(L2NpcInstance flag)
	{
		_flag = flag;
	}

	public boolean removeFlag()
	{
		if (_flag == null) 
			return false;

		_flag.deleteMe();
		_flag = null;
		return true;
	}

	// =========================================================
	// Proeprty
	public final int getClanId() 
	{ 
		return _clanId; 
	}
	
	public final L2NpcInstance getFlag()
	{
		return _flag;
	}

	public SiegeClanType getType() 
	{ 
		return _type; 
	}

    public void setType(SiegeClanType setType) 
	{ 
		_type = setType; 
	}
}
