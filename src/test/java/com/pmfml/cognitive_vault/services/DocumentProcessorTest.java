package com.pmfml.cognitive_vault.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void extractText_whenCorruptDocument_returnsEmptyString() {
        // Arrange: bytes that claim to be a PDF but are not parseable.
        byte[] content = new byte[]{1, 2, 3, 4, 5};
        String contentType = "application/pdf";
        String filename = "document.pdf";

        // Act
        String result = processor.extractText(content, contentType, filename);

        // Assert: extraction degrades gracefully to an empty string.
        assertEquals("", result);
    }

    @Test
    void extractText_whenValidPdf_extractsTextContent() throws Exception {
        // Arrange: build a real PDF in memory containing a known sentence.
        String expectedSentence = "Cognitive Vault PDF extraction works";
        byte[] pdfBytes = buildPdf(expectedSentence);

        // Act
        String result = processor.extractText(pdfBytes, "application/pdf", "document.pdf");

        // Assert: Tika extracted the embedded text.
        assertTrue(result.contains(expectedSentence),
                "Expected extracted PDF text to contain: " + expectedSentence + " but was: " + result);
    }

    private byte[] buildPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void extractText_whenNullOrEmptyContent_returnsEmptyString() {
        // Act & Assert
        assertEquals("", processor.extractText(null, "text/plain", "file.txt"));
        assertEquals("", processor.extractText(new byte[0], "text/plain", "file.txt"));
    }
}
