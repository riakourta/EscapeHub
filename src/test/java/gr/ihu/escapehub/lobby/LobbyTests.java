package gr.ihu.escapehub.lobby;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.lobby.dto.LobbyMessageDto;
import gr.ihu.escapehub.lobby.dto.LobbyStateResponseDto;
import gr.ihu.escapehub.lobby.dto.MessageType;
import gr.ihu.escapehub.lobby.handler.LobbyHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LobbyTests {

    private LobbyHandler lobbyHandler;
    private ObjectMapper objectMapper;
    private static final int REQUIRED_PLAYERS = 4;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lobbyHandler = new LobbyHandler(objectMapper, REQUIRED_PLAYERS);
    }

    private WebSocketSession createMockSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private void joinPlayer(WebSocketSession session, String username) throws Exception {
        LobbyMessageDto joinDto = new LobbyMessageDto(MessageType.JOIN_LOBBY, username, "valid-token");
        TextMessage message = new TextMessage(objectMapper.writeValueAsString(joinDto));
        lobbyHandler.afterConnectionEstablished(session);
        lobbyHandler.handleMessage(session, message);
    }

    @Test
    void shouldRejectStartHeistWhenLessThanFourPlayers() throws Exception {
        // Συνδέονται μόνο 3 παίκτες
        WebSocketSession p1 = createMockSession("1");
        WebSocketSession p2 = createMockSession("2");
        WebSocketSession p3 = createMockSession("3");

        joinPlayer(p1, "Bandit1");
        joinPlayer(p2, "Bandit2");
        joinPlayer(p3, "Bandit3");

        assertEquals(3, lobbyHandler.getConnectedPlayersCount());
        clearInvocations(p1, p2, p3);

        // Ο παίκτης 1 πατάει START_HEIST
        LobbyMessageDto startDto = new LobbyMessageDto(MessageType.START_HEIST, null, null);
        lobbyHandler.handleMessage(p1, new TextMessage(objectMapper.writeValueAsString(startDto)));

        // Επιβεβαίωση ότι ο παίκτης 1 έλαβε μήνυμα ERROR και το παιχνίδι ΔΕΝ ξεκίνησε
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(p1, times(1)).sendMessage(captor.capture());

        LobbyStateResponseDto response = objectMapper.readValue(
                captor.getValue().getPayload(),
                LobbyStateResponseDto.class
        );

        assertEquals(MessageType.ERROR, response.type());
        assertEquals(3, response.count());
        assertEquals(REQUIRED_PLAYERS, response.required());
    }

    @Test
    void shouldStartHeistWhenFourPlayersConnected() throws Exception {
        // Συνδέονται 4 παίκτες
        WebSocketSession p1 = createMockSession("1");
        WebSocketSession p2 = createMockSession("2");
        WebSocketSession p3 = createMockSession("3");
        WebSocketSession p4 = createMockSession("4");

        joinPlayer(p1, "Bandit1");
        joinPlayer(p2, "Bandit2");
        joinPlayer(p3, "Bandit3");
        joinPlayer(p4, "Bandit4");

        assertEquals(4, lobbyHandler.getConnectedPlayersCount());
        clearInvocations(p1, p2, p3, p4);

        // Ο παίκτης 1 πατάει START_HEIST
        LobbyMessageDto startDto = new LobbyMessageDto(MessageType.START_HEIST, null, null);
        lobbyHandler.handleMessage(p1, new TextMessage(objectMapper.writeValueAsString(startDto)));

        // Επιβεβαίωση ότι έγινε broadcast το GAME_STARTING και στους 4 παίκτες
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(p1, times(1)).sendMessage(captor.capture());
        verify(p2, times(1)).sendMessage(any(TextMessage.class));
        verify(p3, times(1)).sendMessage(any(TextMessage.class));
        verify(p4, times(1)).sendMessage(any(TextMessage.class));

        LobbyStateResponseDto response = objectMapper.readValue(
                captor.getValue().getPayload(),
                LobbyStateResponseDto.class
        );

        assertEquals(MessageType.GAME_STARTING, response.type());
        assertEquals(4, response.count());
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