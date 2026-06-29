package com.pmfml.cognitive_vault.events;

import java.util.UUID;

/**
 * Application event signalling that a note should be removed from the
 * Elasticsearch index.
 */
public record NoteUnindexRequestedEvent(UUID noteId) {
}
