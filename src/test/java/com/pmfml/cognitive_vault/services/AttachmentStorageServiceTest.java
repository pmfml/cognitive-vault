package com.pmfml.cognitive_vault.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentStorageServiceTest {

    @Mock
    private S3Client s3Client;

    private AttachmentStorageService storageService;
    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        storageService = new AttachmentStorageService(s3Client, bucketName);
    }

    @Test
    void uploadFile_whenSuccessful_callsPutObject() {
        // Arrange
        String key = "test-key.txt";
        byte[] content = "Hello World".getBytes();
        String contentType = "text/plain";

        // Act
        storageService.uploadFile(key, content, contentType);

        // Assert
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_whenS3ExceptionOccurs_throwsRuntimeException() {
        // Arrange
        String key = "test-key.txt";
        byte[] content = "Hello World".getBytes();
        String contentType = "text/plain";
        S3Exception s3Exception = (S3Exception) S3Exception.builder().message("S3 error").build();
        doThrow(s3Exception).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> storageService.uploadFile(key, content, contentType));
        assertTrue(exception.getMessage().contains("Failed to upload file"));
    }

    @Test
    void downloadFile_whenSuccessful_returnsFileBytes() {
        // Arrange
        String key = "test-key.txt";
        byte[] expectedBytes = "Hello World".getBytes();
        @SuppressWarnings("unchecked")
        ResponseBytes<GetObjectResponse> responseBytes = mock(ResponseBytes.class);
        when(responseBytes.asByteArray()).thenReturn(expectedBytes);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        // Act
        byte[] actualBytes = storageService.downloadFile(key);

        // Assert
        assertArrayEquals(expectedBytes, actualBytes);
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void downloadFile_whenS3ExceptionOccurs_throwsRuntimeException() {
        // Arrange
        String key = "test-key.txt";
        S3Exception s3Exception = (S3Exception) S3Exception.builder().message("S3 error").build();
        doThrow(s3Exception).when(s3Client).getObjectAsBytes(any(GetObjectRequest.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> storageService.downloadFile(key));
        assertTrue(exception.getMessage().contains("Failed to retrieve file"));
    }

    @Test
    void deleteFile_whenSuccessful_callsDeleteObject() {
        // Arrange
        String key = "test-key.txt";

        // Act
        storageService.deleteFile(key);

        // Assert
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFile_whenS3ExceptionOccurs_throwsRuntimeException() {
        // Arrange
        String key = "test-key.txt";
        S3Exception s3Exception = (S3Exception) S3Exception.builder().message("S3 error").build();
        doThrow(s3Exception).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> storageService.deleteFile(key));
        assertTrue(exception.getMessage().contains("Failed to delete file"));
    }
}
