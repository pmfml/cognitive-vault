package com.pmfml.cognitive_vault.repositories;

import com.pmfml.cognitive_vault.entities.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing Relationship entities.
 */
@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, UUID> {

    List<Relationship> findBySourceNoteId(UUID sourceNoteId);

    @Modifying
    @Query("DELETE FROM Relationship r WHERE r.sourceNote.id = :noteId OR r.targetNote.id = :noteId")
    void deleteByNoteId(@Param("noteId") UUID noteId);
}
