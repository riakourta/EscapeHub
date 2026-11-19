package gr.ihu.escapehub.lobby;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.lobby.handler.LobbyHandler;
import gr.ihu.escapehub.state.service.GameStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LobbyTests {

    private ObjectMapper objectMapper;
    private GameStateManager gameStateManager;
    private LobbyHandler lobbyHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        gameStateManager = mock(GameStateManager.class);
        lobbyHandler = new LobbyHandler(objectMapper, gameStateManager, 3, 6);
    }

    private WebSocketSession createMockSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);

        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);

        return session;
    }

    private JsonNode getLastSentJson(WebSocketSession session) throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        List<TextMessage> allMessages = captor.getAllValues();
        return objectMapper.readTree(allMessages.get(allMessages.size() - 1).getPayload());
    }

    private void startHeist(WebSocketSession session) throws Exception {
        LobbyMessageDto startDto = new LobbyMessageDto(
                MessageType.START_HEIST,
                null,
                null
        );

        lobbyHandler.handleMessage(
                session,
                new TextMessage(
                        objectMapper.writeValueAsString(startDto)
                )
        );
    }

    @Test
    void testJoinLobbyUpdatesCountAndMaxPlayers() throws Exception {
        WebSocketSession s1 = createMockSession("s1");

        lobbyHandler.handleMessage(s1, new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"username\":\"#ef4444|Eleftheria\"}"
        ));

        assertEquals(1, lobbyHandler.getConnectedPlayersCount());
        JsonNode response = getLastSentJson(s1);
        assertEquals("LOBBY_UPDATE", response.get("type").asText());
        assertEquals(1, response.get("count").asInt());
        assertEquals(3, response.get("required").asInt());
        assertEquals(6, response.get("maxPlayers").asInt());
    }

    @Test
    void testStartHeistFailsWithLessThanThreePlayers() throws Exception {
        WebSocketSession s1 = createMockSession("s1");
        WebSocketSession s2 = createMockSession("s2");

        lobbyHandler.handleMessage(s1, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#ef4444|P1\"}"));
        lobbyHandler.handleMessage(s2, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#38bdf8|P2\"}"));

        lobbyHandler.handleMessage(s1, new TextMessage("{\"type\":\"START_HEIST\"}"));

        JsonNode response = getLastSentJson(s1);
        assertEquals("ERROR", response.get("type").asText());
        verify(gameStateManager, never()).startGame(any());
    }

    @Test
    void testStartHeistSucceedsWithThreePlayers() throws Exception {
        WebSocketSession s1 = createMockSession("s1");
        WebSocketSession s2 = createMockSession("s2");
        WebSocketSession s3 = createMockSession("s3");

        lobbyHandler.handleMessage(s1, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#ef4444|P1\"}"));
        lobbyHandler.handleMessage(s2, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#38bdf8|P2\"}"));
        lobbyHandler.handleMessage(s3, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#34d399|P3\"}"));

        lobbyHandler.handleMessage(s1, new TextMessage("{\"type\":\"START_HEIST\"}"));

        JsonNode response = getLastSentJson(s1);
        assertEquals("GAME_STARTING", response.get("type").asText());
        verify(gameStateManager, times(1)).startGame(any());
    }

    @Test
    void testLobbyRejectsSeventhPlayer() throws Exception {
        for (int i = 1; i <= 6; i++) {
            WebSocketSession s = createMockSession("s" + i);
            lobbyHandler.handleMessage(s, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#fff|P" + i + "\"}"));
        }

        WebSocketSession s7 = createMockSession("s7");
        lobbyHandler.handleMessage(s7, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#fff|P7\"}"));

        assertEquals(6, lobbyHandler.getConnectedPlayersCount());
        JsonNode response = getLastSentJson(s7);
        assertEquals("ERROR", response.get("type").asText());
    }

    @Test
    void testLeaveLobbyRemovesPlayerAndBroadcastsUpdate() throws Exception {
        WebSocketSession s1 = createMockSession("s1");
        WebSocketSession s2 = createMockSession("s2");

        lobbyHandler.handleMessage(s1, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#ef4444|P1\"}"));
        lobbyHandler.handleMessage(s2, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#38bdf8|P2\"}"));

        lobbyHandler.handleMessage(s1, new TextMessage("{\"type\":\"LEAVE_LOBBY\"}"));

        assertEquals(1, lobbyHandler.getConnectedPlayersCount());
        JsonNode response = getLastSentJson(s2);
        assertEquals("LOBBY_UPDATE", response.get("type").asText());
        assertEquals(1, response.get("count").asInt());
    }

    @Test
    void testChatMessageBroadcastsToPlayers() throws Exception {
        WebSocketSession s1 = createMockSession("s1");
        WebSocketSession s2 = createMockSession("s2");

        lobbyHandler.handleMessage(s1, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#ef4444|Eleftheria\"}"));
        lobbyHandler.handleMessage(s2, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#38bdf8|Christos\"}"));

        lobbyHandler.handleMessage(s1, new TextMessage(
                "{\"type\":\"CHAT_MESSAGE\",\"sender\":\"Eleftheria\",\"color\":\"#ef4444\",\"message\":\"Βρήκα το κλειδί!\"}"
        ));

        JsonNode chatMsg = getLastSentJson(s2);
        assertEquals("CHAT_MESSAGE", chatMsg.get("type").asText());
        assertEquals("Eleftheria", chatMsg.get("sender").asText());
        assertEquals("#ef4444", chatMsg.get("color").asText());
        assertEquals("Βρήκα το κλειδί!", chatMsg.get("message").asText());
    }

    @Test
    void testLeaveGameAbortsWhenTooFewPlayersRemain() throws Exception {
        WebSocketSession s1 = createMockSession("s1");
        WebSocketSession s2 = createMockSession("s2");
        WebSocketSession s3 = createMockSession("s3");

        lobbyHandler.handleMessage(s1, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#ef4444|P1\"}"));
        lobbyHandler.handleMessage(s2, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#38bdf8|P2\"}"));
        lobbyHandler.handleMessage(s3, new TextMessage("{\"type\":\"JOIN_LOBBY\",\"username\":\"#34d399|P3\"}"));

        GameStateManager.LeaveResult mockResult = mock(GameStateManager.LeaveResult.class);
        when(mockResult.outcome()).thenReturn(GameStateManager.PlayerLeaveOutcome.GAME_STOPPED);
        when(mockResult.message()).thenReturn("Έμειναν λιγότεροι από 3 παίκτες.");
        when(gameStateManager.removePlayer("#ef4444|P1")).thenReturn(mockResult);

        lobbyHandler.handleMessage(s1, new TextMessage("{\"type\":\"LEAVE_GAME\",\"username\":\"#ef4444|P1\"}"));

        JsonNode abortMsg = getLastSentJson(s2);
        assertEquals("GAME_ABORTED", abortMsg.get("type").asText());
        assertEquals("Έμειναν λιγότεροι από 3 παίκτες.", abortMsg.get("reason").asText());
    }
}