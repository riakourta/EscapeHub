package gr.ihu.escapehub.puzzle;

import gr.ihu.escapehub.movement.dto.PlayerState;
import gr.ihu.escapehub.puzzle.service.PuzzleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PuzzleServiceTest {

    private final PuzzleService service = new PuzzleService();

    @Test
    void twoDifferentPlayersSolvePuzzle() {
        var result = service.evaluate(List.of(
                player("1", 250, 455),
                player("2", 510, 455)
        ));

        assertTrue(result.solved());
        assertTrue(result.plates().get(0).active());
        assertTrue(result.plates().get(1).active());
    }

    @Test
    void onePlayerCannotSolvePuzzle() {
        var result = service.evaluate(List.of(
                player("1", 250, 455)
        ));

        assertFalse(result.solved());
        assertTrue(result.plates().get(0).active());
        assertFalse(result.plates().get(1).active());
    }

    @Test
    void sameIdentityCannotCountAsTwoPlayers() {
        var result = service.evaluate(List.of(
                player("1", 250, 455),
                player("1", 510, 455)
        ));

        assertFalse(result.solved());
    }

    @Test
    void visitingPlatesAtDifferentTimesDoesNotSolvePuzzle() {
        service.evaluate(List.of(
                player("1", 250, 455)
        ));

        var result = service.evaluate(List.of(
                player("1", 510, 455)
        ));

        assertFalse(result.solved());
        assertFalse(result.plates().get(0).active());
        assertTrue(result.plates().get(1).active());
    }

    @Test
    void solvedPuzzleStaysSolvedAfterPlayersLeave() {
        service.evaluate(List.of(
                player("1", 250, 455),
                player("2", 510, 455)
        ));

        var result = service.evaluate(List.of());

        assertTrue(result.solved());
        assertTrue(result.plates().stream()
                .noneMatch(plate -> plate.active()));
    }

    private PlayerState player(String id, double x, double y) {
        return new PlayerState(
                id, "Player " + id, "blue", x, y, "up"
        );
    }
}