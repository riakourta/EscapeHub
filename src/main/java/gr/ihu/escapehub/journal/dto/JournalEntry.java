package gr.ihu.escapehub.journal.dto;

public record JournalEntry(
        long sequence,
        String type,
        String targetId,
        String message,
        String author
) {
    public JournalEntry(long sequence, String type, String targetId, String message) {
        this(sequence, type, targetId, message, null);
    }
}