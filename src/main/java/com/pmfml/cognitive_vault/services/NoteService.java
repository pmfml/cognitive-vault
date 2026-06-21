package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.dtos.NoteRequest;
import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.entities.Tag;
import com.pmfml.cognitive_vault.exceptions.ResourceNotFoundException;
import com.pmfml.cognitive_vault.repositories.NoteRepository;
import com.pmfml.cognitive_vault.repositories.RelationshipRepository;
import com.pmfml.cognitive_vault.repositories.TagRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class handling business logic for Notes.
 */
@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final RelationshipRepository relationshipRepository;
    private final EmbeddingModel embeddingModel;

    public NoteService(NoteRepository noteRepository,
                       TagRepository tagRepository,
                       RelationshipRepository relationshipRepository,
                       EmbeddingModel embeddingModel) {
        this.noteRepository = noteRepository;
        this.tagRepository = tagRepository;
        this.relationshipRepository = relationshipRepository;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Creates and saves a new Note.
     */
    @Transactional
    public NoteResponse createNote(NoteRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Note title cannot be empty");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Note content cannot be empty");
        }

        Set<Tag> resolvedTags = resolveTags(request.tags());

        String textToEmbed = request.title() + "\n" + request.content();
        float[] embedding = embeddingModel.embed(textToEmbed);

        Note note = Note.builder()
                .title(request.title())
                .content(request.content())
                .type(request.type())
                .language(request.language())
                .tags(resolvedTags)
                .embedding(embedding)
                .lastAccessedAt(Instant.now())
                .build();

        Note savedNote = noteRepository.save(note);
        return mapToResponse(savedNote);
    }

    /**
     * Retrieves a Note by its ID and updates its last accessed timestamp.
     */
    @Transactional
    public NoteResponse getNoteById(UUID id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

        note.setLastAccessedAt(Instant.now());
        Note updatedNote = noteRepository.save(note);
        return mapToResponse(updatedNote);
    }

    /**
     * Lists all Notes stored in the database.
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getAllNotes() {
        return noteRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing Note.
     */
    @Transactional
    public NoteResponse updateNote(UUID id, NoteRequest request) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Note title cannot be empty");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Note content cannot be empty");
        }

        note.setTitle(request.title());
        note.setContent(request.content());
        note.setType(request.type());
        note.setLanguage(request.language());
        note.setTags(resolveTags(request.tags()));

        String textToEmbed = note.getTitle() + "\n" + note.getContent();
        float[] embedding = embeddingModel.embed(textToEmbed);
        note.setEmbedding(embedding);

        note.setLastAccessedAt(Instant.now());

        Note updatedNote = noteRepository.save(note);
        return mapToResponse(updatedNote);
    }

    /**
     * Deletes a Note and its associated relationships.
     */
    @Transactional
    public void deleteNote(UUID id) {
        if (!noteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Note not found with id: " + id);
        }
        relationshipRepository.deleteByNoteId(id);
        noteRepository.deleteById(id);
    }

    /**
     * Helper method to map a Note entity to a NoteResponse record.
     */
    private NoteResponse mapToResponse(Note note) {
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

    /**
     * Helper method to fetch existing tags or create them if they do not exist.
     */
    private Set<Tag> resolveTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        return tagNames.stream()
                .map(name -> tagRepository.findByName(name.trim().toLowerCase())
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name.trim().toLowerCase()).build())))
                .collect(Collectors.toSet());
    }
}
