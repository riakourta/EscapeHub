package gr.ihu.escapehub.journal;

import gr.ihu.escapehub.journal.service.JournalService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JournalServiceTest {

    private final JournalService service = new JournalService();

    @Test
    void recordsExhibitsAndPuzzlesInOrder() {
        service.recordExhibit("exhibit-1", "Διαμάντι Regent");
        service.recordPuzzle("twin-security-plates", "Διπλός Μηχανισμός");

        var entries = service.getEntries();

        assertEquals(2, entries.size());

        assertEquals(1L, entries.get(0).sequence());
        assertEquals("EXHIBIT_COLLECTED", entries.get(0).type());
        assertEquals("exhibit-1", entries.get(0).targetId());

        assertEquals(2L, entries.get(1).sequence());
        assertEquals("PUZZLE_SOLVED", entries.get(1).type());
        assertEquals("twin-security-plates", entries.get(1).targetId());
    }

    @Test
    void repeatedEventsDoNotCreateDuplicateEntries() {
        var first = service.recordExhibit(
                "exhibit-1", "Διαμάντι Regent"
        );

        var repeated = service.recordExhibit(
                "exhibit-1", "Διαμάντι Regent"
        );

        service.recordPuzzle(
                "twin-security-plates", "Διπλός Μηχανισμός"
        );

        service.recordPuzzle(
                "twin-security-plates", "Διπλός Μηχανισμός"
        );

        var entries = service.getEntries();

        assertEquals(first, repeated);
        assertEquals(2, entries.size());
        assertEquals(2L, entries.get(1).sequence());
    }
}