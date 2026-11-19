package gr.ihu.escapehub.puzzle.dto;

import java.util.List;

public record PuzzleState(
        String id,
        String label,
        List<PlateState> plates,
        boolean solved
) {
    public PuzzleState {
        plates = List.copyOf(plates);
    }
}