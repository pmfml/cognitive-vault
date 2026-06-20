package com.pmfml.cognitive_vault.dtos;

import com.pmfml.cognitive_vault.entities.NoteType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for responding with Note details.
 */
public record NoteResponse(
        UUID id,
        String title,
        String content,
        NoteType type,
        String language,
        String summary,
        Set<String> tags,
        Instant createdAt,
        Instant lastAccessedAt,
        Instant lastReviewedAt
) {}
