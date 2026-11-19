package gr.ihu.escapehub.lobby;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.lobby.dto.LobbyMessageDto;
import gr.ihu.escapehub.lobby.dto.LobbyStateResponseDto;
import gr.ihu.escapehub.lobby.dto.MessageType;
import gr.ihu.escapehub.lobby.handler.LobbyHandler;
import gr.ihu.escapehub.state.service.GameStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class LobbyTests {

    private static final int REQUIRED_PLAYERS = 3;
    private static final int MAX_PLAYERS = 6;

    private LobbyHandler lobbyHandler;
    private ObjectMapper objectMapper;
    private GameStateManager gameStateManager;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        gameStateManager = new GameStateManager(
                3600,
                REQUIRED_PLAYERS
        );

        lobbyHandler = new LobbyHandler(
                objectMapper,
                gameStateManager,
                REQUIRED_PLAYERS,
                MAX_PLAYERS
        );
    }

    private WebSocketSession createMockSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);

        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);

        return session;
    }

    private void joinPlayer(
            WebSocketSession session,
            String username
    ) throws Exception {
        LobbyMessageDto joinDto = new LobbyMessageDto(
                MessageType.JOIN_LOBBY,
                username,
                "valid-token"
        );

        TextMessage message = new TextMessage(
                objectMapper.writeValueAsString(joinDto)
        );

        lobbyHandler.afterConnectionEstablished(session);
        lobbyHandler.handleMessage(session, message);
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
    void shouldRejectStartHeistWhenLessThanThreePlayers() throws Exception {
        WebSocketSession p1 = createMockSession("1");
        WebSocketSession p2 = createMockSession("2");

        joinPlayer(p1, "Bandit1");
        joinPlayer(p2, "Bandit2");

        assertEquals(2, lobbyHandler.getConnectedPlayersCount());

        clearInvocations(p1, p2);

        startHeist(p1);

        ArgumentCaptor<TextMessage> captor =
                ArgumentCaptor.forClass(TextMessage.class);

        verify(p1).sendMessage(captor.capture());

        LobbyStateResponseDto response = objectMapper.readValue(
                captor.getValue().getPayload(),
                LobbyStateResponseDto.class
        );

        assertEquals(MessageType.ERROR, response.type());
        assertEquals(2, response.count());
        assertEquals(REQUIRED_PLAYERS, response.required());
        assertFalse(gameStateManager.isGameActive());
    }

    @Test
    void shouldStartHeistWhenThreePlayersConnected() throws Exception {
        WebSocketSession p1 = createMockSession("1");
        WebSocketSession p2 = createMockSession("2");
        WebSocketSession p3 = createMockSession("3");

        joinPlayer(p1, "Bandit1");
        joinPlayer(p2, "Bandit2");
        joinPlayer(p3, "Bandit3");

        assertEquals(3, lobbyHandler.getConnectedPlayersCount());

        clearInvocations(p1, p2, p3);

        startHeist(p1);

        ArgumentCaptor<TextMessage> captor =
                ArgumentCaptor.forClass(TextMessage.class);

        verify(p1).sendMessage(captor.capture());
        verify(p2).sendMessage(any(TextMessage.class));
        verify(p3).sendMessage(any(TextMessage.class));

        LobbyStateResponseDto response = objectMapper.readValue(
                captor.getValue().getPayload(),
                LobbyStateResponseDto.class
        );

        assertEquals(MessageType.GAME_STARTING, response.type());
        assertEquals(3, response.count());
        assertTrue(gameStateManager.isGameActive());
        assertEquals(3, gameStateManager.getActivePlayersCount());
    }

    @Test
    void shouldRemovePlayerOnDisconnect() throws Exception {
        WebSocketSession p1 = createMockSession("1");

        joinPlayer(p1, "Bandit1");

        assertEquals(1, lobbyHandler.getConnectedPlayersCount());

        lobbyHandler.afterConnectionClosed(p1, CloseStatus.NORMAL);

        assertEquals(0, lobbyHandler.getConnectedPlayersCount());
    }
}