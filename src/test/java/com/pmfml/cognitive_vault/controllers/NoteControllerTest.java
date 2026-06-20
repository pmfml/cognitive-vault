package com.pmfml.cognitive_vault.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmfml.cognitive_vault.dtos.NoteRequest;
import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.entities.NoteType;
import com.pmfml.cognitive_vault.exceptions.ResourceNotFoundException;
import com.pmfml.cognitive_vault.services.NoteService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteController.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoteService noteService;

    private UUID noteId;
    private NoteResponse noteResponse;

    @BeforeEach
    void setUp() {
        noteId = UUID.randomUUID();
        noteResponse = new NoteResponse(
                noteId,
                "Test Title",
                "Test Content",
                NoteType.TECHNICAL_NOTE,
                null,
                null,
                Set.of("java"),
                Instant.now(),
                Instant.now(),
                null
        );
    }

    @Test
    void createNote_shouldReturnCreated() throws Exception {
        NoteRequest request = new NoteRequest("Test Title", "Test Content", NoteType.TECHNICAL_NOTE, null, Set.of("java"));
        when(noteService.createNote(any(NoteRequest.class))).thenReturn(noteResponse);

        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(noteId.toString()))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.content").value("Test Content"));
    }

    @Test
    void getNoteById_existingId_shouldReturnOk() throws Exception {
        when(noteService.getNoteById(noteId)).thenReturn(noteResponse);

        mockMvc.perform(get("/api/v1/notes/{id}", noteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId.toString()))
                .andExpect(jsonPath("$.title").value("Test Title"));
    }

    @Test
    void getNoteById_nonExistingId_shouldReturnNotFound() throws Exception {
        when(noteService.getNoteById(noteId)).thenThrow(new ResourceNotFoundException("Note not found"));

        mockMvc.perform(get("/api/v1/notes/{id}", noteId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Note not found"));
    }

    @Test
    void getAllNotes_shouldReturnList() throws Exception {
        when(noteService.getAllNotes()).thenReturn(List.of(noteResponse));

        mockMvc.perform(get("/api/v1/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Title"));
    }

    @Test
    void updateNote_shouldReturnUpdatedNote() throws Exception {
        NoteRequest request = new NoteRequest("Updated Title", "Updated Content", NoteType.TECHNICAL_NOTE, null, Set.of("java"));
        NoteResponse updatedResponse = new NoteResponse(
                noteId,
                "Updated Title",
                "Updated Content",
                NoteType.TECHNICAL_NOTE,
                null,
                null,
                Set.of("java"),
                Instant.now(),
                Instant.now(),
                null
        );

        when(noteService.updateNote(eq(noteId), any(NoteRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/notes/{id}", noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"));
    }

    @Test
    void deleteNote_existingId_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/notes/{id}", noteId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNote_nonExistingId_shouldReturnNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Note not found")).when(noteService).deleteNote(noteId);

        mockMvc.perform(delete("/api/v1/notes/{id}", noteId))
                .andExpect(status().isNotFound());
    }
}
