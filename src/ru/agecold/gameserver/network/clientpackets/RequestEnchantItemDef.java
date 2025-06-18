package ru.agecold.gameserver.network.clientpackets;

public final class RequestEnchantItemDef extends RequestEnchantItem
{
    private int _objectId;

    @Override
	protected void readImpl()
    {
        _objectId = readD();
    }

    @Override
	protected void runImpl()
    {
		//
	}
}
