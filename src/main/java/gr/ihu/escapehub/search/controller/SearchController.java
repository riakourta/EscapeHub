package gr.ihu.escapehub.search.controller;

import gr.ihu.escapehub.search.dto.SearchResultDto;
import gr.ihu.escapehub.search.service.ExhibitSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ExhibitSearchService exhibitSearchService;

    public SearchController(ExhibitSearchService exhibitSearchService) {
        this.exhibitSearchService = exhibitSearchService;
    }

    @GetMapping
    public ResponseEntity<List<SearchResultDto>> search(
            @RequestParam(name = "q", required = false) String query
    ) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        List<SearchResultDto> results = exhibitSearchService.search(query);
        return ResponseEntity.ok(results);
    }
}