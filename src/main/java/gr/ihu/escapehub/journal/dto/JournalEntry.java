package gr.ihu.escapehub.journal.dto;

public record JournalEntry(
        long sequence,
        String type,
        String targetId,
        String message
) {
}