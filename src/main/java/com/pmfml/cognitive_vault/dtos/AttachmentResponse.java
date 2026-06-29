package com.pmfml.cognitive_vault.dtos;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for responding with Attachment metadata details.
 * Internal storage details (such as the S3 key) and the full extracted text
 * are intentionally omitted to avoid leaking infrastructure information.
 */
public record AttachmentResponse(
        UUID id,
        String fileName,
        String contentType,
        Long fileSize,
        Instant createdAt,
        UUID noteId
) {}
