package gr.ihu.escapehub;

import gr.ihu.escapehub.movement.handler.MovementHandler;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import static org.mockito.Mockito.*;

class MovementHandlerTests {
    private WebSocketSession player(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn((Principal) () -> id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void fourPlayersReceiveMovementButSenderDoesNotAndIdentityCannotBeSpoofed() throws Exception {
        MovementHandler handler = new MovementHandler();
        WebSocketSession[] players = {player("p1"), player("p2"), player("p3"), player("p4")};
        for (WebSocketSession p : players) { handler.afterConnectionEstablished(p); }
        for (WebSocketSession p : players) { handler.handleMessage(p, new TextMessage("{\"type\":\"ready\"}")); }
        for (WebSocketSession p : players) { clearInvocations(p); }
        handler.handleMessage(players[0], new TextMessage("{\"type\":\"move\",\"id\":\"spoof\",\"x\":120,\"y\":140}"));
        verify(players[0], never()).sendMessage(any());
        for (int i = 1; i < 4; i++) {
            verify(players[i]).sendMessage(argThat(m -> m.getPayload().toString().contains("p1")
                    && !m.getPayload().toString().contains("spoof")));
        }
        for (WebSocketSession p : players) { clearInvocations(p); }
        handler.handleMessage(players[0], new TextMessage("{\"type\":\"move\",\"x\":-1,\"y\":140}"));
        verify(players[1], never()).sendMessage(any());
        handler.afterConnectionClosed(players[0], CloseStatus.NORMAL);
        verify(players[1]).sendMessage(argThat(m -> !m.getPayload().toString().contains("p1")));
    }

    @Test
    void rejectsAnonymousDuplicateAndFifthPlayer() throws Exception {
        MovementHandler handler = new MovementHandler();
        WebSocketSession anonymous = mock(WebSocketSession.class);
        handler.afterConnectionEstablished(anonymous);
        verify(anonymous).close(CloseStatus.POLICY_VIOLATION);
        for (int i = 1; i <= 4; i++) { handler.afterConnectionEstablished(player("p" + i)); }
        WebSocketSession fifth = player("p5");
        handler.afterConnectionEstablished(fifth);
        verify(fifth).close(any(CloseStatus.class));
        WebSocketSession duplicate = player("p1");
        handler.afterConnectionEstablished(duplicate);
        verify(duplicate).close(any(CloseStatus.class));
    }
}
