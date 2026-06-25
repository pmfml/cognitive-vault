package com.pmfml.cognitive_vault.repositories;

import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.entities.NoteType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
class NoteRepositoryTest {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Test
    void findSimilarNotes_shouldReturnNotesOrderedByDistance() {
        relationshipRepository.deleteAll();
        attachmentRepository.deleteAll();
        noteRepository.deleteAll();
        // Arrange
        // Create three notes with distinct vectors of 384 dimensions.
        float[] vectorA = new float[384];
        vectorA[0] = 1.0f; // points strongly in dimension 0

        float[] vectorB = new float[384];
        vectorB[1] = 1.0f; // points strongly in dimension 1

        float[] vectorC = new float[384];
        vectorC[2] = 1.0f; // points strongly in dimension 2

        Note noteA = Note.builder()
                .title("Java Note")
                .content("Content about Java and Spring Boot")
                .type(NoteType.TECHNICAL_NOTE)
                .embedding(vectorA)
                .createdAt(Instant.now())
                .build();

        Note noteB = Note.builder()
                .title("Python Note")
                .content("Content about Python and Django")
                .type(NoteType.TECHNICAL_NOTE)
                .embedding(vectorB)
                .createdAt(Instant.now())
                .build();

        Note noteC = Note.builder()
                .title("JavaScript Note")
                .content("Content about JavaScript and React")
                .type(NoteType.TECHNICAL_NOTE)
                .embedding(vectorC)
                .createdAt(Instant.now())
                .build();

        noteRepository.save(noteA);
        noteRepository.save(noteB);
        noteRepository.save(noteC);

        // We query with a vector that matches vectorA perfectly
        String queryVector = toVectorString(vectorA);

        // Act
        List<Note> results = noteRepository.findSimilarNotes(queryVector, 3);

        // Assert
        assertFalse(results.isEmpty());
        // noteA should be closest (distance = 0.0)
        assertEquals("Java Note", results.get(0).getTitle());
    }

    @Test
    void findSimilarNotesExcludingSelf_shouldRecommendOtherNotes() {
        relationshipRepository.deleteAll();
        attachmentRepository.deleteAll();
        noteRepository.deleteAll();
        // Arrange
        float[] vectorA = new float[384];
        vectorA[0] = 1.0f;

        float[] vectorB = new float[384];
        vectorB[1] = 1.0f;

        Note noteA = Note.builder()
                .title("Java Note")
                .content("Content about Java")
                .type(NoteType.TECHNICAL_NOTE)
                .embedding(vectorA)
                .createdAt(Instant.now())
                .build();

        Note noteB = Note.builder()
                .title("Python Note")
                .content("Content about Python")
                .type(NoteType.TECHNICAL_NOTE)
                .embedding(vectorB)
                .createdAt(Instant.now())
                .build();

        Note savedA = noteRepository.save(noteA);
        noteRepository.save(noteB);

        String queryVector = toVectorString(vectorA);

        // Act
        // Query similar notes to vectorA, but exclude noteA itself
        List<Note> results = noteRepository.findSimilarNotesExcludingSelf(queryVector, savedA.getId(), 5);

        // Assert
        assertFalse(results.isEmpty());
        // Since noteA is excluded, noteB must be the first result
        assertEquals("Python Note", results.get(0).getTitle());
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
