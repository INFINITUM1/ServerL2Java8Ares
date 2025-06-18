package net.sf.l2j.loginserver;

import javolution.util.FastTable;

public class TestMap
{
	private static FastTable<String> _array;
	private static final FastTable<String> _emptyBuffs = new FastTable<String>();

	public static void main(String[] args)
	{
		prepareTest();
	}

	public static void prepareTest()
	{
		_array = new FastTable<String>();
		long start = System.currentTimeMillis();
		for(int i = 0; i < 10000; i++)
			_array.add("L2Skill@!#!@$!@$-4355-2");

		long time = System.currentTimeMillis() - start;
		System.out.println("ADD: Time: " + time + " ms.");

		runTest();
	}

	public static void runTest()
	{
		long start = System.currentTimeMillis();
		FastTable<String> table = getTable();
		int i = 0;
		for(int n = table.size(); i < n; i++)
		{
			String ttt = table.get(i);
			if(ttt != null)
				ttt.toLowerCase();
		}
		table.clear();
		long time = System.currentTimeMillis() - start;
		System.out.println("READ2: Time: " + time + " ms.");

		start = System.currentTimeMillis();
		for (String strr : getTable())
		{
			if(strr == null)
				continue;

			strr.toLowerCase();
		}
		time = System.currentTimeMillis() - start;
		System.out.println("READ1: Time: " + time + " ms.");

		start = System.currentTimeMillis();
		for (String strr : getArray())
		{
			if(strr == null)
				continue;

			strr.toLowerCase();
		}
		time = System.currentTimeMillis() - start;
		System.out.println("READ3: Time: " + time + " ms.");
		System.out.println(".");
		System.out.println(".");

		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException e)
		{}

		runTest();
	}

	private static FastTable<String> getTable()
	{
		if(_array == null || _array.isEmpty())
			return _emptyBuffs;

		FastTable<String> copy = new FastTable<String>();
		copy.addAll(_array);

		return copy;
	}

	private static String[] getArray()
	{
		FastTable<String> copy = new FastTable<String>();

		if(_array == null || _array.isEmpty())
			return new String[0];

		copy.addAll(_array);

		int ArraySize = copy.size();
		String[] effectArray = new String[ArraySize];
		for (int i = 0; i < ArraySize && i < copy.size() && copy.get(i) != null; i++)
			effectArray[i] = copy.get(i);

		return effectArray;
	}
}