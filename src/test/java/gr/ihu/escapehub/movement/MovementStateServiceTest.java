package gr.ihu.escapehub.movement;

import gr.ihu.escapehub.movement.dto.PlayerState;
import gr.ihu.escapehub.movement.service.MovementStateService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovementStateServiceTest {

    private PlayerState player(String id) {
        return new PlayerState(
                id, "Player " + id, "blue",
                100, 500, "up"
        );
    }

    @Test
    void movementPreservesIdentityAndColor() {
        MovementStateService service = new MovementStateService();
        service.addPlayer(player("1"));

        PlayerState updated = service.updatePosition(
                "1", 150, 450, "right"
        );

        assertEquals("1", updated.id());
        assertEquals("Player 1", updated.nickname());
        assertEquals("blue", updated.color());
        assertEquals(150.0, updated.x());
        assertEquals(450.0, updated.y());
        assertEquals("right", updated.direction());

        assertEquals(
                updated,
                service.findPlayer("1").orElseThrow()
        );
    }

    @Test
    void acceptsSixPlayersButRejectsSeventh() {
        MovementStateService service = new MovementStateService();

        for (int i = 1; i <= 6; i++) {
            service.addPlayer(player(String.valueOf(i)));
        }

        assertEquals(6, service.getPlayers().size());

        assertThrows(
                IllegalStateException.class,
                () -> service.addPlayer(player("7"))
        );

        assertEquals(6, service.getPlayers().size());
    }

    @Test
    void rejectsDuplicatePlayerWithoutReplacingExistingState() {
        MovementStateService service = new MovementStateService();
        service.addPlayer(player("1"));
        service.updatePosition("1", 200, 400, "left");

        assertThrows(
                IllegalStateException.class,
                () -> service.addPlayer(player("1"))
        );

        assertEquals(1, service.getPlayers().size());
        assertEquals(
                200.0,
                service.findPlayer("1").orElseThrow().x()
        );
    }

    @Test
    void disconnectedPlayerCannotMoveAndFreesRoomSlot() {
        MovementStateService service = new MovementStateService();

        for (int i = 1; i <= 6; i++) {
            service.addPlayer(player(String.valueOf(i)));
        }

        service.removePlayer("1");

        assertTrue(service.findPlayer("1").isEmpty());

        assertThrows(
                IllegalStateException.class,
                () -> service.updatePosition("1", 150, 450, "up")
        );

        service.addPlayer(player("7"));
        assertEquals(6, service.getPlayers().size());
    }
}