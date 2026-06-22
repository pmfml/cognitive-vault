package com.pmfml.cognitive_vault.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import com.pmfml.cognitive_vault.exceptions.StorageException;

/**
 * Storage service responsible for direct raw file operations in Amazon S3/MinIO
 * using the configured S3Client.
 */
@Service
public class AttachmentStorageService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentStorageService.class);

    private final S3Client s3Client;
    private final String bucketName;

    /**
     * Constructor injection for the S3 client and configuration properties.
     *
     * @param s3Client   the S3 client
     * @param bucketName the name of the target bucket
     */
    public AttachmentStorageService(S3Client s3Client, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Uploads the raw byte contents of a file to the S3 bucket.
     *
     * @param key         the unique identifier/path for the file in the bucket
     * @param content     the byte content of the file
     * @param contentType the MIME content type of the file
     */
    public void uploadFile(String key, byte[] content, String contentType) {
        log.info("Initiating upload to S3 key: {} (size: {} bytes, type: {})", key, content.length, contentType);
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(content));
            log.info("File upload completed for key: {}", key);
        } catch (S3Exception e) {
            log.error("S3 upload failed for key: {}. Error: {}", key, e.getMessage(), e);
            throw new StorageException("Failed to upload file to storage provider", e);
        }
    }

    /**
     * Downloads a file from the S3 bucket.
     *
     * @param key the identifier/path of the file to retrieve
     * @return the byte content of the downloaded file
     */
    public byte[] downloadFile(String key) {
        log.info("Initiating download from S3 key: {}", key);
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getRequest);
            return responseBytes.asByteArray();
        } catch (S3Exception e) {
            log.error("S3 download failed for key: {}. Error: {}", key, e.getMessage(), e);
            throw new StorageException("Failed to retrieve file from storage provider", e);
        }
    }

    /**
     * Deletes a file from the S3 bucket.
     *
     * @param key the identifier/path of the file to remove
     */
    public void deleteFile(String key) {
        log.info("Initiating deletion of S3 key: {}", key);
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("File deletion completed for key: {}", key);
        } catch (S3Exception e) {
            log.error("S3 deletion failed for key: {}. Error: {}", key, e.getMessage(), e);
            throw new StorageException("Failed to delete file from storage provider", e);
        }
    }
}
