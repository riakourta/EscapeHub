package gr.ihu.escapehub.journal.service;

import gr.ihu.escapehub.journal.dto.JournalEntry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JournalService {

    private final Map<String, JournalEntry> entries = new LinkedHashMap<>();
    private long nextSequence = 1;

    public synchronized JournalEntry recordExhibit(String exhibitId, String label) {
        return addOnce("EXHIBIT_COLLECTED", exhibitId, "Συλλέχθηκε: " + label, null);
    }

    public synchronized JournalEntry recordPuzzle(String puzzleId, String label) {
        return addOnce("PUZZLE_SOLVED", puzzleId, "Λύθηκε: " + label, null);
    }

    public synchronized JournalEntry recordClue(String value, String author) {
        String normalized = value == null ? "" : value.trim();
        return addOnce("CLUE_ADDED", normalized, "Κλειδί: " + normalized + " (από " + author + ")", author);
    }

    public synchronized List<JournalEntry> recordClue(
            String clueId,
            String title,
            String description,
            String sourceExhibit
    ) {
        StringBuilder message = new StringBuilder("Στοιχείο");
        if (sourceExhibit != null && !sourceExhibit.isBlank()) {
            message.append(" [").append(sourceExhibit.trim()).append("]");
        }
        message.append(": ").append(title != null ? title.trim() : "");
        if (description != null && !description.isBlank()) {
            message.append(" — ").append(description.trim());
        }

        addOnce("CLUE_SHARED", clueId, message.toString());
        return getEntries();
    }

    public synchronized List<JournalEntry> getEntries() {
        return List.copyOf(entries.values());
    }

    private JournalEntry addOnce(String type, String targetId, String message, String author) {
        String key = type + ":" + targetId;
        JournalEntry existing = entries.get(key);
        if (existing != null) {
            return existing;
        }
        JournalEntry entry = new JournalEntry(nextSequence++, type, targetId, message, author);
        entries.put(key, entry);
        return entry;
    }
}