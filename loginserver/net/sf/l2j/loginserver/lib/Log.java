package net.sf.l2j.loginserver.lib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.l2j.Config;

public class Log
{
	private static final Logger _log = Logger.getLogger(Log.class.getName());

	public static final void add(String text, String cat)
	{
		String date = new SimpleDateFormat("yy.MM.dd H:mm:ss").format(new Date());

		new File("log/game").mkdirs();

		try
		{
			File file = new File("log/game/" + (cat != null ? cat : "_all") + ".txt");

			FileWriter save = new FileWriter(file, true);
			String out = "[" + date + "] '---': " + text + "\n";
			save.write(out);
			save.flush();
			save.close();
			save = null;
			file = null;
		}
		catch(IOException e)
		{
			System.out.println("saving chat log failed: " + e);
			e.printStackTrace();
		}

		if(cat != null)
			add(text, null);
	}

	@Deprecated
	public static final void Assert(boolean exp)
	{
		Assert(exp, "");
	}

	public static final void Assert(boolean exp, String cmt)
	{
		if(exp || !Config.ASSERT)
			return;

		System.out.println("Assertion error [" + cmt + "]");
		Thread.dumpStack();
	}
}