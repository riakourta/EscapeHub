package gr.ihu.escapehub.lobby.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.lobby.dto.LobbyMessageDto;
import gr.ihu.escapehub.lobby.dto.LobbyStateResponseDto;
import gr.ihu.escapehub.lobby.dto.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LobbyHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(LobbyHandler.class);

    private final Map<WebSocketSession, String> players = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final int minPlayers;

    public LobbyHandler(
            ObjectMapper objectMapper,
            @Value("${escapehub.lobby.min-players:4}") int minPlayers) {
        this.objectMapper = objectMapper;
        this.minPlayers = minPlayers;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.debug("Session connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        LobbyMessageDto incomingMsg = objectMapper.readValue(message.getPayload(), LobbyMessageDto.class);

        if (incomingMsg.type() == null) {
            sendMessageToSession(session, buildStateDto(MessageType.ERROR));
            return;
        }

        switch (incomingMsg.type()) {
            case JOIN_LOBBY -> handleJoinLobby(session, incomingMsg);
            case START_HEIST -> handleStartHeist(session);
            default -> sendMessageToSession(session, buildStateDto(MessageType.ERROR));
        }
    }

    private void handleJoinLobby(WebSocketSession session, LobbyMessageDto msg) throws IOException {
        if (msg.username() == null || msg.username().isBlank()) {
            sendMessageToSession(session, buildStateDto(MessageType.ERROR));
            return;
        }

        players.put(session, msg.username().trim());
        broadcastMessage(buildStateDto(MessageType.LOBBY_UPDATE));
    }

    private void handleStartHeist(WebSocketSession requesterSession) throws IOException {
        // Έλεγχος αν ο παίκτης έχει κάνει πρώτα JOIN και αν συμπληρώθηκε ο ελάχιστος αριθμός
        if (players.containsKey(requesterSession) && players.size() >= minPlayers) {
            broadcastMessage(buildStateDto(MessageType.GAME_STARTING));
        } else {
            sendMessageToSession(requesterSession, buildStateDto(MessageType.ERROR));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (players.remove(session) != null) {
            broadcastMessage(buildStateDto(MessageType.LOBBY_UPDATE));
        }
    }

    private LobbyStateResponseDto buildStateDto(MessageType type) {
        return new LobbyStateResponseDto(
                type,
                players.size(),
                minPlayers,
                players.values()
        );
    }

    private void broadcastMessage(LobbyStateResponseDto responseDto) throws IOException {
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(responseDto));
        for (WebSocketSession s : players.keySet()) {
            if (s.isOpen()) {
                s.sendMessage(textMessage);
            }
        }
    }

    private void sendMessageToSession(WebSocketSession session, LobbyStateResponseDto responseDto) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(responseDto)));
        }
    }

    public int getConnectedPlayersCount() {
        return players.size();
    }
}