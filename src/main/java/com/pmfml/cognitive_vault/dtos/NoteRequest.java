package com.pmfml.cognitive_vault.dtos;

import com.pmfml.cognitive_vault.entities.NoteType;

import java.util.Set;

/**
 * Data Transfer Object for creating or updating a Note.
 */
public record NoteRequest(
        String title,
        String content,
        NoteType type,
        String language,
        Set<String> tags
) {}
