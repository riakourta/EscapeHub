package gr.ihu.escapehub.exhibit.dto;

public record ExhibitState(
        String id,
        String label,
        double x,
        double y,
        double width,
        double height,
        boolean collected,
        String collectedBy
) {
}