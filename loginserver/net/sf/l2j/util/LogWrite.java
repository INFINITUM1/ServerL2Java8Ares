package net.sf.l2j.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javolution.text.TextBuilder;

public class LogWrite
{
	private static final Logger _log = Logger.getLogger(LogWrite.class.getName());

	public static void add(String text, String cat)
	{
		Lock print = new ReentrantLock();
		print.lock();
		try
		{
			new File("log/").mkdirs();
			File file = new File("log/" + (cat != null ? cat : "_all") + ".txt");

			if(!file.exists())
			{
				try
				{
					file.createNewFile();
				}
				catch(IOException e)
				{
					System.out.println("saving " + (cat != null ? cat : "all") + " log failed, can't create file: " + e);
					return;
				}
			}

			FileWriter save = null;
			TextBuilder msgb = new TextBuilder();
			try
			{
				save = new FileWriter(file, true);

				msgb.append(text + "\n");
				save.write(msgb.toString());
			}
			catch(IOException e)
			{
				System.out.println("saving " + (cat != null ? cat : "all") + " log failed: " + e);
				e.printStackTrace();
			}
			finally
			{
				try
				{
					if(save != null)
						save.close();
					msgb.clear();
				}
				catch(Exception e)
				{}
			}
		}
		catch(Exception e3)
		{}
		finally
		{
			print.unlock();
		}
	}
}