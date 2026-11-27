package gr.ihu.escapehub.puzzle.dto;

public record PuzzleSummaryDto(
        String id,
        String title,
        String description,
        String type,
        boolean solved,
        boolean isFinal
) {}