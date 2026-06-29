package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.documents.NoteDocument;
import com.pmfml.cognitive_vault.entities.Attachment;
import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.entities.Tag;
import com.pmfml.cognitive_vault.repositories.elasticsearch.NoteDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for synchronizing Note entity states to the Elasticsearch index.
 */
@Service
public class ElasticsearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexer.class);

    private final NoteDocumentRepository noteDocumentRepository;

    public ElasticsearchIndexer(NoteDocumentRepository noteDocumentRepository) {
        this.noteDocumentRepository = noteDocumentRepository;
    }

    /**
     * Builds an Elasticsearch {@link NoteDocument} from a JPA Note entity.
     * This must be invoked while the persistence context is still open (e.g.
     * inside the originating transaction), since it reads lazy associations
     * such as tags and attachments.
     *
     * @param note the JPA Note entity
     * @return the mapped document, or {@code null} if the note is {@code null}
     */
    public NoteDocument toDocument(Note note) {
        if (note == null) {
            return null;
        }

        Set<String> tagNames = note.getTags() != null
                ? note.getTags().stream().map(Tag::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<String> attachmentTexts = note.getAttachments() != null
                ? note.getAttachments().stream()
                        .map(Attachment::getExtractedText)
                        .filter(text -> text != null && !text.isBlank())
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        return NoteDocument.builder()
                .id(note.getId().toString())
                .title(note.getTitle())
                .content(note.getContent())
                .type(note.getType().name())
                .tags(tagNames)
                .attachmentTexts(attachmentTexts)
                .build();
    }

    /**
     * Persists a pre-built document into the Elasticsearch index.
     * Failures are logged but never propagated, so a search-engine outage does
     * not affect the primary data store.
     *
     * @param document the document to index
     */
    public void index(NoteDocument document) {
        if (document == null) {
            return;
        }
        log.info("Indexing note ID: {} to Elasticsearch index", document.getId());
        try {
            noteDocumentRepository.save(document);
            log.info("Successfully indexed note ID: {}", document.getId());
        } catch (Exception e) {
            log.error("Failed to index note ID: {} to Elasticsearch. Error: {}", document.getId(), e.getMessage());
        }
    }

    /**
     * Deletes a Note document from the Elasticsearch index.
     *
     * @param noteId the ID of the note to remove
     */
    public void deleteNote(UUID noteId) {
        if (noteId == null) {
            return;
        }
        log.info("Deleting note ID: {} from Elasticsearch index", noteId);
        try {
            noteDocumentRepository.deleteById(noteId.toString());
            log.info("Successfully deleted note ID: {} from Elasticsearch", noteId);
        } catch (Exception e) {
            log.error("Failed to delete note ID: {} from Elasticsearch index. Error: {}", noteId, e.getMessage());
        }
    }
}
