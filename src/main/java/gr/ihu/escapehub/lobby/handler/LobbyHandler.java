package gr.ihu.escapehub.lobby.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.lobby.dto.LobbyMessageDto;
import gr.ihu.escapehub.lobby.dto.LobbyStateResponseDto;
import gr.ihu.escapehub.lobby.dto.MessageType;
import gr.ihu.escapehub.state.service.MuseumRoom;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class LobbyHandler extends TextWebSocketHandler {
    private final Map<WebSocketSession, MuseumRoom.Member> players = new LinkedHashMap<>();
    private final ObjectMapper objectMapper;
    private final int minPlayers;
    private final MuseumRoom room;
    public LobbyHandler(ObjectMapper objectMapper,
            @Value("${escapehub.lobby.min-players:4}") int minPlayers, MuseumRoom room) {
        if (minPlayers != 4) { throw new IllegalArgumentException("This museum requires exactly four players"); }
        this.objectMapper = objectMapper;
        this.minPlayers = minPlayers;
        this.room = room;
    }
    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.setTextMessageSizeLimit(1024);
        if (session.getPrincipal() == null) { session.close(CloseStatus.POLICY_VIOLATION); }
    }
    @Override
    protected synchronized void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (session.getPrincipal() == null) { return; }
        LobbyMessageDto incoming;
        try { incoming = objectMapper.readValue(message.getPayload(), LobbyMessageDto.class); }
        catch (IOException e) { send(session, state(MessageType.ERROR)); return; }
        if (incoming == null || incoming.type() == null) { send(session, state(MessageType.ERROR)); return; }
        String id = session.getPrincipal().getName();
        if (room.started()) {
            if (room.contains(id)) { send(session, state(MessageType.GAME_STARTING)); }
            else { session.close(new CloseStatus(1008, "Museum already in progress")); }
            return;
        }
        if (incoming.type() == MessageType.JOIN_LOBBY) {
            if (players.keySet().stream().anyMatch(s -> s != session
                    && s.getPrincipal().getName().equals(id)) || (!players.containsKey(session) && players.size() >= 4)) {
                session.close(new CloseStatus(1008, "Room full or account already connected")); return;
            }
            String raw = incoming.username() == null ? "" : incoming.username();
            String[] parts = raw.split("\\|", 2);
            String color = parts[0].toLowerCase();
            if (!Set.of("blue", "pink", "purple", "green", "red", "yellow").contains(color)) { color = "blue"; }
            String nickname = parts.length == 2 && !parts[1].isBlank() ? parts[1].trim() : id;
            nickname = nickname.substring(0, Math.min(40, nickname.length()));
            players.put(session, new MuseumRoom.Member(id, nickname, color));
            broadcast(state(MessageType.LOBBY_UPDATE));
        } else if (incoming.type() == MessageType.START_HEIST) {
            if (players.containsKey(session) && players.size() == minPlayers) {
                room.start(players.values());
                broadcast(state(MessageType.GAME_STARTING));
            } else { send(session, state(MessageType.ERROR)); }
        } else { send(session, state(MessageType.ERROR)); }
    }
    @Override
    public synchronized void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (players.remove(session) != null) { broadcast(state(MessageType.LOBBY_UPDATE)); }
    }
    private LobbyStateResponseDto state(MessageType type) {
        return new LobbyStateResponseDto(type, players.size(), minPlayers,
                players.values().stream().map(p -> p.color() + "|" + p.nickname()).toList());
    }
    private void broadcast(LobbyStateResponseDto state) throws IOException {
        for (WebSocketSession session : java.util.List.copyOf(players.keySet())) { send(session, state); }
    }
    private void send(WebSocketSession session, LobbyStateResponseDto state) throws IOException {
        if (session.isOpen()) {
            try { session.sendMessage(new TextMessage(objectMapper.writeValueAsString(state))); }
            catch (IOException e) {
                try { session.close(CloseStatus.SERVER_ERROR); }
                catch (IOException ignored) { /* Transport is already unavailable. */ }
            }
        }
    }
    public synchronized int getConnectedPlayersCount() { return players.size(); }
}
