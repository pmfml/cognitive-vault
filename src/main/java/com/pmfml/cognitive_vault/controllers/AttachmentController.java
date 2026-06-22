package com.pmfml.cognitive_vault.controllers;

import com.pmfml.cognitive_vault.dtos.AttachmentResponse;
import com.pmfml.cognitive_vault.services.AttachmentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST Controller for managing Note attachments.
 * Exposes endpoints for uploading, downloading, and deleting files linked to notes.
 */
@RestController
@RequestMapping("/api/v1")
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * Constructor injection for AttachmentService.
     */
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * Uploads a file as an attachment and links it to the specified note.
     *
     * @param noteId the parent note ID
     * @param file   the multipart file to upload
     * @return the metadata details of the uploaded attachment
     * @throws IOException if error occurs while reading the file bytes
     */
    @PostMapping("/notes/{noteId}/attachments")
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable UUID noteId,
            @RequestParam("file") MultipartFile file) {

        try {
            AttachmentResponse response = attachmentService.uploadAttachment(
                    noteId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file content", e);
        }
    }

    /**
     * Retrieves the metadata details of a specific attachment.
     *
     * @param attachmentId the attachment ID
     * @return the attachment metadata details
     */
    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<AttachmentResponse> getAttachmentMetadata(@PathVariable UUID attachmentId) {
        AttachmentResponse response = attachmentService.getAttachmentMetadata(attachmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Downloads the raw binary file content of a specific attachment.
     *
     * @param attachmentId the attachment ID
     * @return the raw bytes of the file with appropriate download headers
     */
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable UUID attachmentId) {
        AttachmentResponse metadata = attachmentService.getAttachmentMetadata(attachmentId);
        byte[] content = attachmentService.getAttachmentContent(attachmentId);

        MediaType mediaType = MediaType.parseMediaType(metadata.contentType());

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(metadata.fileName())
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }

    /**
     * Deletes a specific attachment from the database and storage.
     *
     * @param attachmentId the attachment ID
     * @return 204 No Content response
     */
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable UUID attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }
}
