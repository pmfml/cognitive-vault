package com.pmfml.cognitive_vault.repositories;

import com.pmfml.cognitive_vault.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import java.util.List;

/**
 * Repository interface for managing Attachment entities.
 */
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByNoteId(UUID noteId);
}
