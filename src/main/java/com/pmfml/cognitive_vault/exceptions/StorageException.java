package com.pmfml.cognitive_vault.exceptions;

/**
 * Exception thrown when an operation with the external storage provider (S3/MinIO) fails.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
