package gr.ihu.escapehub.movement.dto;

public record PlayerState(
        String id,
        String nickname,
        String color,
        double x,
        double y,
        String direction
) {
}