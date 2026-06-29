package com.pmfml.cognitive_vault.controllers;

import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.services.HybridSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class SearchController {

    private static final String MAX_LIMIT = "50";

    private final HybridSearchService hybridSearchService;

    public SearchController(HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    /**
     * Executes a hybrid semantic and textual search.
     *
     * @param query the search term (required, must not be blank)
     * @param limit the maximum number of results to return (1-50, default 10)
     * @return a list of matching notes ordered by RRF score
     */
    @GetMapping
    public ResponseEntity<List<NoteResponse>> search(
            @RequestParam("query")
            @NotBlank(message = "Search query cannot be blank") String query,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "Limit must be at least 1")
            @Max(value = 50, message = "Limit must not exceed " + MAX_LIMIT) int limit) {
        List<NoteResponse> results = hybridSearchService.search(query, limit);
        return ResponseEntity.ok(results);
    }
}
