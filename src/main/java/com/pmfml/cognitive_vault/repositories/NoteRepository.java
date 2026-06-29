package com.pmfml.cognitive_vault.repositories;

import com.pmfml.cognitive_vault.entities.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing Note entities, including pgvector similarity search.
 */
public interface NoteRepository extends JpaRepository<Note, UUID> {

    /**
     * Finds notes ordered by semantic similarity to the given embedding using the pgvector cosine distance operator (<=>).
     * The input embedding vector must be formatted as a string (e.g., "[0.1,0.2,0.3...]").
     *
     * @param embedding the string representation of the target vector
     * @param limit maximum number of results
     * @return a list of notes ordered by similarity
     */
    @Query(value = "SELECT * FROM notes n ORDER BY n.embedding <=> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<Note> findSimilarNotes(
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    /**
     * Finds notes ordered by semantic similarity to the given embedding, excluding a specific note ID.
     * Useful when calculating relationships for a specific saved note.
     *
     * @param embedding the string representation of the target vector
     * @param excludeNoteId the note ID to exclude from recommendations
     * @param limit maximum number of results
     * @return a list of notes ordered by similarity
     */
    @Query(value = "SELECT * FROM notes n WHERE n.id != :excludeNoteId ORDER BY n.embedding <=> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<Note> findSimilarNotesExcludingSelf(
            @Param("embedding") String embedding,
            @Param("excludeNoteId") UUID excludeNoteId,
            @Param("limit") int limit
    );

    /**
     * Finds notes that require study/review based on three decay rules:
     * 1. Never reviewed and created > 24 hours ago
     * 2. Accessed recently after the last review
     * 3. Last reviewed > 30 days ago
     */
    @Query("SELECT n FROM Note n WHERE " +
           "(n.lastReviewedAt IS NULL AND n.createdAt < :oneDayAgo) OR " +
           "(n.lastReviewedAt IS NOT NULL AND n.lastAccessedAt > n.lastReviewedAt) OR " +
           "(n.lastReviewedAt < :thirtyDaysAgo)")
    List<Note> findNotesNeedingReview(
            @Param("oneDayAgo") java.time.Instant oneDayAgo,
            @Param("thirtyDaysAgo") java.time.Instant thirtyDaysAgo
    );
}
