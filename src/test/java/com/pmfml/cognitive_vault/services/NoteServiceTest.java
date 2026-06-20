package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.dtos.NoteRequest;
import com.pmfml.cognitive_vault.dtos.NoteResponse;
import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.entities.NoteType;
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
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse response = noteService.updateNote(noteId, request);

        assertNotNull(response);
        assertEquals("Updated Title", response.title());
        assertEquals("Updated Content", response.content());
        assertEquals(NoteType.CODE_SNIPPET, response.type());
        assertEquals("java", response.language());
        verify(noteRepository, times(1)).save(any(Note.class));
    }

    @Test
    void deleteNote_existingNote_shouldInvokeDeletes() {
        when(noteRepository.existsById(noteId)).thenReturn(true);

        noteService.deleteNote(noteId);

        verify(relationshipRepository, times(1)).deleteByNoteId(noteId);
        verify(noteRepository, times(1)).deleteById(noteId);
    }

    @Test
    void deleteNote_nonExistingNote_shouldThrowException() {
        when(noteRepository.existsById(noteId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> noteService.deleteNote(noteId));
        verify(relationshipRepository, never()).deleteByNoteId(any(UUID.class));
        verify(noteRepository, never()).deleteById(any(UUID.class));
    }
}
