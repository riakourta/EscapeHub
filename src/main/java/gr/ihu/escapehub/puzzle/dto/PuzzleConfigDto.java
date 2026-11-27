package gr.ihu.escapehub.puzzle.dto;

import java.util.List;

public record PuzzleConfigDto(
        String id,
        String title,
        String description,
        String type,
        Boolean requiresDifferentPlayers,
        Double playerSize,
        List<PlateConfigDto> plates,
        List<String> acceptedSolutions,
        boolean isFinal
) {
    public record PlateConfigDto(String id, double x, double y, double width, double height) {}
}