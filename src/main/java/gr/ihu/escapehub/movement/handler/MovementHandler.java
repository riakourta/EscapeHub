package gr.ihu.escapehub.movement.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** One authenticated, four-player museum room. State is local to one server. */
@Component
public class MovementHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> players = new LinkedHashMap<>();
    private boolean started;

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getPrincipal() == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        String id = session.getPrincipal().getName();
        if (sessions.containsKey(id) || sessions.size() >= 4) {
            session.close(new CloseStatus(1008, "Room full or player already connected"));
            return;
        }
        session.setTextMessageSizeLimit(1024);
        sessions.put(id, session);
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("id", id);
        player.put("x", 80 + players.size() * 80);
        player.put("y", 100);
        player.put("ready", false);
        players.put(id, player);
        send(session, Map.of("type", "welcome", "id", id));
        broadcastSnapshot();
    }

    @Override
    protected synchronized void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (session.getPrincipal() == null) { return; }
        String id = session.getPrincipal().getName();
        if (sessions.get(id) != session) { return; }
        JsonNode event;
        try { event = mapper.readTree(message.getPayload()); }
        catch (IOException e) { send(session, Map.of("type", "error", "message", "Invalid JSON")); return; }
        if (event == null || !event.isObject()) { return; }
        if ("ready".equals(event.path("type").asText())) {
            players.get(id).put("ready", true);
            if (players.size() == 4 && players.values().stream().allMatch(v -> Boolean.TRUE.equals(v.get("ready")))) {
                started = true;
            }
            broadcastSnapshot();
        } else if ("move".equals(event.path("type").asText()) && started) {
            JsonNode x = event.path("x");
            JsonNode y = event.path("y");
            if (!x.isNumber() || !y.isNumber() || !Double.isFinite(x.asDouble())
                    || !Double.isFinite(y.asDouble()) || x.asDouble() < 12 || x.asDouble() > 788
                    || y.asDouble() < 12 || y.asDouble() > 488) { return; }
            players.get(id).put("x", x.asDouble());
            players.get(id).put("y", y.asDouble());
            Map<String, Object> update = Map.of("type", "move", "id", id, "x", x.asDouble(), "y", y.asDouble());
            for (WebSocketSession peer : sessions.values()) {
                if (peer != session) { send(peer, update); }
            }
        }
    }

    @Override
    public synchronized void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (session.getPrincipal() == null) { return; }
        String id = session.getPrincipal().getName();
        if (sessions.get(id) != session) { return; }
        sessions.remove(id);
        players.remove(id);
        if (sessions.isEmpty()) { started = false; }
        broadcastSnapshot();
    }

    private void broadcastSnapshot() throws IOException {
        for (WebSocketSession session : sessions.values()) {
            send(session, Map.of("type", "snapshot", "started", started, "players", players.values()));
        }
    }

    private void send(WebSocketSession session, Object data) throws IOException {
        if (session.isOpen()) { session.sendMessage(new TextMessage(mapper.writeValueAsString(data))); }
    }
}
