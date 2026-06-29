package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.dtos.AttachmentResponse;
import com.pmfml.cognitive_vault.entities.Attachment;
import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.events.NoteIndexRequestedEvent;
import com.pmfml.cognitive_vault.exceptions.ResourceNotFoundException;
import com.pmfml.cognitive_vault.repositories.AttachmentRepository;
import com.pmfml.cognitive_vault.repositories.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service handling business logic for Note attachments.
 * Integrates storage operations, database persistence, and document processing.
 */
@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    private final AttachmentRepository attachmentRepository;
    private final NoteRepository noteRepository;
    private final AttachmentStorageService storageService;
    private final DocumentProcessor documentProcessor;
    private final ElasticsearchIndexer elasticsearchIndexer;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructor injection for repositories and utility services.
     */
    public AttachmentService(AttachmentRepository attachmentRepository,
                             NoteRepository noteRepository,
                             AttachmentStorageService storageService,
                             DocumentProcessor documentProcessor,
                             ElasticsearchIndexer elasticsearchIndexer,
                             ApplicationEventPublisher eventPublisher) {
        this.attachmentRepository = attachmentRepository;
        this.noteRepository = noteRepository;
        this.storageService = storageService;
        this.documentProcessor = documentProcessor;
        this.elasticsearchIndexer = elasticsearchIndexer;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Uploads and stores a note attachment. Saves physical bytes to MinIO/S3,
     * extracts text, and registers the metadata record in the database.
     *
     * @param noteId      the parent note ID
     * @param filename    the original filename
     * @param contentType the MIME type of the file
     * @param content     the raw file bytes
     * @return the saved attachment response DTO
     */
    @Transactional
    public AttachmentResponse uploadAttachment(UUID noteId, String filename, String contentType, byte[] content) {
        log.info("Processing upload request for attachment: {} linked to note: {}", filename, noteId);

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + noteId));

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content cannot be empty");
        }

        // Generate a unique S3 key partitioned by noteId
        String s3Key = noteId.toString() + "/" + UUID.randomUUID().toString() + "_" + filename;

        // 1. Upload file bytes to MinIO
        storageService.uploadFile(s3Key, content, contentType);

        // Register a compensating action: if the surrounding transaction rolls
        // back after this upload, the orphaned object is removed from storage.
        registerS3RollbackCompensation(s3Key);

        // 2. Extract textual content if it is a text-based format
        String extractedText = documentProcessor.extractText(content, contentType, filename);

        // 3. Save metadata to database
        Attachment attachment = Attachment.builder()
                .fileName(filename)
                .s3Key(s3Key)
                .contentType(contentType)
                .fileSize((long) content.length)
                .extractedText(extractedText)
                .note(note)
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Attachment metadata persisted successfully with ID: {}", saved.getId());

        // Re-index the parent note to include the new attachment's extracted text
        eventPublisher.publishEvent(new NoteIndexRequestedEvent(elasticsearchIndexer.toDocument(note)));

        return mapToResponse(saved);
    }

    /**
     * Downloads and retrieves the raw content bytes of an attachment from S3.
     *
     * @param attachmentId the target attachment ID
     * @return the byte content of the file
     */
    @Transactional(readOnly = true)
    public byte[] getAttachmentContent(UUID attachmentId) {
        log.info("Retrieving content bytes for attachment ID: {}", attachmentId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId));

        return storageService.downloadFile(attachment.getS3Key());
    }

    /**
     * Retrieves the metadata info of an attachment by its ID.
     *
     * @param attachmentId the target attachment ID
     * @return the attachment response DTO
     */
    @Transactional(readOnly = true)
    public AttachmentResponse getAttachmentMetadata(UUID attachmentId) {
        log.info("Retrieving metadata for attachment ID: {}", attachmentId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId));

        return mapToResponse(attachment);
    }

    /**
     * Retrieves all attachments linked to a specific note.
     *
     * @param noteId the parent note ID
     * @return a list of attachment response DTOs
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByNoteId(UUID noteId) {
        log.info("Retrieving attachments for note ID: {}", noteId);
        List<Attachment> attachments = attachmentRepository.findByNoteId(noteId);
        return attachments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Deletes an attachment from both the database and the S3 physical storage.
     *
     * @param attachmentId the ID of the attachment to remove
     */
    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        log.info("Deleting attachment ID: {}", attachmentId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId));

        // 1. Delete physical object in S3/MinIO
        storageService.deleteFile(attachment.getS3Key());

        Note note = attachment.getNote();

        // 2. Delete database record
        attachmentRepository.delete(attachment);
        log.info("Attachment ID: {} deleted from database and storage.", attachmentId);

        if (note != null) {
            // Remove the deleted attachment from the note's list to prevent indexing stale data
            if (note.getAttachments() != null) {
                note.getAttachments().remove(attachment);
            }
            // 3. Re-index parent note in Elasticsearch
            eventPublisher.publishEvent(new NoteIndexRequestedEvent(elasticsearchIndexer.toDocument(note)));
        }
    }

    /**
     * Registers a transaction synchronization that deletes the freshly uploaded
     * S3 object if the surrounding transaction ends up rolling back. This keeps
     * the object storage consistent with the database when persistence fails
     * after the upload has already happened.
     *
     * <p>The hook is only registered when a transaction synchronization is
     * active; otherwise the method is a no-op (e.g. when invoked outside a
     * transactional context).
     *
     * @param s3Key the key of the uploaded object to compensate for
     */
    private void registerS3RollbackCompensation(String s3Key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    log.warn("Transaction rolled back after S3 upload. Deleting orphaned object: {}", s3Key);
                    try {
                        storageService.deleteFile(s3Key);
                    } catch (Exception e) {
                        log.error("Failed to delete orphaned S3 object: {}. Manual cleanup may be required.", s3Key, e);
                    }
                }
            }
        });
    }

    /**
     * Maps an Attachment JPA entity to an AttachmentResponse record.
     */
    private AttachmentResponse mapToResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getCreatedAt(),
                attachment.getNote().getId()
        );
    }
}
