package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.entities.NoteType;
import com.pmfml.cognitive_vault.entities.Relationship;
import com.pmfml.cognitive_vault.repositories.NoteRepository;
import com.pmfml.cognitive_vault.repositories.RelationshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationshipServiceTest {

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private RelationshipService relationshipService;

    private Note sourceNote;
    private UUID sourceNoteId;

    @BeforeEach
    void setUp() {
        sourceNoteId = UUID.randomUUID();
        float[] embedding = new float[384];
        embedding[0] = 1.0f; // vetor canônico apontando para dimensão 0
        sourceNote = Note.builder()
                .id(sourceNoteId)
                .title("Source Note")
                .content("Content")
                .type(NoteType.TECHNICAL_NOTE)
                .embedding(embedding)
                .build();
    }

    // --- Testes de calculateCosineSimilarity ---

    @Test
    void calculateCosineSimilarity_identicalVectors_shouldReturnOne() {
        float[] vectorA = new float[384];
        vectorA[0] = 1.0f;
        float[] vectorB = new float[384];
        vectorB[0] = 1.0f;

        double result = relationshipService.calculateCosineSimilarity(vectorA, vectorB);

        assertEquals(1.0, result, 1e-9, "Vetores idênticos devem ter similaridade 1.0");
    }

    @Test
    void calculateCosineSimilarity_orthogonalVectors_shouldReturnZero() {
        float[] vectorA = new float[384];
        vectorA[0] = 1.0f; // aponta para dimensão 0
        float[] vectorB = new float[384];
        vectorB[1] = 1.0f; // aponta para dimensão 1

        double result = relationshipService.calculateCosineSimilarity(vectorA, vectorB);

        assertEquals(0.0, result, 1e-9, "Vetores ortogonais devem ter similaridade 0.0");
    }

    @Test
    void calculateCosineSimilarity_nullVectorA_shouldReturnZero() {
        float[] vectorB = new float[384];
        vectorB[0] = 1.0f;

        double result = relationshipService.calculateCosineSimilarity(null, vectorB);

        assertEquals(0.0, result);
    }

    @Test
    void calculateCosineSimilarity_nullVectorB_shouldReturnZero() {
        float[] vectorA = new float[384];
        vectorA[0] = 1.0f;

        double result = relationshipService.calculateCosineSimilarity(vectorA, null);

        assertEquals(0.0, result);
    }

    @Test
    void calculateCosineSimilarity_zeroVectors_shouldReturnZero() {
        float[] vectorA = new float[384]; // todos zeros
        float[] vectorB = new float[384]; // todos zeros

        double result = relationshipService.calculateCosineSimilarity(vectorA, vectorB);

        assertEquals(0.0, result, "Vetores nulos devem ter similaridade 0.0 (evita divisão por zero)");
    }

    @Test
    void calculateCosineSimilarity_knownValues_shouldBeAccurate() {
        // [1, 0] e [1, 1] têm cos(45°) = 1/sqrt(2) ≈ 0.7071
        float[] vectorA = new float[384];
        vectorA[0] = 1.0f;
        float[] vectorB = new float[384];
        vectorB[0] = 1.0f;
        vectorB[1] = 1.0f;

        double result = relationshipService.calculateCosineSimilarity(vectorA, vectorB);
        double expected = 1.0 / Math.sqrt(2.0);

        assertEquals(expected, result, 1e-6, "Similaridade deve corresponder ao cosseno do ângulo entre os vetores");
    }

    // --- Testes de recalculateRelationships ---

    @Test
    void recalculateRelationships_shouldDeleteOldRelationshipsFirst() {
        when(noteRepository.findSimilarNotesExcludingSelf(anyString(), eq(sourceNoteId), anyInt()))
                .thenReturn(List.of());

        relationshipService.recalculateRelationships(sourceNote);

        verify(relationshipRepository, times(1)).deleteBySourceNoteId(sourceNoteId);
    }

    @Test
    void recalculateRelationships_withCandidates_shouldSaveTop5() {
        // Criamos 7 notas candidatas com embeddings distintos
        List<Note> candidates = buildCandidateNotes(7);
        when(noteRepository.findSimilarNotesExcludingSelf(anyString(), eq(sourceNoteId), eq(15)))
                .thenReturn(candidates);
        when(relationshipRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        relationshipService.recalculateRelationships(sourceNote);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Relationship>> captor = ArgumentCaptor.forClass(List.class);
        verify(relationshipRepository).saveAll(captor.capture());

        List<Relationship> saved = captor.getValue();
        assertTrue(saved.size() <= 5, "Deve salvar no máximo 5 relacionamentos");
        assertEquals(sourceNote, saved.get(0).getSourceNote(), "SourceNote deve ser a nota de origem");
    }

    @Test
    void recalculateRelationships_shouldSaveRelationshipsSortedByScoreDescending() {
        // Candidata A: altamente similar (dimensão 0, igual à fonte)
        float[] highSimilarity = new float[384];
        highSimilarity[0] = 1.0f;

        // Candidata B: baixa similaridade (dimensão 1, ortogonal à fonte)
        float[] lowSimilarity = new float[384];
        lowSimilarity[1] = 1.0f;

        Note highCandidate = Note.builder().id(UUID.randomUUID()).title("High")
                .content("c").type(NoteType.TECHNICAL_NOTE).embedding(highSimilarity).build();
        Note lowCandidate = Note.builder().id(UUID.randomUUID()).title("Low")
                .content("c").type(NoteType.TECHNICAL_NOTE).embedding(lowSimilarity).build();

        // A ordem de retorno do banco é intencional: primeira a de baixa similaridade
        when(noteRepository.findSimilarNotesExcludingSelf(anyString(), eq(sourceNoteId), eq(15)))
                .thenReturn(List.of(lowCandidate, highCandidate));
        when(relationshipRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        relationshipService.recalculateRelationships(sourceNote);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Relationship>> captor = ArgumentCaptor.forClass(List.class);
        verify(relationshipRepository).saveAll(captor.capture());

        List<Relationship> saved = captor.getValue();
        assertEquals(2, saved.size());
        // A primeira relação salva deve ser a de maior pontuação (highCandidate)
        assertTrue(saved.get(0).getSimilarityScore() >= saved.get(1).getSimilarityScore(),
                "Relações devem estar ordenadas por score decrescente");
        assertEquals("High", saved.get(0).getTargetNote().getTitle());
    }

    @Test
    void recalculateRelationships_noteWithNullEmbedding_shouldDoNothing() {
        Note noteWithoutEmbedding = Note.builder()
                .id(UUID.randomUUID())
                .title("No Embedding")
                .content("content")
                .type(NoteType.TECHNICAL_NOTE)
                .embedding(null)
                .build();

        relationshipService.recalculateRelationships(noteWithoutEmbedding);

        verifyNoInteractions(relationshipRepository, noteRepository);
    }

    @Test
    void recalculateRelationships_withNoCandidates_shouldSaveEmptyList() {
        when(noteRepository.findSimilarNotesExcludingSelf(anyString(), eq(sourceNoteId), eq(15)))
                .thenReturn(List.of());
        when(relationshipRepository.saveAll(anyList())).thenReturn(List.of());

        relationshipService.recalculateRelationships(sourceNote);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Relationship>> captor = ArgumentCaptor.forClass(List.class);
        verify(relationshipRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().isEmpty(), "Sem candidatos, nenhum relacionamento deve ser salvo");
    }

    // --- Helper ---

    private List<Note> buildCandidateNotes(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    float[] emb = new float[384];
                    emb[i % 384] = (i + 1) * 0.1f;
                    return Note.builder()
                            .id(UUID.randomUUID())
                            .title("Candidate " + i)
                            .content("content")
                            .type(NoteType.TECHNICAL_NOTE)
                            .embedding(emb)
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
