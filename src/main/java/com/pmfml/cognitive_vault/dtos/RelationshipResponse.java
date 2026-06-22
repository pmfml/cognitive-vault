package com.pmfml.cognitive_vault.dtos;

import java.util.UUID;

/**
 * Data Transfer Object for responding with semantic Relationship details.
 */
public record RelationshipResponse(
        UUID relationshipId,
        NoteResponse targetNote,
        Double similarityScore
) {}
