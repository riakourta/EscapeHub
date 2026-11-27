package gr.ihu.escapehub.exhibit.dto;

public record ExhibitClueDto(
        String clueId,
        String exhibitId,
        String exhibitName,
        String title,
        String text
) {
}