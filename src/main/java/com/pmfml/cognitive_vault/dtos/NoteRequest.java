package com.pmfml.cognitive_vault.dtos;

import com.pmfml.cognitive_vault.entities.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Data Transfer Object for creating or updating a Note.
 */
public record NoteRequest(
        @NotBlank(message = "Note title cannot be empty")
        String title,

        @NotBlank(message = "Note content cannot be empty")
        String content,

        @NotNull(message = "Note type is required")
        NoteType type,

        String language,
        Set<String> tags
) {}
