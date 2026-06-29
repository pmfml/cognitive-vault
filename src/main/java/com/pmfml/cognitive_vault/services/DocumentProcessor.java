package com.pmfml.cognitive_vault.services;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Service responsible for extracting textual content from uploaded files (attachments)
 * to support full-text search indexing and semantic analysis.
 *
 * <p>Plain-text formats are decoded directly as UTF-8 for speed and fidelity, while
 * richer document formats (such as PDF) are delegated to Apache Tika, which detects
 * the format and extracts its textual content.
 */
@Service
public class DocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class);

    private static final String TEXT_MIME_PREFIX = "text/";
    private static final String JSON_MIME_TYPE = "application/json";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".txt", ".md", ".json");

    private final Tika tika = new Tika();

    /**
     * Extracts text content from raw file bytes based on the file content type or filename.
     * Plain-text formats (.txt, .md, .json, or {@code text/*}) are decoded as UTF-8.
     * Other formats (such as PDF) are parsed by Apache Tika.
     * Returns an empty string when the content cannot be extracted.
     *
     * @param content     the file content as a byte array
     * @param contentType the MIME type of the file
     * @param filename    the name of the file
     * @return the extracted text content, or an empty string if not extractable
     */
    public String extractText(byte[] content, String contentType, String filename) {
        if (content == null || content.length == 0) {
            return "";
        }

        log.info("Attempting text extraction for file: {} (MIME: {}, size: {} bytes)", filename, contentType, content.length);

        if (isPlainText(contentType, filename)) {
            return decodeAsUtf8(content, filename);
        }

        return extractWithTika(content, contentType, filename);
    }

    /**
     * Determines whether the file should be treated as plain text and decoded directly.
     */
    private boolean isPlainText(String contentType, String filename) {
        boolean mimeIsText = contentType != null
                && (contentType.startsWith(TEXT_MIME_PREFIX) || contentType.equals(JSON_MIME_TYPE));
        boolean extensionIsText = filename != null && hasSupportedExtension(filename);
        return mimeIsText || extensionIsText;
    }

    /**
     * Decodes raw bytes as a UTF-8 string for plain-text formats.
     */
    private String decodeAsUtf8(byte[] content, String filename) {
        try {
            String extractedText = new String(content, StandardCharsets.UTF_8);
            log.info("Successfully extracted {} characters of text from file: {}", extractedText.length(), filename);
            return extractedText;
        } catch (Exception e) {
            log.error("Failed to parse text file: {}. Error: {}", filename, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Extracts text from richer document formats (such as PDF) using Apache Tika.
     * Any parsing failure degrades gracefully to an empty string.
     */
    private String extractWithTika(byte[] content, String contentType, String filename) {
        try (InputStream stream = new ByteArrayInputStream(content)) {
            String parsed = tika.parseToString(stream);
            String result = parsed == null ? "" : parsed.strip();
            log.info("Tika extracted {} characters of text from file: {}", result.length(), filename);
            return result;
        } catch (Exception e) {
            log.warn("Tika could not extract text from file: {} (MIME: {}). Returning empty content. Error: {}",
                    filename, contentType, e.getMessage());
            return "";
        }
    }

    private boolean hasSupportedExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
    }
}
