package crsm;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Supabase Realtime subscription for technician availability changes. */
final class TechnicianRealtimeClient implements AutoCloseable {
    private static final String TOPIC = "realtime:crsm-technicians";
    private final String projectUrl;
    private final String anonKey;
    private final String accessToken;
    private final Runnable onChange;
    private final Consumer<String> onState;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "crsm-technician-realtime"); thread.setDaemon(true); return thread;
    });
    private final AtomicLong refs = new AtomicLong(1);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private final AtomicBoolean heartbeatStarted = new AtomicBoolean();
    private volatile WebSocket socket;
    private volatile boolean closed;
    private int reconnectAttempts;

    TechnicianRealtimeClient(UserSession session, Runnable onChange, Consumer<String> onState) {
        String url = AppConfig.get("SUPABASE_URL");
        String key = AppConfig.get("SUPABASE_ANON_KEY");
        if (url == null || url.isBlank() || key == null || key.isBlank()) {
            throw new IllegalStateException("Set SUPABASE_URL and SUPABASE_ANON_KEY to receive technician availability updates.");
        }
        this.projectUrl = url.replaceAll("/+$", "");
        this.anonKey = key;
        this.accessToken = session.accessToken();
        this.onChange = onChange;
        this.onState = onState;
    }

    void start() {
        onState.accept("Connecting to live availability…");
        connect();
    }

    private void connect() {
        if (closed) return;
        String endpoint = projectUrl.replaceFirst("^https://", "wss://").replaceFirst("^http://", "ws://")
                + "/realtime/v1/websocket?apikey="
                + URLEncoder.encode(anonKey, StandardCharsets.UTF_8) + "&vsn=2.0.0";
        http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(12))
                .buildAsync(URI.create(endpoint), new Listener())
                .whenComplete((created, error) -> {
                    if (error != null) scheduleReconnect(error.getMessage());
                    else socket = created;
                });
    }

    private void join(WebSocket webSocket) {
        Map<String,Object> change = Map.of("event", "*", "schema", "public", "table", "technicians",
                "select", List.of("technician_id", "technician_name", "availability"));
        Map<String,Object> config = Map.of("broadcast", Map.of("ack", false, "self", false),
                "presence", Map.of("key", ""), "postgres_changes", List.of(change), "private", false);
        Map<String,Object> payload = Map.of("config", config, "access_token", accessToken);
        send(webSocket, frame(null, Long.toString(refs.getAndIncrement()), TOPIC, "phx_join", payload));
    }

    private List<Object> frame(String joinRef, String ref, String topic, String event, Object payload) {
        List<Object> message = new ArrayList<>();
        message.add(joinRef); message.add(ref); message.add(topic); message.add(event); message.add(payload);
        return message;
    }

    private void send(WebSocket webSocket, Object message) {
        webSocket.sendText(JsonCodec.stringify(message), true);
    }

    private void scheduleReconnect(String reason) {
        if (closed || !reconnectScheduled.compareAndSet(false, true)) return;
        onState.accept("Live updates disconnected; retrying. " + (reason == null ? "" : reason));
        int delay = Math.min(30, 1 << Math.min(reconnectAttempts++, 4));
        scheduler.schedule(() -> { reconnectScheduled.set(false); connect(); }, delay, TimeUnit.SECONDS);
    }

    @Override public void close() {
        closed = true;
        scheduler.shutdownNow();
        WebSocket current = socket;
        if (current != null) current.sendClose(WebSocket.NORMAL_CLOSURE, "Window closed");
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder fragments = new StringBuilder();

        @Override public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            reconnectAttempts = 0;
            reconnectScheduled.set(false);
            join(webSocket);
            if (heartbeatStarted.compareAndSet(false, true)) scheduler.scheduleAtFixedRate(() -> {
                WebSocket current = socket;
                if (!closed && current != null) send(current, frame(null, Long.toString(refs.getAndIncrement()), "phoenix", "heartbeat", Map.of()));
            }, 20, 20, TimeUnit.SECONDS);
            webSocket.request(1);
        }

        @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            fragments.append(data);
            if (last) {
                String text = fragments.toString(); fragments.setLength(0);
                handleFrame(text);
            }
            webSocket.request(1);
            return null;
        }

        private void handleFrame(String text) {
            try {
                Object parsed = JsonCodec.parse(text);
                if (!(parsed instanceof List<?> message) || message.size() < 5) return;
                String event = String.valueOf(message.get(3));
                if (event.equals("postgres_changes")) onChange.run();
                if (event.equals("phx_reply") && message.get(4) instanceof Map<?,?> reply) {
                    Object status = reply.get("status");
                    if ("ok".equals(status)) onState.accept("Live availability updates connected");
                    else if ("error".equals(status)) scheduleReconnect(String.valueOf(reply.get("response")));
                }
            } catch (RuntimeException ignored) {
                onState.accept("Received an unreadable realtime message; use Refresh if needed.");
            }
        }

        @Override public void onError(WebSocket webSocket, Throwable error) { scheduleReconnect(error.getMessage()); }

        @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            scheduleReconnect("Connection closed (" + statusCode + ").");
            return null;
        }
    }
}
