package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.dtos.NoteRequest;
import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.dtos.RelationshipResponse;
import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.entities.NoteType;
import com.pmfml.cognitive_vault.entities.Relationship;
import com.pmfml.cognitive_vault.entities.Tag;
import com.pmfml.cognitive_vault.exceptions.ResourceNotFoundException;
import com.pmfml.cognitive_vault.repositories.NoteRepository;
import com.pmfml.cognitive_vault.repositories.RelationshipRepository;
import com.pmfml.cognitive_vault.repositories.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ElasticsearchIndexer elasticsearchIndexer;

    @Mock
    private RelationshipService relationshipService;

    @InjectMocks
    private NoteService noteService;

    private Note note;
    private UUID noteId;

    @BeforeEach
    void setUp() {
        noteId = UUID.randomUUID();
        note = Note.builder()
                .id(noteId)
                .title("Original Title")
                .content("Original Content")
                .type(NoteType.TECHNICAL_NOTE)
                .tags(Set.of(Tag.builder().name("java").build()))
                .createdAt(Instant.now())
                .lastAccessedAt(Instant.now())
                .build();
    }

    @Test
    void createNote_withValidInput_shouldReturnSavedNote() {
        NoteRequest request = new NoteRequest("New Note", "Content", NoteType.TECHNICAL_NOTE, null, Set.of("java", "spring"));

        when(tagRepository.findByName("java")).thenReturn(Optional.of(Tag.builder().name("java").build()));
        when(tagRepository.findByName("spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(embeddingModel.embed(anyString())).thenReturn(new float[384]);

        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
            Note toSave = invocation.getArgument(0);
            toSave.setId(noteId);
            return toSave;
        });

        NoteResponse response = noteService.createNote(request);

        assertNotNull(response);
        assertEquals(noteId, response.id());
        assertEquals("New Note", response.title());
        assertTrue(response.tags().contains("java"));
        assertTrue(response.tags().contains("spring"));
        verify(noteRepository, times(1)).save(any(Note.class));
        verify(relationshipService, times(1)).recalculateRelationships(any(Note.class));
        verify(elasticsearchIndexer, times(1)).indexNote(any(Note.class));
    }

    @Test
    void createNote_withInvalidTitle_shouldThrowException() {
        NoteRequest request = new NoteRequest("", "Content", NoteType.TECHNICAL_NOTE, null, null);

        assertThrows(IllegalArgumentException.class, () -> noteService.createNote(request));
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void getNoteById_existingNote_shouldUpdateAccessedTimestampAndReturnResponse() {
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant beforeAccess = Instant.now();
        NoteResponse response = noteService.getNoteById(noteId);

        assertNotNull(response);
        assertEquals(noteId, response.id());
        assertTrue(response.lastAccessedAt().isAfter(beforeAccess) || response.lastAccessedAt().equals(beforeAccess));
        verify(noteRepository, times(1)).findById(noteId);
        verify(noteRepository, times(1)).save(any(Note.class));
    }

    @Test
    void getNoteById_nonExistingNote_shouldThrowException() {
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> noteService.getNoteById(noteId));
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void getAllNotes_shouldReturnList() {
        when(noteRepository.findAll()).thenReturn(List.of(note));

        List<NoteResponse> responses = noteService.getAllNotes();

        assertEquals(1, responses.size());
        assertEquals("Original Title", responses.get(0).title());
    }

    @Test
    void updateNote_existingNote_shouldModifyFieldsAndReturnResponse() {
        NoteRequest request = new NoteRequest("Updated Title", "Updated Content", NoteType.CODE_SNIPPET, "java", Set.of("java"));

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(tagRepository.findByName("java")).thenReturn(Optional.of(Tag.builder().name("java").build()));
        when(embeddingModel.embed(anyString())).thenReturn(new float[384]);
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse response = noteService.updateNote(noteId, request);

        assertNotNull(response);
        assertEquals("Updated Title", response.title());
        assertEquals("Updated Content", response.content());
        assertEquals(NoteType.CODE_SNIPPET, response.type());
        assertEquals("java", response.language());
        verify(noteRepository, times(1)).save(any(Note.class));
        verify(relationshipService, times(1)).recalculateRelationships(any(Note.class));
        verify(elasticsearchIndexer, times(1)).indexNote(any(Note.class));
    }

    @Test
    void getRelatedNotes_existingNote_shouldReturnRelationships() {
        when(noteRepository.existsById(noteId)).thenReturn(true);
        Relationship rel = Relationship.builder()
                .id(UUID.randomUUID())
                .sourceNote(note)
                .targetNote(note)
                .similarityScore(0.95)
                .build();
        when(relationshipRepository.findBySourceNoteId(noteId)).thenReturn(List.of(rel));

        List<RelationshipResponse> responses = noteService.getRelatedNotes(noteId);

        assertEquals(1, responses.size());
        assertEquals(0.95, responses.get(0).similarityScore());
    }

    @Test
    void getRelatedNotes_nonExistingNote_shouldThrowException() {
        when(noteRepository.existsById(noteId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> noteService.getRelatedNotes(noteId));
        verify(relationshipRepository, never()).findBySourceNoteId(any(UUID.class));
    }

    @Test
    void getNotesNeedingReview_shouldReturnList() {
        when(noteRepository.findNotesNeedingReview(any(Instant.class), any(Instant.class))).thenReturn(List.of(note));

        List<NoteResponse> responses = noteService.getNotesNeedingReview();

        assertEquals(1, responses.size());
        assertEquals("Original Title", responses.get(0).title());
    }

    @Test
    void reviewNote_existingNote_shouldUpdateTimestampAndReturnResponse() {
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant beforeReview = Instant.now();
        NoteResponse response = noteService.reviewNote(noteId);

        assertNotNull(response);
        assertNotNull(response.lastReviewedAt());
        assertTrue(response.lastReviewedAt().isAfter(beforeReview) || response.lastReviewedAt().equals(beforeReview));
        verify(noteRepository, times(1)).save(any(Note.class));
        verify(elasticsearchIndexer, times(1)).indexNote(any(Note.class));
    }

    @Test
    void reviewNote_nonExistingNote_shouldThrowException() {
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> noteService.reviewNote(noteId));
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void deleteNote_existingNote_shouldInvokeDeletes() {
        when(noteRepository.existsById(noteId)).thenReturn(true);

        noteService.deleteNote(noteId);

        verify(relationshipRepository, times(1)).deleteByNoteId(noteId);
        verify(noteRepository, times(1)).deleteById(noteId);
        verify(elasticsearchIndexer, times(1)).deleteNote(noteId);
    }

    @Test
    void deleteNote_nonExistingNote_shouldThrowException() {
        when(noteRepository.existsById(noteId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> noteService.deleteNote(noteId));
        verify(relationshipRepository, never()).deleteByNoteId(any(UUID.class));
        verify(noteRepository, never()).deleteById(any(UUID.class));
    }
}
