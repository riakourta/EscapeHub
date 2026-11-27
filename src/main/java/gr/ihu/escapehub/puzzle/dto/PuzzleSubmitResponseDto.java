package gr.ihu.escapehub.puzzle.dto;

public record PuzzleSubmitResponseDto(
        String puzzleId,
        boolean correct,
        boolean solved,
        String message,
        boolean doorUnlocked,
        String nextRoom
) {}