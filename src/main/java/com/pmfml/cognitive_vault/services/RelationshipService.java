package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.entities.Relationship;
import com.pmfml.cognitive_vault.repositories.NoteRepository;
import com.pmfml.cognitive_vault.repositories.RelationshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating and persisting semantic relationships between notes.
 */
@Service
public class RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final NoteRepository noteRepository;

    public RelationshipService(RelationshipRepository relationshipRepository, NoteRepository noteRepository) {
        this.relationshipRepository = relationshipRepository;
        this.noteRepository = noteRepository;
    }

    /**
     * Recalculates relationships for a given note: deletes old ones where this note is the source,
     * finds candidate notes by similarity, calculates exact cosine similarity in memory,
     * and saves the top 5 relationships.
     */
    @Transactional
    public void recalculateRelationships(Note note) {
        if (note.getEmbedding() == null) {
            return;
        }

        // 1. Delete old relationships where this note is the source
        relationshipRepository.deleteBySourceNoteId(note.getId());

        // 2. Retrieve candidate notes (e.g. up to 15 closest) excluding self
        String vectorStr = toVectorString(note.getEmbedding());
        List<Note> candidates = noteRepository.findSimilarNotesExcludingSelf(vectorStr, note.getId(), 15);

        // 3. Compute cosine similarity in memory and build Relationship objects
        List<Relationship> relationships = candidates.stream()
                .map(candidate -> {
                    double similarity = calculateCosineSimilarity(note.getEmbedding(), candidate.getEmbedding());
                    return Relationship.builder()
                            .sourceNote(note)
                            .targetNote(candidate)
                            .similarityScore(similarity)
                            .build();
                })
                .sorted((r1, r2) -> Double.compare(r2.getSimilarityScore(), r1.getSimilarityScore()))
                .limit(5)
                .collect(Collectors.toList());

        // 4. Save top 5 relationships
        relationshipRepository.saveAll(relationships);
    }

    /**
     * Calculates the cosine similarity of two float vectors: (u . v) / (||u|| ||v||)
     */
    public double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length || vectorA.length == 0) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
