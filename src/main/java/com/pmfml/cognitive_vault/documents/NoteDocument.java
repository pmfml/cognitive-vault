package com.pmfml.cognitive_vault.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.HashSet;
import java.util.Set;

/**
 * Elasticsearch Document representing a Note and its metadata.
 * Designed to separate indexing concerns from JPA relational entity concerns.
 */
@Document(indexName = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private String type;

    @Builder.Default
    @Field(type = FieldType.Keyword)
    private Set<String> tags = new HashSet<>();

    @Builder.Default
    @Field(type = FieldType.Text, analyzer = "standard")
    private Set<String> attachmentTexts = new HashSet<>();
}
