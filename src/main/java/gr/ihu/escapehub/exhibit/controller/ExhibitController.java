package gr.ihu.escapehub.exhibit.controller;

import gr.ihu.escapehub.exhibit.dto.ExhibitInfoDto;
import gr.ihu.escapehub.exhibit.service.ExhibitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exhibits")
public class ExhibitController {

    private final ExhibitService exhibitService;

    public ExhibitController(ExhibitService exhibitService) {
        this.exhibitService = exhibitService;
    }

    @GetMapping
    public ResponseEntity<List<ExhibitInfoDto>> getAllExhibits() {
        return ResponseEntity.ok(exhibitService.getAllExhibitDetails());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExhibitInfoDto> getExhibitById(@PathVariable String id) {
        return exhibitService.getExhibitDetailsById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}