package com.pmfml.cognitive_vault.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Service responsible for extracting textual content from uploaded files (attachments)
 * to support full-text search indexing and semantic analysis.
 */
@Service
public class DocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class);

    private static final String TEXT_MIME_PREFIX = "text/";
    private static final String JSON_MIME_TYPE = "application/json";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".txt", ".md", ".json");

    /**
     * Extracts text content from raw file bytes based on the file content type or filename.
     * Currently decodes text-based formats (like .txt, .md) to UTF-8 strings.
     * Returns an empty string for non-extractable or unsupported binary file formats.
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

        boolean isTextType = (contentType != null && (contentType.startsWith(TEXT_MIME_PREFIX) || contentType.equals(JSON_MIME_TYPE)))
                || (filename != null && hasSupportedExtension(filename));

        if (isTextType) {
            try {
                String extractedText = new String(content, StandardCharsets.UTF_8);
                log.info("Successfully extracted {} characters of text from file: {}", extractedText.length(), filename);
                return extractedText;
            } catch (Exception e) {
                log.error("Failed to parse text file: {}. Error: {}", filename, e.getMessage(), e);
                return "";
            }
        }

        log.warn("Text extraction not supported for content type: {} or file: {}", contentType, filename);
        return "";
    }

    private boolean hasSupportedExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
    }
}
