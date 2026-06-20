package com.pmfml.cognitive_vault.dtos;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for responding with Attachment metadata details.
 */
public record AttachmentResponse(
        UUID id,
        String fileName,
        String s3Key,
        String contentType,
        Long fileSize,
        String extractedText,
        Instant createdAt,
        UUID noteId
) {}
