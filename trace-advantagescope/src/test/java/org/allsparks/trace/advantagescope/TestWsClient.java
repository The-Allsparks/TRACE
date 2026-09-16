package org.allsparks.trace.advantagescope;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/** Desktop WebSocket client for protocol tests. Not used on the Control Hub. */
final class TestWsClient implements WebSocket.Listener {
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final StringBuilder partial = new StringBuilder();
    private WebSocket socket;

    static TestWsClient connect(int port) throws Exception {
        TestWsClient client = new TestWsClient();
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        client.socket = http.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .buildAsync(URI.create("ws://127.0.0.1:" + port), client)
                .get(3, TimeUnit.SECONDS);
        return client;
    }

    void send(String text) {
        socket.sendText(text, true).join();
    }

    List<String> messages() {
        return new ArrayList<>(messages);
    }

    boolean waitFor(String substring, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (String message : messages) {
                if (message.contains(substring)) {
                    return true;
                }
            }
            Thread.sleep(20L);
        }
        return false;
    }

    void close() {
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "test").join();
            } catch (Exception ignored) {
                socket.abort();
            }
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        partial.append(data);
        if (last) {
            messages.add(partial.toString());
            partial.setLength(0);
        }
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        // Protocol tests assert on received JSON, not transport errors.
    }
}
