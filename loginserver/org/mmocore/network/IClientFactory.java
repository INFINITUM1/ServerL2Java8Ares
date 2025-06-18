package org.mmocore.network;

public interface IClientFactory<T extends MMOClient>
{
	public T create(MMOConnection<T> con);
}
