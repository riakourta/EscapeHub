package gr.ihu.escapehub.puzzle.dto;

import java.util.List;

public record RoomPuzzlesConfigDto(
        String roomId,
        String nextRoom,
        List<PuzzleConfigDto> puzzles
) {}
