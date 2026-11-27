package gr.ihu.escapehub.puzzle.controller;

import gr.ihu.escapehub.puzzle.dto.PuzzleSubmitRequestDto;
import gr.ihu.escapehub.puzzle.dto.PuzzleSubmitResponseDto;
import gr.ihu.escapehub.puzzle.service.PuzzleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/puzzles")
public class PuzzleController {

    private final PuzzleService puzzleService;

    public PuzzleController(PuzzleService puzzleService) {
        this.puzzleService = puzzleService;
    }

    @GetMapping
    public ResponseEntity getPuzzles() {
        return ResponseEntity.ok(Map.of(
                "puzzles", puzzleService.getAllPuzzles(),
                "doorUnlocked", puzzleService.isDoorUnlocked(),
                "nextRoom", puzzleService.getNextRoom()
        ));
    }

    @PostMapping("/{puzzleId}/solve")
    public ResponseEntity<PuzzleSubmitResponseDto> solvePuzzle(
            @PathVariable String puzzleId,
            @RequestBody PuzzleSubmitRequestDto request
    ) {
        String answer = request != null ? request.answer() : "";
        return ResponseEntity.ok(puzzleService.submitAnswer(puzzleId, answer));
    }
}
