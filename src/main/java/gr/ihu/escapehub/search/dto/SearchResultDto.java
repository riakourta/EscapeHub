package gr.ihu.escapehub.search.dto;

public record SearchResultDto(
        String title,
        String summary,
        String imageUrl,
        String source,
        String artist,
        String date
) {
}