package gr.ihu.escapehub.exhibit.dto;

import java.util.List;

public record ExhibitInfoDto(
        String id,
        String label,
        String name,
        String category,
        String artist,
        String date,
        String imageUrl,
        String description,
        String securityNote,
        double x,
        double y,
        double width,
        double height,
        List<ExhibitClueDto> clues
) {
}