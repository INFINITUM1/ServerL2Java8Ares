package org.mmocore.network;

public final class HeaderInfo<T>
{
	private int _headerPending;
	private int _dataPending;
	private boolean _multiPacket;
	private T _client;

	public HeaderInfo()
	{

	}

	public HeaderInfo<T> set(int headerPending, int dataPending, boolean multiPacket, T client)
	{
		this.setHeaderPending(headerPending);
		this.setDataPending(dataPending);
		this.setMultiPacket(multiPacket);
		this.setClient(client);
		return this;
	}

	protected boolean headerFinished()
	{
		return getHeaderPending() == 0;
	}

	protected boolean packetFinished()
	{
		return getDataPending() == 0;
	}

	/**
	 * @param dataPending the dataPending to set
	 */
	private void setDataPending(int dataPending)
	{
		_dataPending = dataPending;
	}

	/**
	 * @return the dataPending
	 */
	protected int getDataPending()
	{
		return _dataPending;
	}

	/**
	 * @param headerPending the headerPending to set
	 */
	private void setHeaderPending(int headerPending)
	{
		_headerPending = headerPending;
	}

	/**
	 * @return the headerPending
	 */
	protected int getHeaderPending()
	{
		return _headerPending;
	}

	/**
	 * @param client the client to set
	 */
	protected void setClient(T client)
	{
		_client = client;
	}

	/**
	 * @return the client
	 */
	protected T getClient()
	{
		return _client;
	}

	/**
	 * @param multiPacket the multiPacket to set
	 */
	private void setMultiPacket(boolean multiPacket)
	{
		_multiPacket = multiPacket;
	}

	/**
	 * @return the multiPacket
	 */
	public boolean isMultiPacket()
	{
		return _multiPacket;
	}
}
