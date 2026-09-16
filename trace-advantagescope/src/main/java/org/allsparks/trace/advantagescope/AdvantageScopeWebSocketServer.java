package org.allsparks.trace.advantagescope;

import fi.iki.elonen.NanoWSD;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone WebSocket listen on the FTC Dashboard port AdvantageScope already
 * uses ({@code ws://&lt;hub&gt;:8000} by default). Does not serve the Dashboard UI.
 */
final class AdvantageScopeWebSocketServer extends NanoWSD {
    private final CopyOnWriteArrayList<LiveSocket> clients = new CopyOnWriteArrayList<>();
    private final AtomicInteger reconnectCount = new AtomicInteger();
    private final String opModeName;
    private final AtomicInteger everConnected = new AtomicInteger();

    AdvantageScopeWebSocketServer(int port, String opModeName) {
        super(port);
        this.opModeName = opModeName == null ? "unknown" : opModeName;
    }

    int clientCount() {
        return clients.size();
    }

    int reconnectCount() {
        return reconnectCount.get();
    }

    /**
     * Broadcast one payload to every socket. Slow or dead clients are dropped
     * instead of growing a send queue.
     *
     * @return number of successful sends
     */
    int broadcast(String payload) {
        int sent = 0;
        Iterator<LiveSocket> iterator = clients.iterator();
        while (iterator.hasNext()) {
            LiveSocket socket = iterator.next();
            try {
                socket.send(payload);
                sent++;
            } catch (IOException failed) {
                clients.remove(socket);
                closeQuietly(socket);
            }
        }
        return sent;
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new LiveSocket(handshake);
    }

    void shutdown() {
        for (LiveSocket socket : clients) {
            closeQuietly(socket);
        }
        clients.clear();
        stop();
    }

    private static void closeQuietly(LiveSocket socket) {
        try {
            socket.close(WebSocketFrame.CloseCode.NormalClosure, "TRACE live closed", false);
        } catch (Exception ignored) {
            // Live visualization is expendable.
        }
    }

    final class LiveSocket extends WebSocket {
        LiveSocket(IHTTPSession handshake) {
            super(handshake);
        }

        @Override
        protected void onOpen() {
            if (everConnected.getAndIncrement() > 0) {
                reconnectCount.incrementAndGet();
            }
            clients.add(this);
        }

        @Override
        protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            clients.remove(this);
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            String text = message.getTextPayload();
            if (!AdvantageScopeJson.isGetRobotStatus(text)) {
                return;
            }
            try {
                send(AdvantageScopeJson.robotStatus(opModeName, true));
            } catch (IOException ignored) {
                clients.remove(this);
            }
        }

        @Override
        protected void onPong(WebSocketFrame pong) {
            // Heartbeat is GET_ROBOT_STATUS, not WebSocket ping.
        }

        @Override
        protected void onException(IOException exception) {
            clients.remove(this);
        }
    }
}
