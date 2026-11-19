package gr.ihu.escapehub.lobby.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.lobby.dto.ChatMessageDto;
import gr.ihu.escapehub.lobby.dto.LobbyMessageDto;
import gr.ihu.escapehub.lobby.dto.LobbyStateResponseDto;
import gr.ihu.escapehub.lobby.dto.MessageType;
import gr.ihu.escapehub.state.service.GameStateManager;
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
    private static final String DEFAULT_CHAT_COLOR = "#FFFFFF";

    private final Map<WebSocketSession, String> players = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final GameStateManager gameStateManager;
    private final int minPlayers;
    private final int maxPlayers;

    public LobbyHandler(
            ObjectMapper objectMapper,
            GameStateManager gameStateManager,
            @Value("${escapehub.lobby.min-players:3}") int minPlayers,
            @Value("${escapehub.lobby.max-players:6}") int maxPlayers) {
        this.objectMapper = objectMapper;
        this.gameStateManager = gameStateManager;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.debug("Session connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        LobbyMessageDto incomingMsg = objectMapper.readValue(message.getPayload(), LobbyMessageDto.class);

        if (incomingMsg.type() == null) {
            sendMessageToSession(session, buildStateDto(MessageType.ERROR, null));
            return;
        }

        switch (incomingMsg.type()) {
            case JOIN_LOBBY -> handleJoinLobby(session, incomingMsg);
            case LEAVE_LOBBY -> handleLeaveLobby(session);
            case START_HEIST -> handleStartHeist(session);
            case LEAVE_GAME -> handleLeaveGame(session, incomingMsg);
            case CHAT_MESSAGE -> handleChatMessage(session, message.getPayload());
            default -> sendMessageToSession(session, buildStateDto(MessageType.ERROR, null));
        }
    }

    private void handleJoinLobby(WebSocketSession session, LobbyMessageDto msg) throws IOException {
        if (msg.username() == null || msg.username().isBlank()) {
            sendMessageToSession(session, buildStateDto(MessageType.ERROR, null));
            return;
        }

        if (!players.containsKey(session) && players.size() >= maxPlayers) {
            sendMessageToSession(session, buildStateDto(MessageType.ERROR, null));
            return;
        }

        players.put(session, msg.username().trim());
        broadcastMessage(buildStateDto(MessageType.LOBBY_UPDATE, null));
    }

    private void handleLeaveLobby(WebSocketSession session) throws IOException {
        if (players.remove(session) != null) {
            broadcastMessage(buildStateDto(MessageType.LOBBY_UPDATE, null));
        }
    }

    private void handleStartHeist(WebSocketSession requesterSession) throws IOException {
        boolean isInLobby = players.containsKey(requesterSession);
        boolean validPlayerCount = players.size() >= minPlayers && players.size() <= maxPlayers;

        if (isInLobby && validPlayerCount) {
            gameStateManager.startGame(players.values());
            broadcastMessage(buildStateDto(MessageType.GAME_STARTING, null));
        } else {
            sendMessageToSession(requesterSession, buildStateDto(MessageType.ERROR, null));
        }
    }

    private void handleLeaveGame(WebSocketSession session, LobbyMessageDto msg) throws IOException {
        String leavingPlayer = players.remove(session);
        if ((leavingPlayer == null || leavingPlayer.isBlank()) && msg.username() != null) {
            leavingPlayer = msg.username().trim();
        }

        handlePlayerExitInGame(leavingPlayer);
    }

    private void handlePlayerExitInGame(String playerName) throws IOException {
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        GameStateManager.LeaveResult result = gameStateManager.removePlayer(playerName);
        if (result.outcome() == GameStateManager.PlayerLeaveOutcome.GAME_STOPPED) {
            broadcastMessage(buildStateDto(MessageType.GAME_ABORTED, result.message()));
        } else if (result.outcome() == GameStateManager.PlayerLeaveOutcome.CONTINUE) {
            broadcastMessage(buildStateDto(MessageType.LOBBY_UPDATE, null));
        }
    }

    private void handleChatMessage(WebSocketSession session, String rawPayload) throws IOException {
        if (!players.containsKey(session)) {
            sendMessageToSession(session, buildStateDto(MessageType.ERROR, null));
            return;
        }

        ChatMessageDto incomingChat = objectMapper.readValue(rawPayload, ChatMessageDto.class);
        if (incomingChat.message() == null || incomingChat.message().isBlank()) {
            sendMessageToSession(session, buildStateDto(MessageType.ERROR, null));
            return;
        }

        String sender = (incomingChat.sender() != null && !incomingChat.sender().isBlank())
                ? incomingChat.sender().trim()
                : players.get(session);

        String color = (incomingChat.color() != null && !incomingChat.color().isBlank())
                ? incomingChat.color().trim()
                : DEFAULT_CHAT_COLOR;

        ChatMessageDto outgoingChat = new ChatMessageDto(
                MessageType.CHAT_MESSAGE,
                sender,
                color,
                incomingChat.message().trim(),
                System.currentTimeMillis()
        );

        broadcastChatMessage(outgoingChat);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String removedPlayer = players.remove(session);
        if (removedPlayer != null) {
            GameStateManager.LeaveResult result = gameStateManager.removePlayer(removedPlayer);
            if (result.outcome() == GameStateManager.PlayerLeaveOutcome.GAME_STOPPED) {
                broadcastMessage(buildStateDto(MessageType.GAME_ABORTED, result.message()));
            } else {
                broadcastMessage(buildStateDto(MessageType.LOBBY_UPDATE, null));
            }
        }
    }

    private LobbyStateResponseDto buildStateDto(MessageType type, String reason) {
        return new LobbyStateResponseDto(
                type,
                players.size(),
                minPlayers,
                maxPlayers,
                players.values(),
                reason
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

    private void broadcastChatMessage(ChatMessageDto chatDto) throws IOException {
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(chatDto));
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

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}