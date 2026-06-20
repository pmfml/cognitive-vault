package com.pmfml.cognitive_vault.services;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentProcessorTest {

    private final DocumentProcessor processor = new DocumentProcessor();

    @Test
    void extractText_whenPlainTextBytes_returnsParsedString() {
        // Arrange
        String originalText = "Hello, this is a plain text file content with special characters: áéíóúçñ.";
        byte[] content = originalText.getBytes(StandardCharsets.UTF_8);
        String contentType = "text/plain";
        String filename = "sample.txt";

        // Act
        String result = processor.extractText(content, contentType, filename);

        // Assert
        assertEquals(originalText, result);
    }

    @Test
    void extractText_whenMarkdownFileExtension_returnsParsedString() {
        // Arrange
        String originalText = "# Heading\nThis is markdown text.";
        byte[] content = originalText.getBytes(StandardCharsets.UTF_8);
        String contentType = "application/octet-stream"; // non-standard text mime
        String filename = "notes.md";

        // Act
        String result = processor.extractText(content, contentType, filename);

        // Assert
        assertEquals(originalText, result);
    }

    @Test
    void extractText_whenUnsupportedContentType_returnsEmptyString() {
        // Arrange
        byte[] content = new byte[]{1, 2, 3, 4, 5};
        String contentType = "application/pdf";
        String filename = "document.pdf";

        // Act
        String result = processor.extractText(content, contentType, filename);

        // Assert
        assertEquals("", result);
    }

    @Test
    void extractText_whenNullOrEmptyContent_returnsEmptyString() {
        // Act & Assert
        assertEquals("", processor.extractText(null, "text/plain", "file.txt"));
        assertEquals("", processor.extractText(new byte[0], "text/plain", "file.txt"));
    }
}
