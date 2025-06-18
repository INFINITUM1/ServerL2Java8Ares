package org.mmocore.network;

import javolution.text.TextBuilder;

public abstract class ReceivablePacket<T extends MMOClient> extends AbstractPacket<T> implements Runnable
{
	protected ReceivablePacket()
	{

	}

	protected int getAvaliableBytes()
	{
		return getByteBuffer().remaining();
	}

	protected abstract boolean read();

	@Override
	public abstract void run();

	protected void readB(byte[] dst)
	{
		getByteBuffer().get(dst);
	}

	protected void readB(byte[] dst, int offset, int len)
	{
		getByteBuffer().get(dst, offset, len);
	}

	protected int readC()
	{
		return getByteBuffer().get() & 0xFF;
	}

	protected int readH()
	{
		return getByteBuffer().getShort() & 0xFFFF;
	}

	protected int readD()
	{
		return getByteBuffer().getInt();
	}

	protected long readQ()
	{
		return getByteBuffer().getLong();
	}

	protected double readF()
	{
		return getByteBuffer().getDouble();
	}

	protected String readS()
	{
		TextBuilder tb = TextBuilder.newInstance();
		char ch;

		while((ch = getByteBuffer().getChar()) != 0)
		{
			tb.append(ch);
		}
		String str = tb.toString();
		TextBuilder.recycle(tb);
		return str;
	}
}
