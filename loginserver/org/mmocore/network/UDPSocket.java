package org.mmocore.network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class UDPSocket implements ISocket
{
	private final DatagramSocket _socket;

	public UDPSocket(DatagramSocket socket)
	{
		_socket = socket;
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.mmocore.network.ISocket#close()
	 */
	@Override
	public void close() throws IOException
	{
		_socket.close();
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.mmocore.network.ISocket#getReadableByteChannel()
	 */
	@Override
	public ReadableByteChannel getReadableByteChannel()
	{
		return _socket.getChannel();
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.mmocore.network.ISocket#getWritableByteChannel()
	 */
	@Override
	public WritableByteChannel getWritableByteChannel()
	{
		return _socket.getChannel();
	}

	/* (non-Javadoc)
	 * @see org.mmocore.network.ISocket#getInetAddress()
	 */
	@Override
	public InetAddress getInetAddress()
	{
		return _socket.getInetAddress();
	}

	@Override
	public int getPort()
	{
		return _socket.getPort();
	}
}
