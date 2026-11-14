package gr.ihu.escapehub;

import gr.ihu.escapehub.movement.handler.MovementHandler;
import gr.ihu.escapehub.state.service.MuseumRoom;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MovementHandlerTests {
    private WebSocketSession player(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn((Principal) () -> id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
    private MuseumRoom room() {
        MuseumRoom room = new MuseumRoom();
        room.start(List.of(new MuseumRoom.Member("p1", "One", "blue"),
                new MuseumRoom.Member("p2", "Two", "green"),
                new MuseumRoom.Member("p3", "Three", "pink"),
                new MuseumRoom.Member("p4", "Four", "purple")));
        return room;
    }
    @Test
    void fourPlayersReceiveMovementWithServerIdentityAndRejectWallsAndExhibits() throws Exception {
        MovementHandler handler = new MovementHandler(room());
        WebSocketSession[] players = {player("p1"), player("p2"), player("p3"), player("p4")};
        for (WebSocketSession p : players) { handler.afterConnectionEstablished(p); }
        for (WebSocketSession p : players) { clearInvocations(p); }
        handler.handleMessage(players[0], new TextMessage("{\"type\":\"move\",\"id\":\"spoof\",\"x\":120,\"y\":140}"));
        verify(players[0], never()).sendMessage(any());
        for (int i = 1; i < 4; i++) {
            verify(players[i]).sendMessage(argThat(m -> m.getPayload().toString().contains("p1")
                    && !m.getPayload().toString().contains("spoof")));
        }
        for (WebSocketSession p : players) { clearInvocations(p); }
        handler.handleMessage(players[0], new TextMessage("{\"type\":\"move\",\"x\":-1,\"y\":140}"));
        handler.handleMessage(players[0], new TextMessage("{\"type\":\"move\",\"x\":360,\"y\":370}"));
        verify(players[1], never()).sendMessage(any());
        handler.afterConnectionClosed(players[0], CloseStatus.NORMAL);
        verify(players[1]).sendMessage(argThat(m -> !m.getPayload().toString().contains("p1")));
    }
    @Test
    void rejectsAnonymousDuplicateAndUnadmittedUsers() throws Exception {
        MovementHandler handler = new MovementHandler(room());
        WebSocketSession anonymous = mock(WebSocketSession.class);
        handler.afterConnectionEstablished(anonymous);
        verify(anonymous).close(any(CloseStatus.class));
        handler.afterConnectionEstablished(player("p1"));
        WebSocketSession fifth = player("p5");
        handler.afterConnectionEstablished(fifth);
        verify(fifth).close(any(CloseStatus.class));
        WebSocketSession duplicate = player("p1");
        handler.afterConnectionEstablished(duplicate);
        verify(duplicate).close(any(CloseStatus.class));
    }
    @Test
    void alarmIsSharedAndRoomResetsAfterEveryoneLeaves() throws Exception {
        MuseumRoom room = room();
        MovementHandler handler = new MovementHandler(room);
        WebSocketSession p1 = player("p1"), p2 = player("p2");
        handler.afterConnectionEstablished(p1);
        handler.afterConnectionEstablished(p2);
        clearInvocations(p1,p2);
        handler.handleMessage(p1,new TextMessage("{\"type\":\"move\",\"x\":360,\"y\":430}"));
        assertTrue(room.alarm());
        verify(p2).sendMessage(argThat(m -> m.getPayload().toString().contains("\"alarm\":true")));
        handler.afterConnectionClosed(p1,CloseStatus.NORMAL);
        assertTrue(room.started());
        handler.afterConnectionClosed(p2,CloseStatus.NORMAL);
        assertFalse(room.started());
        assertFalse(room.alarm());
    }
}
