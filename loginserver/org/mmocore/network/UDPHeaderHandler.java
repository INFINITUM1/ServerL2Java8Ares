package org.mmocore.network;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public abstract class UDPHeaderHandler<T extends MMOClient> extends HeaderHandler<T, UDPHeaderHandler<T>> {

    /**
     * @param subHeaderHandler
     */
    public UDPHeaderHandler(UDPHeaderHandler<T> subHeaderHandler) {
        super(subHeaderHandler);
    }

    private final HeaderInfo<T> _headerInfoReturn = new HeaderInfo<T>();

    protected abstract HeaderInfo handleHeader(ByteBuffer buf);

    protected abstract void onUDPConnection(SelectorThread<T> selector, DatagramChannel dc, SocketAddress key, ByteBuffer buf);

    /**
     * @return the headerInfoReturn
     */
    protected final HeaderInfo<T> getHeaderInfoReturn() {
        return _headerInfoReturn;
    }
}
