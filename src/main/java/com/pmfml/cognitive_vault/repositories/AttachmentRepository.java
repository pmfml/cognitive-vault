package com.pmfml.cognitive_vault.repositories;

import com.pmfml.cognitive_vault.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import java.util.List;

/**
 * Repository interface for managing Attachment entities.
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByNoteId(UUID noteId);
}
