package gr.ihu.escapehub.journal.service;

import gr.ihu.escapehub.journal.dto.JournalEntry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JournalService {

    private final Map<String, JournalEntry> entries =
            new LinkedHashMap<>();

    private long nextSequence = 1;

    public synchronized JournalEntry recordExhibit(
            String exhibitId,
            String label
    ) {
        return addOnce(
                "EXHIBIT_COLLECTED",
                exhibitId,
                "Συλλέχθηκε: " + label
        );
    }

    public synchronized JournalEntry recordPuzzle(
            String puzzleId,
            String label
    ) {
        return addOnce(
                "PUZZLE_SOLVED",
                puzzleId,
                "Λύθηκε: " + label
        );
    }

    public synchronized List<JournalEntry> getEntries() {
        return List.copyOf(entries.values());
    }

    private JournalEntry addOnce(
            String type,
            String targetId,
            String message
    ) {
        String key = type + ":" + targetId;

        JournalEntry existing = entries.get(key);

        if (existing != null) {
            return existing;
        }

        JournalEntry entry = new JournalEntry(
                nextSequence++,
                type,
                targetId,
                message
        );

        entries.put(key, entry);
        return entry;
    }
}