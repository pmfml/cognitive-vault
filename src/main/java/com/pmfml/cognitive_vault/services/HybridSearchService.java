package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.documents.NoteDocument;
import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.repositories.NoteRepository;
import com.pmfml.cognitive_vault.repositories.elasticsearch.NoteDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating Hybrid Search by combining Semantic (vector) search from pgvector
 * and Textual full-text search from Elasticsearch using Reciprocal Rank Fusion (RRF).
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);

    private static final int RRF_K = 60; // Standard constant for Reciprocal Rank Fusion
    private static final int EXPANSION_FACTOR = 2; // Fetch more candidates to improve fusion quality

    private final NoteRepository noteRepository;
    private final NoteDocumentRepository noteDocumentRepository;
    private final EmbeddingModel embeddingModel;

    public HybridSearchService(NoteRepository noteRepository,
                               NoteDocumentRepository noteDocumentRepository,
                               EmbeddingModel embeddingModel) {
        this.noteRepository = noteRepository;
        this.noteDocumentRepository = noteDocumentRepository;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Performs a hybrid search combining pgvector semantic search and Elasticsearch textual search.
     *
     * @param queryText the user search term
     * @param limit     the maximum number of results to return
     * @return a list of ranked NoteResponse objects
     */
    @Transactional
    public List<NoteResponse> search(String queryText, int limit) {
        if (queryText == null || queryText.isBlank()) {
            log.info("Empty query text provided for hybrid search. Returning empty results.");
            return Collections.emptyList();
        }
        if (limit <= 0) {
            log.warn("Invalid search limit: {}. Defaulting to 10.", limit);
            limit = 10;
        }

        int candidateLimit = limit * EXPANSION_FACTOR;

        // 1. Semantic (Vector) Search candidates
        List<Note> vectorCandidates = getVectorCandidates(queryText, candidateLimit);

        // 2. Textual (Elasticsearch) Search candidates
        List<NoteDocument> textCandidates = getTextCandidates(queryText, candidateLimit);

        // 3. Reciprocal Rank Fusion (RRF) Ranking
        List<UUID> rankedIds = runReciprocalRankFusion(vectorCandidates, textCandidates, limit);

        if (rankedIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Fetch from database preserving exact RRF order
        List<Note> notes = noteRepository.findAllById(rankedIds);

        // Update transparent audit access timestamp
        Instant now = Instant.now();
        notes.forEach(note -> note.setLastAccessedAt(now));
        noteRepository.saveAll(notes);

        Map<UUID, Note> noteMap = notes.stream()
                .collect(Collectors.toMap(Note::getId, note -> note));

        return rankedIds.stream()
                .map(noteMap::get)
                .filter(java.util.Objects::nonNull)
                .map(NoteMapper::toResponse)
                .collect(Collectors.toList());
    }

    private List<Note> getVectorCandidates(String queryText, int candidateLimit) {
        try {
            float[] queryVector = embeddingModel.embed(queryText);
            String vectorString = VectorUtils.toVectorString(queryVector);
            return noteRepository.findSimilarNotes(vectorString, candidateLimit);
        } catch (Exception e) {
            log.error("Failed to perform vector search during hybrid search. Error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<NoteDocument> getTextCandidates(String queryText, int candidateLimit) {
        try {
            List<NoteDocument> allResults = noteDocumentRepository.searchNotes(queryText);
            return allResults.stream()
                    .limit(candidateLimit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to perform Elasticsearch search during hybrid search. Error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<UUID> runReciprocalRankFusion(List<Note> vectorList, List<NoteDocument> textList, int limit) {
        Map<UUID, Double> rrfScores = new HashMap<>();

        // Add scores from vector list
        for (int i = 0; i < vectorList.size(); i++) {
            UUID id = vectorList.get(i).getId();
            double score = 1.0 / (RRF_K + (i + 1));
            rrfScores.put(id, rrfScores.getOrDefault(id, 0.0) + score);
        }

        // Add scores from text list
        for (int i = 0; i < textList.size(); i++) {
            try {
                UUID id = UUID.fromString(textList.get(i).getId());
                double score = 1.0 / (RRF_K + (i + 1));
                rrfScores.put(id, rrfScores.getOrDefault(id, 0.0) + score);
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format in Elasticsearch document ID: {}", textList.get(i).getId());
            }
        }

        // Sort by score descending and limit results
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }
}
