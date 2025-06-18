package org.mmocore.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javolution.util.FastList;
import javolution.util.FastList.Node;
import net.sf.l2j.Config;
import net.sf.l2j.util.LogWrite;
import net.sf.l2j.util.TimeLogger;

public class SelectorThread<T extends MMOClient> extends Thread {

    class FloodWorker extends Thread {

        @Override
        public void run() {
            for (;;) {
                if (_shutdown) {
                    return;
                }

                clearFlood();
				try
				{
					Thread.sleep(Config.BAN_CLEAR);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
            }
        }
    }

	class GcWorker extends Thread
	{
		@Override
		public void run()
		{
			for (;;)
			{
				if (_shutdown) {
					return;
				}
				System.gc();
				try
				{
					Thread.sleep(Config.GARBAGE_CLEAR);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

    private Selector _selector;
	private static SelectorThread _sThread;

    // Implementations
    private final IPacketHandler<T> _packetHandler;
    private IMMOExecutor<T> _executor;
    private IClientFactory<T> _clientFactory;
    private IAcceptFilter _acceptFilter;
    private final TCPHeaderHandler<T> _tcpHeaderHandler;

    private boolean _shutdown;

	public static SelectorThread getInstance()
	{
		return _sThread;
	}

    // Pending Close
    private FastList<MMOConnection<T>> _pendingClose = new FastList<MMOConnection<T>>();

    // Configs
    private final int HELPER_BUFFER_SIZE;
    private final int HELPER_BUFFER_COUNT;
    private final int MAX_SEND_PER_PASS;
    private int HEADER_SIZE = 2;
    private final ByteOrder BYTE_ORDER;
    private final long SLEEP_TIME;

    // MAIN BUFFERS
    private final ByteBuffer DIRECT_WRITE_BUFFER;
    private final ByteBuffer WRITE_BUFFER;
    private final ByteBuffer READ_BUFFER;

    // ByteBuffers General Purpose Pool
    private final FastList<ByteBuffer> _bufferPool = new FastList<ByteBuffer>();
	private FloodWorker _floodWorker;

	static class IpInfo
	{
		private int _c;
		private long _l;
		private int _d = 0;

		public IpInfo(int c, long l)
		{
			_c = c;
			_l = l;
		}

		public IpInfo(int c, long l, int d)
		{
			_c = c;
			_l = l;
			_d = d;
		}
	}

	private final ConcurrentHashMap<String, IpInfo> _connects = new ConcurrentHashMap<String, IpInfo>();
	public FastList<String> blackIPs;
	public FastList<String> whiteIPs;

	public SelectorThread(SelectorConfig<T> sc, IMMOExecutor<T> executor, IClientFactory<T> clientFactory, IAcceptFilter acceptFilter) throws IOException
	{
		HELPER_BUFFER_SIZE = sc.getHelperBufferSize();
        HELPER_BUFFER_COUNT = sc.getHelperBufferCount();
        MAX_SEND_PER_PASS = sc.getMaxSendPerPass();
        BYTE_ORDER = sc.getByteOrder();
        SLEEP_TIME = sc.getSelectorSleepTime();

        DIRECT_WRITE_BUFFER = ByteBuffer.allocateDirect(sc.getWriteBufferSize()).order(BYTE_ORDER);
        WRITE_BUFFER = ByteBuffer.wrap(new byte[sc.getWriteBufferSize()]).order(BYTE_ORDER);
        READ_BUFFER = ByteBuffer.wrap(new byte[sc.getReadBufferSize()]).order(BYTE_ORDER);

        _tcpHeaderHandler = sc.getTCPHeaderHandler();
        this.initBufferPool();
        _acceptFilter = acceptFilter;
		_packetHandler = sc.getTCPPacketHandler();
        _clientFactory = clientFactory;
        this.setExecutor(executor);
        this.initializeSelector();
    }

    protected void initBufferPool() {
        for (int i = 0; i < HELPER_BUFFER_COUNT; i++) {
            this.getFreeBuffers().addLast(ByteBuffer.wrap(new byte[HELPER_BUFFER_SIZE]).order(BYTE_ORDER));
        }
    }

    public void openServerSocket(InetAddress address, int tcpPort) throws IOException {
        ServerSocketChannel selectable = ServerSocketChannel.open();
        selectable.configureBlocking(false);

        ServerSocket ss = selectable.socket();
        if (address == null) {
            ss.bind(new InetSocketAddress(tcpPort));
        } else {
            ss.bind(new InetSocketAddress(address, tcpPort));
        }
        selectable.register(this.getSelector(), SelectionKey.OP_ACCEPT);
    }

    protected void initializeSelector() throws IOException {
        setName("SelectorThread-" + getId());
        this.setSelector(Selector.open());
    }

    protected ByteBuffer getPooledBuffer() {
        if (this.getFreeBuffers().isEmpty()) {
            return ByteBuffer.wrap(new byte[HELPER_BUFFER_SIZE]).order(BYTE_ORDER);
        }
        return this.getFreeBuffers().removeFirst();
    }

    public void recycleBuffer(ByteBuffer buf) {
        if (this.getFreeBuffers().size() < HELPER_BUFFER_COUNT) {
            buf.clear();
            this.getFreeBuffers().addLast(buf);
        }
    }

    public FastList<ByteBuffer> getFreeBuffers() {
        return _bufferPool;
    }

    public SelectionKey registerClientSocket(SelectableChannel sc, int interestOps) throws ClosedChannelException {
        SelectionKey sk = null;

        sk = sc.register(this.getSelector(), interestOps);
        //sk.attach(ob)
        return sk;
    }

	Lock process = new ReentrantLock();

    @Override
    public void run() {
        //System.out.println("Selector Started");
		_sThread = this;

		_floodWorker = new FloodWorker();
		_floodWorker.start();

        int totalKeys = 0;
        Iterator<SelectionKey> iter;
        SelectionKey key;
        MMOConnection<T> con;
        FastList.Node<MMOConnection<T>> n, end, temp;

        // main loop
        for (;;) {
            // check for shutdown
            if (this.isShuttingDown()) {
                this.closeSelectorThread();
                break;
            }

            try {
                totalKeys = getSelector().selectNow();
            } catch (IOException e) {
                //TODO logging
                e.printStackTrace();
            }
            //System.out.println("Selector Selected "+totalKeys);

            if (totalKeys > 0) {
                Set<SelectionKey> keys = getSelector().selectedKeys();
                iter = keys.iterator();

                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();

                    if (!key.isValid()) {
                        continue;
                    }
                    switch (key.readyOps()) {
                        case SelectionKey.OP_CONNECT:
                            this.finishConnection(key);
                            break;
                        case SelectionKey.OP_ACCEPT:
                            this.acceptConnection(key);
                            break;
                        case SelectionKey.OP_READ:
                            this.readPacket(key);
                            break;
                        case SelectionKey.OP_WRITE:
                            this.writePacket2(key);
                            break;
                        case SelectionKey.OP_READ | SelectionKey.OP_WRITE:
                            this.writePacket2(key);
                            // key might have been invalidated on writePacket
                            if (key.isValid()) {
                                this.readPacket(key);
                            }
                            break;
                    }

                }
            }

            // process pending close
			this.process.lock();
			try {
                for (n = this.getPendingClose().head(), end = this.getPendingClose().tail(); (n = n.getNext()) != end;) {
                    con = n.getValue();
					if (con == null) {
						continue;
					}

					if(con.getSendQueue().isEmpty())
					{
						temp = n.getPrevious();
						this.getPendingClose().delete(n);
						n = temp;
						this.closeConnectionImpl(con);
					}
                }
            }
			finally
			{
				process.unlock();
			}

            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @SuppressWarnings("unchecked")
    protected void finishConnection(SelectionKey key) {
        try {
            ((SocketChannel) key.channel()).finishConnect();
        } catch (IOException e) {
            MMOConnection<T> con = (MMOConnection<T>) key.attachment();
			if (con != null)
			{
				T client = con.getClient();
				client.getConnection().onForcedDisconnection();
				this.closeConnectionImpl(client.getConnection());
			}
        }

        // key might have been invalidated on finishConnect()
        if (key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
        }
    }

    protected void acceptConnection(SelectionKey key) {
        SocketChannel sc;
        try {
            while ((sc = ((ServerSocketChannel) key.channel()).accept()) != null) {
				Socket socket = sc.socket();
				InetAddress _ia = socket.getInetAddress();
				if (forbidden(_ia.getHostAddress()))
				{
					socket.close();
				}
				else if (this.getAcceptFilter() == null || this.getAcceptFilter().accept(sc)) {
                    sc.configureBlocking(false);
					socket.setTcpNoDelay(true);
					socket.setKeepAlive(false);
					socket.setSendBufferSize(4096);
					socket.setReceiveBufferSize(4096);
					socket.setSoTimeout(70000);
                    SelectionKey clientKey = sc.register(this.getSelector(), SelectionKey.OP_READ /*| SelectionKey.OP_WRITE*/);

                    MMOConnection<T> con = new MMOConnection<T>(this, new TCPSocket(sc.socket()), clientKey);
					T client = this.getClientFactory().create(con);
                    clientKey.attach(con);
					con.ia = _ia;
                } else {
					socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void readPacket(SelectionKey key) {
        if (key.channel() instanceof SocketChannel) {
            this.readTCPPacket(key);
        }
    }

    @SuppressWarnings("unchecked")
    protected void readTCPPacket(SelectionKey key) {
        MMOConnection<T> con = (MMOConnection<T>) key.attachment();
        T client = con.getClient();

        ByteBuffer buf;
        if ((buf = con.getReadBuffer()) == null) {
            buf = READ_BUFFER;
        }
        int result = -2;

        // if we try to to do a read with no space in the buffer it will read 0 bytes
        // going into infinite loop
        if (buf.position() == buf.limit()) {
            // should never happen
			con.onForcedDisconnection();
            closeConnectionImpl(con);
        }

        //System.out.println("POS ANTES SC.READ(): "+buf.position()+" limit: "+buf.limit()+" - buf: "+(buf == READ_BUFFER ? "READ_BUFFER" : "TEMP"));
        try {
            result = con.getReadableByteChannel().read(buf);
        } catch (IOException e) {
            //error handling goes bellow
        }

        //System.out.println("LEU: "+result+" pos: "+buf.position());
        if (result > 0) {
            // TODO this should be done vefore even reading
            if (!con.isClosed()) {
                buf.flip();
                // try to read as many packets as possible
                while (this.tryReadPacket2(key, client, buf)) {
                }
            } else if (buf == READ_BUFFER) {
                READ_BUFFER.clear();
            }
        } else if (result == 0) {
            // read interest but nothing to read? wtf?
			con.onForcedDisconnection();
            closeConnectionImpl(con);
		} else if (result == -1) {
            closeConnectionImpl(con);
        } else {
            con.onForcedDisconnection();
            closeConnectionImpl(con);
        }
    }

    @SuppressWarnings("unchecked")
    protected boolean tryReadPacket2(SelectionKey key, T client, ByteBuffer buf) {
        MMOConnection<T> con = client.getConnection();
        //System.out.println("BUFF POS ANTES DE LER: "+buf.position()+" - REMAINING: "+buf.remaining());

        if (buf.hasRemaining()) {
            TCPHeaderHandler<T> handler = _tcpHeaderHandler;
            // parse all jeaders
            HeaderInfo<T> ret;
            while (!handler.isChildHeaderHandler()) {
                handler.handleHeader(key, buf);
                handler = handler.getSubHeaderHandler();
            }
            // last header
            ret = handler.handleHeader(key, buf);

            if (ret != null) {
                int result = buf.remaining();

                // then check if header was processed
                if (ret.headerFinished()) {
                    // get expected packet size
                    int size = ret.getDataPending();

                    //System.out.println("IF: ("+size+" <= "+result+") => (size <= result)");
                    // do we got enough bytes for the packet?
                    if (size <= result) {
                        // avoid parsing dummy packets (packets without body)
                        if (size > 0) {
                            int pos = buf.position();
                            this.parseClientPacket(this.getPacketHandler(), buf, size, client);
                            buf.position(pos + size);
                        }

                        // if we are done with this buffer
                        if (!buf.hasRemaining()) {
                            //System.out.println("BOA 2");
                            if (buf != READ_BUFFER) {
                                con.setReadBuffer(null);
                                this.recycleBuffer(buf);
                            } else {
                                READ_BUFFER.clear();
                            }

                            return false;
                        }

                        return true;
                    }
					if (con.ia != null)
					{
						vBanSuku(con.ia.getHostAddress(), Config.BAN_TIME, true);
						LogWrite.add(TimeLogger.getLogTime() + " IP: " + con.ia.getHostAddress(), "forbidden");
					}
                    // we dont have enough bytes for the dataPacket so we need to read
                    client.getConnection().enableReadInterest();

                    //System.out.println("LIMIT "+buf.limit());
                    if (buf == READ_BUFFER) {
                        buf.position(buf.position() - HEADER_SIZE);
                        this.allocateReadBuffer(con);
                    } else {
                        buf.position(buf.position() - HEADER_SIZE);
                        buf.compact();
                    }
                    return false;
                }
                // we dont have enough data for header so we need to read
                client.getConnection().enableReadInterest();

                if (buf == READ_BUFFER) {
                    this.allocateReadBuffer(con);
                } else {
                    buf.compact();
                }
                return false;
            }
            // null ret means critical error
            // kill the connection
            this.closeConnectionImpl(con);
            return false;
        }
        //con.disableReadInterest();
        return false; //empty buffer
    }

    protected void allocateReadBuffer(MMOConnection<T> con) {
        //System.out.println("con: "+Integer.toHexString(con.hashCode()));
        //Util.printHexDump(READ_BUFFER);
        con.setReadBuffer(this.getPooledBuffer().put(READ_BUFFER));
        READ_BUFFER.clear();
    }

    protected void parseClientPacket(IPacketHandler<T> handler, ByteBuffer buf, int dataSize, T client) {
        int pos = buf.position();

        boolean ret = client.decrypt(buf, dataSize);

        //System.out.println("pCP -> BUF: POS: "+buf.position()+" - LIMIT: "+buf.limit()+" == Packet: SIZE: "+dataSize);
        if (buf.hasRemaining() && ret) {
            //  apply limit
            int limit = buf.limit();
            buf.limit(pos + dataSize);
            //System.out.println("pCP2 -> BUF: POS: "+buf.position()+" - LIMIT: "+buf.limit()+" == Packet: SIZE: "+size);
            ReceivablePacket<T> cp = handler.handlePacket(buf, client);

            if (cp != null) {
                cp.setByteBuffer(buf);
                cp.setClient(client);

                if (cp.read()) {
					getExecutor().execute(cp);
                }
                //this.getExecutor().execute(cp);
            }
            buf.limit(limit);
        }
    }

    protected void prepareWriteBuffer(T client, SendablePacket<T> sp) {
        WRITE_BUFFER.clear();

        //set the write buffer
        sp.setByteBuffer(WRITE_BUFFER);

        // reserve space for the size
        int headerPos = sp.getByteBuffer().position();
        int headerSize = sp.getHeaderSize();
        sp.getByteBuffer().position(headerPos + headerSize);

        // write contents
        sp.write();

        int dataSize = sp.getByteBuffer().position() - headerPos - headerSize;
        sp.getByteBuffer().position(headerPos + headerSize);
        client.encrypt(sp.getByteBuffer(), dataSize);

        // write size
        sp.writeHeader(dataSize);
        //sp.writeHeader(HEADER_TYPE, headerPos);
    }

    @SuppressWarnings("unchecked")
    protected void writePacket2(SelectionKey key) {
        MMOConnection<T> con = (MMOConnection<T>) key.attachment();
		T client = con.getClient();

        this.prepareWriteBuffer2(con);
        DIRECT_WRITE_BUFFER.flip();

        int size = DIRECT_WRITE_BUFFER.remaining();

        //System.err.println("WRITE SIZE: "+size);
        int result = -1;

        try {
            result = con.getWritableChannel().write(DIRECT_WRITE_BUFFER);
        } catch (IOException e) {
            // error handling goes on the if bellow
            //System.err.println("IOError: " + e.getMessage());
        }

        // check if no error happened
        if (result >= 0) {
            // check if we writed everything
            if (result == size) {
				Lock send = new ReentrantLock();
				send.lock();
				try {
                    if (con.getSendQueue().isEmpty() && !con.hasPendingWriteBuffer()) {
                        con.disableWriteInterest();
                    }
                }
				finally
				{
					send.unlock();
				}
            } else {
                con.createWriteBuffer(DIRECT_WRITE_BUFFER);
            }
            //System.err.println("DEBUG: INCOMPLETE WRITE - write size: "+size);
            //System.err.flush();

            if (result == 0) {
                //System.err.println("DEBUG: write result: 0 - write size: "+size+" - DWB rem: "+DIRECT_WRITE_BUFFER.remaining());
                //System.err.flush();
            }

        } else {
            //System.err.println("IOError: "+result);
            //System.err.flush();
            con.onForcedDisconnection();
            this.closeConnectionImpl(con);
        }
    }

    protected void prepareWriteBuffer2(MMOConnection<T> con) {
        DIRECT_WRITE_BUFFER.clear();

        // if theres pending content add it
        if (con.hasPendingWriteBuffer()) {
            con.movePendingWriteBufferTo(DIRECT_WRITE_BUFFER);
        }
        //System.err.println("ADDED PENDING TO DIRECT "+DIRECT_WRITE_BUFFER.position());

        if (DIRECT_WRITE_BUFFER.remaining() > 1 && !con.hasPendingWriteBuffer()) {

            int i = 0;

            FastList<SendablePacket<T>> sendQueue = con.getSendQueue();
            Node<SendablePacket<T>> n, temp, end;
            SendablePacket<T> sp;

			Lock send = new ReentrantLock();
			send.lock();
			try {
                for (n = sendQueue.head(), end = sendQueue.tail(); (n = n.getNext()) != end && i++ < MAX_SEND_PER_PASS;) {
                    sp = n.getValue();
                    // put into WriteBuffer
                    putPacketIntoWriteBuffer(con.getClient(), sp);

                    // delete packet from queue
                    temp = n.getPrevious();
                    sendQueue.delete(n);
                    n = temp;

                    WRITE_BUFFER.flip();
                    if (DIRECT_WRITE_BUFFER.remaining() >= WRITE_BUFFER.limit()) {
                        DIRECT_WRITE_BUFFER.put(WRITE_BUFFER);
                    } else {
                        // there is no more space in the direct buffer
                        //con.addWriteBuffer(this.getPooledBuffer().put(WRITE_BUFFER));
                        con.createWriteBuffer(WRITE_BUFFER);
                        break;
                    }
                }
            }
			finally
			{
				send.unlock();
			}
        }
    }

    protected final void putPacketIntoWriteBuffer(T client, SendablePacket<T> sp) {
        WRITE_BUFFER.clear();

        // set the write buffer
        sp.setByteBuffer(WRITE_BUFFER);

        // reserve space for the size
        int headerPos = sp.getByteBuffer().position();
        int headerSize = sp.getHeaderSize();
        sp.getByteBuffer().position(headerPos + headerSize);

        // write content to buffer
        sp.write();

        // size (incl header)
        int dataSize = sp.getByteBuffer().position() - headerPos - headerSize;

        sp.getByteBuffer().position(headerPos + headerSize);
        client.encrypt(sp.getByteBuffer(), dataSize);

        // recalculate size after encryption
        dataSize = sp.getByteBuffer().position() - headerPos - headerSize;

        // prepend header
        //this.prependHeader(headerPos, size);
        sp.getByteBuffer().position(headerPos);
        sp.writeHeader(dataSize);
        sp.getByteBuffer().position(headerPos + headerSize + dataSize);
    }

    /*protected void prependHeader(int pos, int size)
     {
     switch (HEADER_TYPE)
     {
     case BYTE_HEADER:
     WRITE_BUFFER.put(pos, (byte) size);
     break;
     case SHORT_HEADER:
     WRITE_BUFFER.putShort(pos, (short) size);
     break;
     case INT_HEADER:
     WRITE_BUFFER.putInt(pos, size);
     break;
     }
     }*/

    /*protected int getHeaderValue(ByteBuffer buf)
     {
     switch (HEADER_TYPE)
     {
     case BYTE_HEADER:
     return buf.get() & 0xFF;
     case SHORT_HEADER:
     return buf.getShort() & 0xFFFF;
     case INT_HEADER:
     return buf.getInt();
     }
     return -1; // O.o
     }*/
    protected void setSelector(Selector selector) {
        _selector = selector;
    }

    public Selector getSelector() {
        return _selector;
    }

    protected void setExecutor(IMMOExecutor<T> executor) {
        _executor = executor;
    }

    protected IMMOExecutor<T> getExecutor() {
        return _executor;
    }

    public IPacketHandler<T> getPacketHandler() {
        return _packetHandler;
    }

    protected void setClientFactory(IClientFactory<T> clientFactory) {
        _clientFactory = clientFactory;
    }

    public IClientFactory<T> getClientFactory() {
        return _clientFactory;
    }

    public void setAcceptFilter(IAcceptFilter acceptFilter) {
        _acceptFilter = acceptFilter;
    }

    public IAcceptFilter getAcceptFilter() {
        return _acceptFilter;
    }

    public void closeConnection(MMOConnection<T> con) {
		synchronized (getPendingClose())
		{
			getPendingClose().addLast(con);
		}
    }

    protected void closeConnectionImpl(MMOConnection<T> con) {
		try {
			if (con.ia != null) {
				remove(con.ia.getHostAddress());
			}
			// notify connection
			con.onDisconnection();
		} finally {
            try {
                // close socket and the SocketChannel
                con.getSocket().close();
            } catch (IOException e) {
                // ignore, we are closing anyway
            } finally {
                con.releaseBuffers();
                // clear attachment
                con.getSelectionKey().attach(null);
                // cancel key
                con.getSelectionKey().cancel();
            }
        }
    }

    protected FastList<MMOConnection<T>> getPendingClose() {
        return _pendingClose;
    }

    public void shutdown() {
        _shutdown = true;
    }

    public boolean isShuttingDown() {
        return _shutdown;
    }

    protected void closeAllChannels() {
        Set<SelectionKey> keys = this.getSelector().keys();
        for (SelectionKey key : keys) {
            try {
                key.channel().close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    protected void closeSelectorThread() {
        this.closeAllChannels();
        try {
            this.getSelector().close();
        } catch (IOException e) {
            // Ignore
        }
    }

	protected void clearFlood()
	{
		IpInfo info = null;
		Long now_time = System.currentTimeMillis();
		for(Map.Entry<String, IpInfo> entry : this._connects.entrySet())
		{
			String address = entry.getKey();
			info = entry.getValue();
			if(address == null || info == null)
			{
				continue;
			}

			if(now_time > info._l)
			{
				_connects.remove(address);
			}
		}
		info = null;
	}

	public void vBanSuku(String address, long duration)
	{
		long bantime = System.currentTimeMillis() + duration;
		IpInfo ban = new IpInfo(10, bantime);
		updateIpInfo(address, ban);
	}

	public void vBanSuku(String address, long duration, boolean ddos)
	{
		long bantime = System.currentTimeMillis() + duration;
		IpInfo ban = new IpInfo(1, 0, 1);
		updateIpInfo(address, ban);
	}

	private IpInfo getIpInfo(String address)
	{
		return _connects.get(address);
	}

	private void updateIpInfo(String address, IpInfo info)
	{
		_connects.put(address, info);
	}

	public void remove(String address)
	{
		IpInfo ip = getIpInfo(address);
		if(ip == null)
		{
			return;
		}
		int count = ip._c;
		long last = ip._l;

		count--;
		if(count == 0)
		{
			count = 1;
		}
		last = System.currentTimeMillis() + Config.INTERVAL;
		IpInfo update = new IpInfo(count, last);
		updateIpInfo(address, update);
	}

	private boolean isWhite(String ip)
	{
		if(whiteIPs.contains(ip))
		{
			return true;
		}
		return false;
	}

	private boolean isBlack(String ip)
	{
		return Config.blackIPs.contains(ip);
	}

	private boolean forbidden(String address)
	{
		if(isBlack(address))
		{
			return true;
		}
		if(!Config.ENABLE_ANTIBRUTE)
		{
			return false;
		}
		Long now_time = System.currentTimeMillis();
		IpInfo ip = getIpInfo(address);
		if(ip == null)
		{
			ip = new IpInfo(1, now_time + Config.INTERVAL);
			updateIpInfo(address, ip);

			return false;
		}
		int ddos = ip._d;
		if(ddos == 1)
		{
			return true;
		}
		int count = ip._c;
		long last = ip._l;
		if(now_time - last < 0)
		{
			return true;
		}
		if(count >= Config.MAX_ATTEMPTS)
		{
			return true;
		}
		last = now_time + Config.INTERVAL;
		if(count >= 2)
		{
			last = now_time + Config.INTERVAL * count;
		}
		count++;
		updateIpInfo(address, new IpInfo(count, last));
		return false;
	}
}
