package org.mmocore.network;

import java.nio.ByteBuffer;

public abstract class AbstractPacket<T extends MMOClient>
{
	protected ByteBuffer _buf;

	protected T _client;

	protected void setClient(final T client)
	{
		_client = client;
	}

	public T getClient()
	{
		return _client;
	}

	protected void setByteBuffer(final ByteBuffer buf)
	{
		_buf = buf;
	}

	protected ByteBuffer getByteBuffer()
	{
		return _buf;
	}
}
