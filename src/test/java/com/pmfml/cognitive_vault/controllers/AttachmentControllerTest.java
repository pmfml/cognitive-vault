package com.pmfml.cognitive_vault.controllers;

import com.pmfml.cognitive_vault.dtos.AttachmentResponse;
import com.pmfml.cognitive_vault.exceptions.ResourceNotFoundException;
import com.pmfml.cognitive_vault.services.AttachmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttachmentController.class)
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    @Test
    void uploadAttachment_whenValidRequest_returnsCreated() throws Exception {
        // Arrange
        UUID noteId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "Hello World".getBytes());

        AttachmentResponse mockResponse = new AttachmentResponse(
                attachmentId,
                "hello.txt",
                "key",
                "text/plain",
                11L,
                "Hello World",
                Instant.now(),
                noteId
        );

        when(attachmentService.uploadAttachment(eq(noteId), eq("hello.txt"), eq("text/plain"), any(byte[].class)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/notes/{noteId}/attachments", noteId)
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(attachmentId.toString()))
                .andExpect(jsonPath("$.fileName").value("hello.txt"))
                .andExpect(jsonPath("$.extractedText").value("Hello World"));

        verify(attachmentService, times(1))
                .uploadAttachment(eq(noteId), eq("hello.txt"), eq("text/plain"), any(byte[].class));
    }

    @Test
    void getAttachmentMetadata_whenExists_returnsMetadata() throws Exception {
        // Arrange
        UUID attachmentId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        AttachmentResponse mockResponse = new AttachmentResponse(
                attachmentId,
                "hello.txt",
                "key",
                "text/plain",
                11L,
                "Hello World",
                Instant.now(),
                noteId
        );

        when(attachmentService.getAttachmentMetadata(attachmentId)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/attachments/{attachmentId}", attachmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attachmentId.toString()))
                .andExpect(jsonPath("$.fileName").value("hello.txt"));
    }

    @Test
    void getAttachmentMetadata_whenNotFound_returnsNotFound() throws Exception {
        // Arrange
        UUID attachmentId = UUID.randomUUID();
        when(attachmentService.getAttachmentMetadata(attachmentId))
                .thenThrow(new ResourceNotFoundException("Attachment not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/attachments/{attachmentId}", attachmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadAttachment_whenExists_returnsBytesAndHeaders() throws Exception {
        // Arrange
        UUID attachmentId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        AttachmentResponse mockResponse = new AttachmentResponse(
                attachmentId,
                "hello.txt",
                "key",
                "text/plain",
                11L,
                "Hello World",
                Instant.now(),
                noteId
        );
        byte[] content = "Hello World".getBytes();

        when(attachmentService.getAttachmentMetadata(attachmentId)).thenReturn(mockResponse);
        when(attachmentService.getAttachmentContent(attachmentId)).thenReturn(content);

        // Act & Assert
        mockMvc.perform(get("/api/v1/attachments/{attachmentId}/download", attachmentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"hello.txt\""))
                .andExpect(content().bytes(content));
    }

    @Test
    void deleteAttachment_whenSuccessful_returnsNoContent() throws Exception {
        // Arrange
        UUID attachmentId = UUID.randomUUID();
        doNothing().when(attachmentService).deleteAttachment(attachmentId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/attachments/{attachmentId}", attachmentId))
                .andExpect(status().isNoContent());

        verify(attachmentService, times(1)).deleteAttachment(attachmentId);
    }
}
