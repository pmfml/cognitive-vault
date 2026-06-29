package com.pmfml.cognitive_vault.repositories.elasticsearch;

import com.pmfml.cognitive_vault.documents.NoteDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * Repository interface for managing Elasticsearch NoteDocument indices.
 */
public interface NoteDocumentRepository extends ElasticsearchRepository<NoteDocument, String> {

    /**
     * Executes a full-text search across title, content, and attachment texts.
     * Boosts title relevance by 3x and content by 2x.
     *
     * @param queryText the text to search for
     * @return a list of matching documents ordered by relevance score
     */
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^3\", \"content^2\", \"attachmentTexts\"]}}")
    List<NoteDocument> searchNotes(String queryText);
}
