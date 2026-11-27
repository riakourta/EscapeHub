package gr.ihu.escapehub.puzzle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.journal.dto.JournalEntry;
import gr.ihu.escapehub.journal.service.JournalService;
import gr.ihu.escapehub.movement.dto.PlayerState;
import gr.ihu.escapehub.puzzle.dto.*;
import gr.ihu.escapehub.puzzle.event.DoorUnlockedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PuzzleService {

    private final RoomPuzzlesConfigDto config;
    private final JournalService journalService;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, Boolean> solvedById = new ConcurrentHashMap<>();
    private volatile boolean doorUnlocked = false;

    public PuzzleService() {
        this(new ObjectMapper(), new JournalService(), null);
    }

    @Autowired
    public PuzzleService(
            ObjectMapper mapper,
            JournalService journalService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.journalService = journalService;
        this.eventPublisher = eventPublisher;
        this.config = loadConfig(mapper);
        for (PuzzleConfigDto p : config.puzzles()) {
            solvedById.put(p.id(), false);
        }
    }

    private RoomPuzzlesConfigDto loadConfig(ObjectMapper mapper) {
        try (InputStream is = getClass().getResourceAsStream("/missions/room1_puzzles.json")) {
            if (is == null) {
                throw new IllegalStateException("Δεν βρέθηκε το missions/room1_puzzles.json");
            }
            return mapper.readValue(is, RoomPuzzlesConfigDto.class);
        } catch (Exception e) {
            throw new IllegalStateException("Σφάλμα φόρτωσης room1_puzzles.json", e);
        }
    }

    public synchronized PuzzleState getState() {
        evaluateJournalClues();
        return new PuzzleState(config.roomId(), "Πρωτόκολλο Πτέρυγας Richelieu", List.of(), doorUnlocked);
    }

    public synchronized PuzzleState evaluate(List<PlayerState> players) {
        return getState();
    }

    public synchronized List<PuzzleSummaryDto> getAllPuzzles() {
        evaluateJournalClues();
        return config.puzzles().stream()
                .map(p -> new PuzzleSummaryDto(
                        p.id(),
                        p.title(),
                        p.description(),
                        p.type(),
                        Boolean.TRUE.equals(solvedById.get(p.id())),
                        p.isFinal()
                ))
                .toList();
    }

    public synchronized boolean isDoorUnlocked() {
        evaluateJournalClues();
        return doorUnlocked;
    }

    public String getNextRoom() {
        return config.nextRoom();
    }

    public synchronized void evaluateJournalClues() {
        if (journalService == null) {
            return;
        }

        List<JournalEntry> entries = journalService.getEntries();
        Set<String> distinctAuthors = new HashSet<>();
        boolean requiresDifferentPlayers = false;

        for (PuzzleConfigDto puzzle : config.puzzles()) {
            if (Boolean.TRUE.equals(puzzle.requiresDifferentPlayers())) {
                requiresDifferentPlayers = true;
            }

            Set<String> accepted = new HashSet<>();
            if (puzzle.acceptedSolutions() != null) {
                for (String sol : puzzle.acceptedSolutions()) {
                    accepted.add(sol.trim().toLowerCase());
                }
            }

            JournalEntry matchingEntry = entries.stream()
                    .filter(e -> e.targetId() != null && accepted.contains(e.targetId().trim().toLowerCase()))
                    .findFirst()
                    .orElse(null);

            boolean stepSolved = matchingEntry != null;
            solvedById.put(puzzle.id(), stepSolved);

            if (stepSolved && matchingEntry.author() != null && !matchingEntry.author().isBlank()) {
                distinctAuthors.add(matchingEntry.author().trim().toLowerCase());
            }
        }

        boolean allSolved = config.puzzles().stream()
                .allMatch(p -> Boolean.TRUE.equals(solvedById.get(p.id())));

        boolean distinctPlayersOk = !requiresDifferentPlayers || distinctAuthors.size() >= config.puzzles().size();

        if (allSolved && distinctPlayersOk && !doorUnlocked) {
            doorUnlocked = true;
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new DoorUnlockedEvent(config.roomId(), config.nextRoom()));
            }
        }
    }

    public synchronized PuzzleSubmitResponseDto submitAnswer(String puzzleId, String rawAnswer) {
        evaluateJournalClues();
        String msg = doorUnlocked
                ? "Οι δύο κωδικοί καταχωρήθηκαν στο Κοινό Ημερολόγιο από δύο διαφορετικούς παίκτες — η πόρτα ξεκλείδωσε!"
                : "Αναμονή καταχώρισης των δύο κωδικών στο Κοινό Ημερολόγιο από δύο διαφορετικούς παίκτες.";
        return new PuzzleSubmitResponseDto(puzzleId, doorUnlocked, doorUnlocked, msg, doorUnlocked, config.nextRoom());
    }
}