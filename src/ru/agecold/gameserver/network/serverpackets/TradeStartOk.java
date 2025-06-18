package ru.agecold.gameserver.network.serverpackets;

public class TradeStartOk extends L2GameServerPacket {
    
    @Override
    protected final void writeImpl() {

        writeC(0x1F);
    }
}
