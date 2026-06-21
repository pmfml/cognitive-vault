package com.pmfml.cognitive_vault.controllers;

import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.services.HybridSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for exposing hybrid search operations.
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final HybridSearchService hybridSearchService;

    public SearchController(HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    /**
     * Executes a hybrid semantic and textual search.
     *
     * @param query the search term (required)
     * @param limit the maximum number of results to return (default 10)
     * @return a list of matching notes ordered by RRF score
     */
    @GetMapping
    public ResponseEntity<List<NoteResponse>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<NoteResponse> results = hybridSearchService.search(query, limit);
        return ResponseEntity.ok(results);
    }
}
