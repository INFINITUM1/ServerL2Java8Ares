package ru.agecold.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;
import java.util.logging.Logger;

import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.network.L2GameClient;
import ru.agecold.gameserver.network.serverpackets.L2GameServerPacket;
import ru.agecold.util.Log;
import ru.agecold.util.TimeLogger;
import org.mmocore.network.nio.impl.ReceivablePacket;

/**
 * Packets received by the game server from clients
 *
 * @author KenM
 */
public abstract class L2GameClientPacket extends ReceivablePacket<L2GameClient> {

    private static final Logger _log = Logger.getLogger(L2GameClientPacket.class.getName());

    @Override
    protected boolean read() {
        //System.out.println(getClient().toString() + "Client: " + getType());
        L2PcInstance player = getClient().getActiveChar();
        if (player != null && player.isSpy()) {
            Log.add(TimeLogger.getLogTime() + " Player: " + player.getName() + "// " + getClient().toString() + "Clent: " + getType(), "packet_spy");
        }
        try {
            readImpl();
            return true;
        } catch (BufferUnderflowException e) {
            _log.warning("Client: " + _client + " - Failed reading: " + getType() + ". Buffer underflow!");
        } catch (Exception e) {
            _log.warning("Client: " + _client + " - Failed reading: " + getType() + " //" + e);
        }
        _client.onPacketReadFail();
        return false;
    }

    protected abstract void readImpl();

    @Override
    public void run() {
        try {
            runImpl();
        } catch (Exception e) {
            _log.warning("Client: " + _client + " - Failed running: " + getType() + " //" + e);
        }
    }

    protected abstract void runImpl();

    protected final void sendPacket(L2GameServerPacket gsp) {
        getClient().sendPacket(gsp);
    }

    /**
     * @return A String with this packet name for debuging purposes
     */
    public String getType() {
        return "C." + getClass().getSimpleName();
    }
}
