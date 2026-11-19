package gr.ihu.escapehub.movement;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.movement.handler.MovementHandler;
import gr.ihu.escapehub.movement.service.MovementStateService;
import gr.ihu.escapehub.movement.service.MovementTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovementHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final MovementStateService state = new MovementStateService();
    private final MovementTokenService tokens =
            mock(MovementTokenService.class);

    private final MovementHandler handler =
            new MovementHandler(mapper, state, tokens);

    private WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);

        when(session.getId()).thenReturn("session-" + id);
        when(session.isOpen()).thenReturn(true);

        return session;
    }

    private WebSocketSession join(long id) throws Exception {
        User user = new User("user" + id, "unused-hash");
        user.setId(id);

        when(tokens.authenticate("token" + id)).thenReturn(user);

        WebSocketSession session = session(String.valueOf(id));

        handler.handleMessage(session, new TextMessage(
                "{\"type\":\"join\",\"token\":\"token" + id
                        + "\",\"nickname\":\"Player\",\"color\":\"blue\"}"
        ));

        return session;
    }

    @Test
    void broadcastsMovementUsingAuthenticatedIdentity() throws Exception {
        WebSocketSession first = join(1);
        WebSocketSession second = join(2);

        clearInvocations(first, second);

        handler.handleMessage(first, new TextMessage(
                """
                {
                  "type": "move",
                  "id": "fake-id",
                  "x": 120,
                  "y": 500,
                  "direction": "right"
                }
                """
        ));

        ArgumentCaptor<TextMessage> messages =
                ArgumentCaptor.forClass(TextMessage.class);

        verify(second).sendMessage(messages.capture());
        verify(first, never()).sendMessage(any());

        var response = mapper.readTree(
                messages.getValue().getPayload()
        );

        assertEquals("move", response.path("type").asText());
        assertEquals("1", response.path("player").path("id").asText());
        assertEquals(120.0, response.path("player").path("x").asDouble());
    }

    @Test
    void rejectsMovementBeforeJoin() throws Exception {
        WebSocketSession anonymous = session("anonymous");

        handler.handleMessage(anonymous, new TextMessage(
                """
                {
                  "type": "move",
                  "x": 120,
                  "y": 500,
                  "direction": "right"
                }
                """
        ));

        ArgumentCaptor<TextMessage> messages =
                ArgumentCaptor.forClass(TextMessage.class);

        verify(anonymous).sendMessage(messages.capture());

        var response = mapper.readTree(
                messages.getValue().getPayload()
        );

        assertEquals(
                "AUTH_REQUIRED",
                response.path("code").asText()
        );

        assertTrue(state.getPlayers().isEmpty());
    }

    @Test
    void removesDisconnectedPlayerAndUpdatesOthers() throws Exception {
        WebSocketSession first = join(1);
        WebSocketSession second = join(2);

        clearInvocations(second);

        handler.afterConnectionClosed(first, CloseStatus.NORMAL);

        assertTrue(state.findPlayer("1").isEmpty());
        assertEquals(1, state.getPlayers().size());

        ArgumentCaptor<TextMessage> messages =
                ArgumentCaptor.forClass(TextMessage.class);

        verify(second).sendMessage(messages.capture());

        var response = mapper.readTree(
                messages.getValue().getPayload()
        );

        assertEquals("snapshot", response.path("type").asText());
        assertEquals(1, response.path("players").size());
        assertEquals("2", response.path("players").get(0).path("id").asText());
    }
}