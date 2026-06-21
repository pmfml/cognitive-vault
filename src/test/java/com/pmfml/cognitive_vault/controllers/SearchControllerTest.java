package com.pmfml.cognitive_vault.controllers;

import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.entities.NoteType;
import com.pmfml.cognitive_vault.services.HybridSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HybridSearchService hybridSearchService;

    private NoteResponse noteResponse;

    @BeforeEach
    void setUp() {
        noteResponse = new NoteResponse(
                UUID.randomUUID(),
                "Search Match",
                "Some matching content",
                NoteType.TECHNICAL_NOTE,
                "en",
                "Summary match",
                Set.of("java"),
                Instant.now(),
                Instant.now(),
                null
        );
    }

    @Test
    void search_shouldReturnMatchingNotes() throws Exception {
        // Arrange
        when(hybridSearchService.search("java", 10)).thenReturn(List.of(noteResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/search")
                        .param("query", "java")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Search Match"))
                .andExpect(jsonPath("$[0].content").value("Some matching content"));
    }

    @Test
    void search_withDefaultLimit_shouldReturnMatchingNotes() throws Exception {
        // Arrange
        when(hybridSearchService.search("java", 10)).thenReturn(List.of(noteResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/search")
                        .param("query", "java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Search Match"));
    }
}
