package com.pmfml.cognitive_vault.events;

import com.pmfml.cognitive_vault.documents.NoteDocument;

/**
 * Application event signalling that a note should be (re)indexed in Elasticsearch.
 * The fully-built {@link NoteDocument} is carried in the event so the indexing
 * listener does not need to access lazy JPA associations after the transaction
 * has committed.
 */
public record NoteIndexRequestedEvent(NoteDocument document) {
}
