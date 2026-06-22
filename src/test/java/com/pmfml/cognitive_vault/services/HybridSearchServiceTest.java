package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.documents.NoteDocument;
import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.entities.NoteType;
import com.pmfml.cognitive_vault.repositories.NoteRepository;
import com.pmfml.cognitive_vault.repositories.elasticsearch.NoteDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NoteDocumentRepository noteDocumentRepository;

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private HybridSearchService hybridSearchService;

    private UUID idA;
    private UUID idB;
    private Note noteA;
    private Note noteB;
    private NoteDocument docA;
    private NoteDocument docB;

    @BeforeEach
    void setUp() {
        idA = UUID.randomUUID();
        idB = UUID.randomUUID();

        noteA = Note.builder()
                .id(idA)
                .title("Note A Title")
                .content("Note A Content")
                .type(NoteType.TECHNICAL_NOTE)
                .createdAt(Instant.now())
                .build();

        noteB = Note.builder()
                .id(idB)
                .title("Note B Title")
                .content("Note B Content")
                .type(NoteType.TECHNICAL_NOTE)
                .createdAt(Instant.now())
                .build();

        docA = NoteDocument.builder()
                .id(idA.toString())
                .title("Note A Title")
                .content("Note A Content")
                .type(NoteType.TECHNICAL_NOTE.name())
                .build();

        docB = NoteDocument.builder()
                .id(idB.toString())
                .title("Note B Title")
                .content("Note B Content")
                .type(NoteType.TECHNICAL_NOTE.name())
                .build();
    }

    @Test
    void search_whenQueryIsEmpty_shouldReturnEmptyList() {
        List<NoteResponse> results = hybridSearchService.search("", 5);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_whenRrfRanksAAboveB_shouldReturnCorrectOrder() {
        // Arrange
        // Vector Search: Note A, Note B
        // Text Search: Note A, Note B
        when(embeddingModel.embed("query")).thenReturn(new float[384]);
        when(noteRepository.findSimilarNotes(anyString(), anyInt())).thenReturn(List.of(noteA, noteB));
        when(noteDocumentRepository.searchNotes("query")).thenReturn(List.of(docA, docB));
        when(noteRepository.findAllById(any())).thenReturn(List.of(noteA, noteB));

        // Act
        Instant beforeSearch = Instant.now();
        List<NoteResponse> responses = hybridSearchService.search("query", 2);

        // Assert
        assertEquals(2, responses.size());
        assertEquals(idA, responses.get(0).id());
        assertEquals(idB, responses.get(1).id());

        // Verify Audit Hook
        assertNotNull(noteA.getLastAccessedAt());
        assertNotNull(noteB.getLastAccessedAt());
        assertTrue(noteA.getLastAccessedAt().isAfter(beforeSearch) || noteA.getLastAccessedAt().equals(beforeSearch));
        verify(noteRepository, times(1)).saveAll(anyList());
    }

    @Test
    void search_whenRrfRanksBAboveA_shouldReturnCorrectOrder() {
        // Arrange
        // Vector Search: Note B
        // Text Search: Note B, Note A
        when(embeddingModel.embed("query")).thenReturn(new float[384]);
        when(noteRepository.findSimilarNotes(anyString(), anyInt())).thenReturn(List.of(noteB));
        when(noteDocumentRepository.searchNotes("query")).thenReturn(List.of(docB, docA));
        when(noteRepository.findAllById(any())).thenReturn(List.of(noteB, noteA));

        // Act
        Instant beforeSearch = Instant.now();
        List<NoteResponse> responses = hybridSearchService.search("query", 2);

        // Assert
        assertEquals(2, responses.size());
        assertEquals(idB, responses.get(0).id());
        assertEquals(idA, responses.get(1).id());

        // Verify Audit Hook
        assertNotNull(noteA.getLastAccessedAt());
        assertNotNull(noteB.getLastAccessedAt());
        assertTrue(noteB.getLastAccessedAt().isAfter(beforeSearch) || noteB.getLastAccessedAt().equals(beforeSearch));
        verify(noteRepository, times(1)).saveAll(anyList());
    }
}
