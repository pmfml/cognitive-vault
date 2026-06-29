package com.pmfml.cognitive_vault.listeners;

import com.pmfml.cognitive_vault.events.NoteIndexRequestedEvent;
import com.pmfml.cognitive_vault.events.NoteUnindexRequestedEvent;
import com.pmfml.cognitive_vault.services.ElasticsearchIndexer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for note indexing events and synchronizes the Elasticsearch index
 * only after the originating database transaction has successfully committed.
 *
 * <p>This decouples the availability of the search engine from primary write
 * operations: if Elasticsearch is unavailable, the note is still persisted and
 * the indexing failure is simply logged by the indexer.
 */
@Component
public class ElasticsearchIndexingListener {

    private final ElasticsearchIndexer elasticsearchIndexer;

    public ElasticsearchIndexingListener(ElasticsearchIndexer elasticsearchIndexer) {
        this.elasticsearchIndexer = elasticsearchIndexer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNoteIndexRequested(NoteIndexRequestedEvent event) {
        elasticsearchIndexer.index(event.document());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNoteUnindexRequested(NoteUnindexRequestedEvent event) {
        elasticsearchIndexer.deleteNote(event.noteId());
    }
}
