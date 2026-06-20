package com.pmfml.cognitive_vault.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3BucketInitializerTest {

    @Mock
    private S3Client s3Client;

    private S3BucketInitializer initializer;
    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        initializer = new S3BucketInitializer(s3Client, bucketName);
    }

    @Test
    void run_whenBucketExists_doesNotCreateBucket() {
        // Arrange
        // headBucket succeeds by default when not throwing exceptions

        // Act
        initializer.run();

        // Assert
        verify(s3Client, times(1)).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void run_whenBucketDoesNotExist_createsBucket() {
        // Arrange
        S3Exception noSuchBucketException = NoSuchBucketException.builder()
                .message("The specified bucket does not exist")
                .statusCode(404)
                .build();
        doThrow(noSuchBucketException).when(s3Client).headBucket(any(HeadBucketRequest.class));

        // Act
        initializer.run();

        // Assert
        verify(s3Client, times(1)).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, times(1)).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void run_whenGenericS3ExceptionOccurs_rethrowsException() {
        // Arrange
        S3Exception genericException = (S3Exception) S3Exception.builder()
                .message("Internal service error")
                .statusCode(500)
                .build();
        doThrow(genericException).when(s3Client).headBucket(any(HeadBucketRequest.class));

        // Act & Assert
        assertThrows(S3Exception.class, () -> initializer.run());
        verify(s3Client, times(1)).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }
}
