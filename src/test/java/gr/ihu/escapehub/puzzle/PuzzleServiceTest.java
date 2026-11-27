package gr.ihu.escapehub.puzzle;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.journal.service.JournalService;
import gr.ihu.escapehub.puzzle.event.DoorUnlockedEvent;
import gr.ihu.escapehub.puzzle.service.PuzzleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PuzzleServiceTest {

    private JournalService journalService;
    private PuzzleService puzzleService;
    private List<DoorUnlockedEvent> publishedEvents;

    @BeforeEach
    void setUp() {
        journalService = new JournalService();
        publishedEvents = new ArrayList<>();
        ApplicationEventPublisher publisher = event -> {
            if (event instanceof DoorUnlockedEvent doorEvent) {
                publishedEvents.add(doorEvent);
            }
        };
        puzzleService = new PuzzleService(new ObjectMapper(), journalService, publisher);
    }

    @Test
    void doorStartsLocked() {
        assertFalse(puzzleService.isDoorUnlocked());
        assertEquals("room2.html", puzzleService.getNextRoom());
    }

    @Test
    void doorRemainsLockedWhenSamePlayerEntersBothCodes() {
        journalService.recordClue("1722", "rozalia");
        journalService.recordClue("140", "rozalia");

        assertFalse(puzzleService.isDoorUnlocked());
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    void doorRemainsLockedWhenWrongCodeIsEntered() {
        journalService.recordClue("1722", "rozalia");
        journalService.recordClue("999", "eleftheria");

        assertFalse(puzzleService.isDoorUnlocked());
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    void doorUnlocksAutomaticallyWhenTwoDifferentPlayersEnterValidCodes() {
        journalService.recordClue("1722", "rozalia");
        assertFalse(puzzleService.isDoorUnlocked());

        journalService.recordClue("140", "eleftheria");
        assertTrue(puzzleService.isDoorUnlocked());
        assertEquals(1, publishedEvents.size());
        assertEquals("room2.html", publishedEvents.get(0).nextRoom());
    }

    @Test
    void doorAcceptsDecimalCaratFormatFromSecondPlayer() {
        journalService.recordClue("1722", "player1");
        journalService.recordClue("140,64", "player2");

        assertTrue(puzzleService.isDoorUnlocked());
    }
}