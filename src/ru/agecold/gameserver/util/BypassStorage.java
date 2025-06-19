package ru.agecold.gameserver.util;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс хранилища и валидации ссылок в html.
 *
 * @author G1ta0
 */
public class BypassStorage
{
	private static final Pattern htmlBypass = Pattern.compile("<(?:button|a)[^>]+?action=\"bypass +(?:-h +)?([^\"]+?)\"[^>]*?>", Pattern.CASE_INSENSITIVE);
	private static final Pattern htmlLink = Pattern.compile("<(?:button|a)[^>]+?action=\"link +([^\"]+?)\"[^>]*?>", Pattern.CASE_INSENSITIVE);
	private static final Pattern bbsWrite = Pattern.compile("<(?:button|a)[^>]+?action=\"write +(\\S+) +\\S+ +\\S+ +\\S+ +\\S+ +\\S+\"[^>]*?>", Pattern.CASE_INSENSITIVE);
	private static final Pattern directHtmlBypass = Pattern.compile("^(_mrsl|_diary|_match|manor_menu_select).*", Pattern.DOTALL);
	private static final Pattern directBbsBypass = Pattern.compile("^(_bbshome|_bbsgetfav|_bbsaddfav|_bbslink|_bbsloc|_bbsclan|_bbsmemo|_maillist|_friendlist).*", Pattern.DOTALL);

	public static class ValidBypass
	{
		public String bypass;
		public final boolean args;
		public final boolean bbs;

		public ValidBypass(final String bypass, final boolean args, final boolean bbs)
		{
			this.bypass = bypass;
			this.args = args;
			this.bbs = bbs;
		}
	}

	private final List<ValidBypass> bypasses = new CopyOnWriteArrayList<>();

	public void parseHtml(final CharSequence html, final boolean bbs)
	{
		clear(bbs);

		Matcher m = htmlBypass.matcher(html);
		while(m.find())
		{
			String bypass = m.group(1);
			int i = bypass.indexOf(" $");
			if(i > 0)
				bypass = bypass.substring(0, i);
			addBypass(new ValidBypass(bypass, i >= 0, bbs));
		}

		if(bbs)
		{
			m = bbsWrite.matcher(html);
			while(m.find())
			{
				final String bypass = m.group(1);
				addBypass(new ValidBypass(bypass, true, true));
			}
		}
		m = htmlLink.matcher(html);
		while(m.find())
		{
			String bypass = m.group(1);
			addBypass(new ValidBypass(bypass, false, bbs));
		}
	}

	public ValidBypass validate(final String bypass)
	{
		ValidBypass ret = null;

		if(directHtmlBypass.matcher(bypass).matches())
			ret = new ValidBypass(bypass, false, false);
		else if(directBbsBypass.matcher(bypass).matches())
			ret = new ValidBypass(bypass, false, true);
		else
		{
			final boolean args = bypass.indexOf(" ") > 0;
			for(final ValidBypass bp : bypasses)
			{
				//При передаче аргументов, мы можем проверить только часть команды до первого аргумента
				if(Objects.equals(bp.bypass, bypass) || (bp.args && args && bypass.startsWith(bp.bypass + " ")))
				{
					ret = bp;
					break;
				}
			}
		}

		if(ret != null)
		{
			ret.bypass = bypass;
			clear(ret.bbs);
		}

		return ret;
	}

	private void addBypass(final ValidBypass bypass)
	{
		bypasses.add(bypass);
	}

	private void clear(final boolean bbs)
	{
		bypasses.stream().filter(bp -> bp.bbs == bbs).forEach(bypasses::remove);
	}
}