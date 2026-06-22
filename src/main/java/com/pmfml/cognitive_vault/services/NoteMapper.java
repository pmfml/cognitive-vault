package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.entities.Tag;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for mapping Note entities to NoteResponse DTOs.
 * Centralizes the conversion logic to avoid duplication across services.
 */
public final class NoteMapper {

    private NoteMapper() {
        // Utility class, no instantiation
    }

    /**
     * Maps a Note JPA entity to a NoteResponse record.
     *
     * @param note the source entity
     * @return the mapped DTO
     */
    public static NoteResponse toResponse(Note note) {
        Set<String> tagNames = note.getTags() != null
                ? note.getTags().stream().map(Tag::getName).collect(Collectors.toSet())
                : Set.of();

        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getType(),
                note.getLanguage(),
                note.getSummary(),
                tagNames,
                note.getCreatedAt(),
                note.getLastAccessedAt(),
                note.getLastReviewedAt()
        );
    }
}
