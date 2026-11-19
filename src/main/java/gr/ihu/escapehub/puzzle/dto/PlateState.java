package gr.ihu.escapehub.puzzle.dto;

public record PlateState(
        String id,
        double x,
        double y,
        double width,
        double height,
        boolean active
) {
}