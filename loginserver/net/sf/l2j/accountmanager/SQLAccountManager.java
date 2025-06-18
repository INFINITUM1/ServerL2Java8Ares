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
package net.sf.l2j.accountmanager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.sf.l2j.Base64;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Server;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.TimeLogger;

/**
 * This class SQL Account Manager
 * 
 * @author netimperia
 * @version $Revision: 2.3.2.1.2.3 $ $Date: 2005/08/08 22:47:12 $
 */
public class SQLAccountManager
{
	private static String _uname = "";
	private static String _pass = "";
	private static String _level = "";
	private static String _mode = "";

	public static void main(String[] args) throws SQLException, IOException, NoSuchAlgorithmException
	{
		Server.serverMode = Server.MODE_LOGINSERVER;
		Config.load();
		System.out.println("Please choose an option:");
        System.out.println("");
		System.out.println("1 - Create new account or update existing one (change pass and access level).");
		System.out.println("2 - Change access level.");
		System.out.println("3 - Delete existing account.");
		System.out.println("4 - List accounts & access levels.");		
		System.out.println("5 - Exit.");
		LineNumberReader _in = new LineNumberReader(new InputStreamReader(System.in));
		while (!(_mode.equals("1") || _mode.equals("2") || _mode.equals("3") 
				|| _mode.equals("4") || _mode.equals("5")) )
		{
			System.out.print("Your choice: ");
			_mode = _in.readLine();
		}
		
		if (_mode.equals("1") || _mode.equals("2") || _mode.equals("3"))
		{
			if (_mode.equals("1") || _mode.equals("2") || _mode.equals("3"))
			while (_uname.length() == 0)
			{
				System.out.print("Username: ");
				_uname = _in.readLine().toLowerCase();
			}
			
			if (_mode.equals("1"))
			while (_pass.length() == 0)
			{
				System.out.print("Password: ");
				_pass = _in.readLine();
			}
			
			if (_mode.equals("1") || _mode.equals("2"))
			while (_level.length() == 0)
			{
				System.out.print("Access level: ");
				_level = _in.readLine();
			}

		}

		if (_mode.equals("1"))
		{
			// Add or Update
			addOrUpdateAccount(_uname,_pass,_level);		
		}
		else if(_mode.equals("2"))
		{
			// Change Level
			changeAccountLevel(_uname,_level);			
		}
		else if(_mode.equals("3"))
		{
			// Delete
			System.out.print("Do you really want to delete this account ? Y/N : ");
			String yesno = _in.readLine();
			if (yesno.equals("Y"))
			{
				// Yes		
				deleteAccount(_uname);
			}
			
		}
		else if(_mode.equals("4"))
		{
			// List
 			printAccInfo();			
		}
				
		return;
	}

	private static void printAccInfo() throws SQLException 
	{
		int count = 0;
		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT login, access_level FROM accounts ORDER BY login ASC");
			rset = statement.executeQuery();
			while (rset.next())
			{
				System.out.println(rset.getString("login")	+ " -> " + rset.getInt("access_level"));
				count++;
			}
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: SQLAccountManager - printAccInfo(): " + e);
		}
		finally
		{
			Close.closeConStatRes(con, statement, rset);
		}
		System.out.println("Number of accounts: " + count + ".");
	}		
	
	private static void addOrUpdateAccount(String account,String password, String level) throws IOException, SQLException, NoSuchAlgorithmException		
	{
		// Encode Password		
		MessageDigest md = MessageDigest.getInstance("SHA");
		byte[] newpass;
		newpass = password.getBytes("UTF-8");
		newpass = md.digest(newpass);
		
		// Add to Base
		Connect con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE\taccounts (login, password, access_level) VALUES (?,?,?)");
			statement.setString(1, account);
			statement.setString(2, Base64.encodeBytes(newpass));
			statement.setString(3, level);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: SQLAccountManager - addOrUpdateAccount(): " + e);
		}
		finally
		{
			Close.closeConStat(con, statement);
		}
	}

	private static void changeAccountLevel(String account, String level) throws SQLException		
	{
		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			// Check Account Exist
			statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login=?");
			statement.setString(1, account);
			rset = statement.executeQuery();
			if(!rset.next())
			{
				System.out.println("False");
			}
			else if(rset.getInt(1)>0)
			{
				// Exist

				// Update
				statement = con.prepareStatement("UPDATE accounts SET access_level=? WHERE login=?;");
				statement.setEscapeProcessing(true);
				statement.setString(1, level);
				statement.setString(2, account);
				statement.executeUpdate();

				System.out.println("Account " + account + " has been updated.");
			}
			else
			{
				// Not Exist
				System.out.println("Account " + account + " does not exist.");
			}
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: SQLAccountManager - changeAccountLevel(): " + e);
		}
		finally
		{
			Close.closeConStatRes(con, statement, rset);
		}
	}	

	private static void deleteAccount(String account) throws SQLException		
	{
		Connect con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		ResultSet rcln = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			// Check Account Exist
			statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login=?;");
			statement.setString(1, account);
			rset = statement.executeQuery();
			if(!rset.next())
			{
				System.out.println("False");
				Close.closeResultSet(rset);
			}
			else if(rset.getInt(1)>0)
			{
				Close.closeResultSet(rset);
				// Account exist

				// Get Accounts ID
				statement = con.prepareStatement("SELECT obj_Id, char_name, clanid FROM characters WHERE account_name=?;");
				statement.setEscapeProcessing(true);
				statement.setString(1, account);
				rset = statement.executeQuery();

				while (rset.next())
				{
					System.out.println("Deleting character " + rset.getString("char_name") + ".");

					// Check If clan leader Remove Clan and remove all from it
					Close.closeStatement(statement);
					statement = con.prepareStatement("SELECT COUNT(*) FROM clan_data WHERE leader_id=?;");
					statement.setString(1, rset.getString("clanid"));
					rcln = statement.executeQuery();
					rcln.next();
					if(rcln.getInt(1) > 0)
					{
						Close.closeResultSet(rcln);
						// Clan Leader

						// Get Clan Name
						Close.closeStatement(statement);
						statement = con.prepareStatement("SELECT clan_name FROM clan_data WHERE leader_id=?;");
						statement.setString(1, rset.getString("clanid"));
						rcln = statement.executeQuery();
						rcln.next();

						System.out.println("Deleting clan " + rcln.getString("clan_name") + ".");

						// Delete Clan Wars
						Close.closeStatement(statement);
						statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?;");
						statement.setEscapeProcessing(true);
						statement.setString(1, rcln.getString("clan_name"));
						statement.setString(2, rcln.getString("clan_name"));
						statement.executeUpdate();

						Close.closeResultSet(rcln);

						// Remove All From clan
						Close.closeStatement(statement);
						statement = con.prepareStatement("UPDATE characters SET clanid=0 WHERE clanid=?;");
						statement.setString(1, rset.getString("clanid"));
						statement.executeUpdate();


						// Delete Clan
						Close.closeStatement(statement);
						statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?;");
						statement.setString(1, rset.getString("clanid"));
						statement.executeUpdate();

						Close.closeStatement(statement);
						statement = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?;");
						statement.setString(1, rset.getString("clanid"));
						statement.executeUpdate();

						Close.closeStatement(statement);
						statement = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?;");
						statement.setString(1, rset.getString("clanid"));
						statement.executeUpdate();

					}
					else
					{
						Close.closeResultSet(rcln);
					}

					// skills
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=?;");
					statement.setString(1, rset.getString("obj_Id"));
					statement.executeUpdate();

					// shortcuts
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?;");
					statement.setString(1, rset.getString("obj_Id"));
					statement.executeUpdate();

					// items
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?;");
					statement.setString(1, rset.getString("obj_Id"));
					statement.executeUpdate();

					// recipebook
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?;");
					statement.setString(1, rset.getString("obj_Id"));
					statement.executeUpdate();

					// quests
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=?;");
					statement.setString(1, rset.getString("obj_Id"));
					statement.executeUpdate();

					// macroses
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=?;");
					statement.setString(1, rset.getString("obj_Id"));
					statement.executeUpdate();

					// friends
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=?;");
					statement.setString(1, rset.getString("obj_Id"));
					statement.executeUpdate();

					// merchant_lease
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?;");
					statement.setString(1, rset.getString("obj_Id"));
					statement.executeUpdate();

					// boxaccess
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM boxaccess WHERE charname=?;");
					statement.setString(1, rset.getString("char_name"));
					statement.executeUpdate();

					// characters
					Close.closeStatement(statement);
					statement = con.prepareStatement("DELETE FROM characters WHERE obj_Id=?;");
					statement.setString(1, rset.getString("obj_Id"));
					statement.executeUpdate();

				}

				// Delete Account
				Close.closeStatement(statement);
				statement = con.prepareStatement("DELETE FROM accounts WHERE login=?;");
				statement.setEscapeProcessing(true);
				statement.setString(1, account);
				statement.executeUpdate();

				System.out.println("Account " + account + " has been deleted.");
			}
			else
			{
				// Not Exist
				System.out.println("Account " + account + " does not exist.");
			}
		}
		catch (SQLException e)
		{
			System.out.println(TimeLogger.getLogTime() + "WARNING: SQLAccountManager - changeAccountLevel(): " + e);
		}
		finally
		{
			Close.closeConStatRes(con, statement, rset);
		}
	}	

}
