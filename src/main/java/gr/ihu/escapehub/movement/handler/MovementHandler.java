package gr.ihu.escapehub.movement.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.state.service.MuseumRoom;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MovementHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> players = new LinkedHashMap<>();
    private final MuseumRoom room;
    public MovementHandler(MuseumRoom room) { this.room = room; }
    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getPrincipal() == null || !room.contains(session.getPrincipal().getName())) {
            session.close(new CloseStatus(1008, "Start with four players in the lobby")); return;
        }
        String id = session.getPrincipal().getName();
        if (sessions.containsKey(id) || sessions.size() >= 4) {
            session.close(new CloseStatus(1008, "Account already connected or museum full")); return;
        }
        session.setTextMessageSizeLimit(1024);
        sessions.put(id, session);
        MuseumRoom.Member member = room.member(id);
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("id", id);
        player.put("nickname", member.nickname());
        player.put("color", member.color());
        player.put("x", 280 + room.slot(id) * 60);
        player.put("y", 500);
        player.put("direction", "up");
        players.put(id, player);
        send(session, Map.of("type", "welcome", "id", id));
        snapshot();
    }
    @Override
    protected synchronized void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (session.getPrincipal() == null) { return; }
        String id = session.getPrincipal().getName();
        if (sessions.get(id) != session) { return; }
        JsonNode event;
        try { event = mapper.readTree(message.getPayload()); }
        catch (IOException e) { return; }
        if (event == null || !"move".equals(event.path("type").asText())) { return; }
        JsonNode x = event.path("x"), y = event.path("y");
        if (!x.isNumber() || !y.isNumber() || !Double.isFinite(x.asDouble())
                || !Double.isFinite(y.asDouble()) || collision(x.asDouble(), y.asDouble())) { return; }
        String direction = event.path("direction").asText("up");
        if (!Set.of("up", "down", "left", "right").contains(direction)) { return; }
        Map<String, Object> player = players.get(id);
        player.put("x", x.asDouble()); player.put("y", y.asDouble()); player.put("direction", direction);
        Map<String, Object> update = Map.of("type", "move", "id", id,
                "x", x.asDouble(), "y", y.asDouble(), "direction", direction);
        for (WebSocketSession peer : java.util.List.copyOf(sessions.values())) { if (peer != session) { send(peer, update); } }
        // Same 10px touch margin used by the museum renderer, now shared by the server.
        if (!room.alarm() && overlaps(x.asDouble() - 10, y.asDouble() - 10, 50, 50, 350, 360, 90, 70)) {
            room.triggerAlarm(); snapshot();
        }
    }
    private boolean collision(double x, double y) {
        if (x < 20 || y < 20 || x > 750 || y > 550) { return true; }
        if (overlaps(x, y, 30, 30, 350, 360, 90, 70)) { return true; }
        return room.alarm() && (overlaps(x, y, 30, 30, 180, 150, 70, 70)
                || overlaps(x, y, 30, 30, 520, 150, 70, 70));
    }
    private boolean overlaps(double x, double y, double w, double h, double ox, double oy, double ow, double oh) {
        return x < ox + ow && x + w > ox && y < oy + oh && y + h > oy;
    }
    @Override
    public synchronized void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (session.getPrincipal() == null) { return; }
        String id = session.getPrincipal().getName();
        if (sessions.get(id) != session) { return; }
        sessions.remove(id); players.remove(id);
        if (sessions.isEmpty()) { room.reset(); }
        snapshot();
    }
    private void snapshot() throws IOException {
        for (WebSocketSession session : java.util.List.copyOf(sessions.values())) {
            send(session, Map.of("type", "snapshot", "started", room.started(),
                    "alarm", room.alarm(), "players", players.values()));
        }
    }
    private void send(WebSocketSession session, Object data) throws IOException {
        if (session.isOpen()) {
            try { session.sendMessage(new TextMessage(mapper.writeValueAsString(data))); }
            catch (IOException e) {
                try { session.close(CloseStatus.SERVER_ERROR); }
                catch (IOException ignored) { /* Transport is already unavailable. */ }
            }
        }
    }
}
