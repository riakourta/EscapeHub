package gr.ihu.escapehub.puzzle.service;

import gr.ihu.escapehub.movement.dto.PlayerState;
import gr.ihu.escapehub.puzzle.dto.PlateState;
import gr.ihu.escapehub.puzzle.dto.PuzzleState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PuzzleService {

    private static final double PLAYER_SIZE = 30;

    private static final PlateState FIRST_PLATE =
            new PlateState(
                    "plate-1",
                    250, 455,
                    38, 38,
                    false
            );

    private static final PlateState SECOND_PLATE =
            new PlateState(
                    "plate-2",
                    510, 455,
                    38, 38,
                    false
            );

    private PuzzleState current = new PuzzleState(
            "twin-security-plates",
            "Διπλός Μηχανισμός Ασφαλείας (2 Παίκτες)",
            List.of(FIRST_PLATE, SECOND_PLATE),
            false
    );

    public synchronized PuzzleState getState() {
        return current;
    }

    public synchronized PuzzleState evaluate(
            List<PlayerState> players
    ) {
        boolean firstActive = players.stream()
                .anyMatch(player -> overlaps(player, FIRST_PLATE));

        boolean secondActive = players.stream()
                .anyMatch(player -> overlaps(player, SECOND_PLATE));

        boolean twoDifferentPlayers = players.stream()
                .filter(first -> overlaps(first, FIRST_PLATE))
                .anyMatch(first -> players.stream()
                        .anyMatch(second ->
                                !first.id().equals(second.id())
                                        && overlaps(second, SECOND_PLATE)
                        )
                );

        // Μόλις λυθεί, ο γρίφος παραμένει λυμένος.
        boolean solved = current.solved() || twoDifferentPlayers;

        current = new PuzzleState(
                current.id(),
                current.label(),
                List.of(
                        withActive(FIRST_PLATE, firstActive),
                        withActive(SECOND_PLATE, secondActive)
                ),
                solved
        );

        return current;
    }

    private PlateState withActive(
            PlateState plate,
            boolean active
    ) {
        return new PlateState(
                plate.id(),
                plate.x(),
                plate.y(),
                plate.width(),
                plate.height(),
                active
        );
    }

    private boolean overlaps(
            PlayerState player,
            PlateState plate
    ) {
        return player.x() < plate.x() + plate.width()
                && player.x() + PLAYER_SIZE > plate.x()
                && player.y() < plate.y() + plate.height()
                && player.y() + PLAYER_SIZE > plate.y();
    }
}