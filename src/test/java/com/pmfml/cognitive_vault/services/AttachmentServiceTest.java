package com.pmfml.cognitive_vault.services;

import com.pmfml.cognitive_vault.dtos.AttachmentResponse;
import com.pmfml.cognitive_vault.entities.Attachment;
import com.pmfml.cognitive_vault.entities.Note;
import com.pmfml.cognitive_vault.events.NoteIndexRequestedEvent;
import com.pmfml.cognitive_vault.exceptions.ResourceNotFoundException;
import com.pmfml.cognitive_vault.repositories.AttachmentRepository;
import com.pmfml.cognitive_vault.repositories.NoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private AttachmentStorageService storageService;

    @Mock
    private DocumentProcessor documentProcessor;

    @Mock
    private ElasticsearchIndexer elasticsearchIndexer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AttachmentService attachmentService;

    @Test
    void uploadAttachment_whenSuccessful_uploadsAndPersists() {
        // Arrange
        UUID noteId = UUID.randomUUID();
        Note note = Note.builder().id(noteId).title("My Note").build();
        byte[] content = "Hello World".getBytes();
        String filename = "hello.txt";
        String contentType = "text/plain";
        String extractedText = "Hello World";

        Attachment savedAttachment = Attachment.builder()
                .id(UUID.randomUUID())
                .fileName(filename)
                .s3Key("some-key")
                .contentType(contentType)
                .fileSize((long) content.length)
                .extractedText(extractedText)
                .note(note)
                .createdAt(Instant.now())
                .build();

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(documentProcessor.extractText(content, contentType, filename)).thenReturn(extractedText);
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(savedAttachment);

        // Act
        AttachmentResponse response = attachmentService.uploadAttachment(noteId, filename, contentType, content);

        // Assert
        assertNotNull(response);
        assertEquals(filename, response.fileName());
        assertEquals(noteId, response.noteId());
        verify(storageService, times(1)).uploadFile(anyString(), eq(content), eq(contentType));
        verify(attachmentRepository, times(1)).save(any(Attachment.class));
        verify(eventPublisher, times(1)).publishEvent(any(NoteIndexRequestedEvent.class));
    }

    @Test
    void uploadAttachment_whenNoteNotFound_throwsResourceNotFoundException() {
        // Arrange
        UUID noteId = UUID.randomUUID();
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> attachmentService.uploadAttachment(noteId, "file.txt", "text/plain", new byte[]{1}));
        verify(storageService, never()).uploadFile(anyString(), any(), anyString());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void getAttachmentContent_whenSuccessful_returnsBytes() {
        // Arrange
        UUID attachmentId = UUID.randomUUID();
        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .s3Key("note-id/uuid_hello.txt")
                .build();
        byte[] expectedContent = "bytes".getBytes();

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(storageService.downloadFile("note-id/uuid_hello.txt")).thenReturn(expectedContent);

        // Act
        byte[] result = attachmentService.getAttachmentContent(attachmentId);

        // Assert
        assertArrayEquals(expectedContent, result);
        verify(storageService, times(1)).downloadFile("note-id/uuid_hello.txt");
    }

    @Test
    void deleteAttachment_whenSuccessful_deletesFromS3AndDatabase() {
        // Arrange
        UUID attachmentId = UUID.randomUUID();
        Note note = Note.builder().id(UUID.randomUUID()).title("Title").build();
        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .s3Key("key")
                .note(note)
                .build();

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        // Act
        attachmentService.deleteAttachment(attachmentId);

        // Assert
        verify(storageService, times(1)).deleteFile("key");
        verify(attachmentRepository, times(1)).delete(attachment);
        verify(eventPublisher, times(1)).publishEvent(any(NoteIndexRequestedEvent.class));
    }

    @Test
    void uploadAttachment_whenPersistenceFails_compensatesByDeletingS3Object() {
        // Arrange
        UUID noteId = UUID.randomUUID();
        Note note = Note.builder().id(noteId).title("My Note").build();
        byte[] content = "Hello World".getBytes();
        String filename = "hello.txt";
        String contentType = "text/plain";

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(documentProcessor.extractText(content, contentType, filename)).thenReturn("Hello World");
        when(attachmentRepository.save(any(Attachment.class))).thenThrow(new RuntimeException("Database unavailable"));

        // Simulate an active transaction synchronization context.
        TransactionSynchronizationManager.initSynchronization();
        try {
            // Act: persistence fails after the S3 upload already happened.
            assertThrows(RuntimeException.class,
                    () -> attachmentService.uploadAttachment(noteId, filename, contentType, content));

            // Capture the key that was uploaded to storage.
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(storageService, times(1)).uploadFile(keyCaptor.capture(), eq(content), eq(contentType));

            // Simulate the transaction manager completing the (rolled back) transaction.
            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            // Assert: the orphaned object was compensated (deleted) using the same key.
            verify(storageService, times(1)).deleteFile(keyCaptor.getValue());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
