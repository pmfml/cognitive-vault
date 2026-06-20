package com.pmfml.cognitive_vault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Component that automatically creates the S3 bucket on application startup
 * if it does not already exist in the target S3/MinIO environment.
 */
@Component
public class S3BucketInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(S3BucketInitializer.class);

    private final S3Client s3Client;
    private final String bucketName;

    /**
     * Constructor injection for S3Client and the configured bucket name.
     *
     * @param s3Client   the Amazon S3 client
     * @param bucketName the target bucket name
     */
    public S3BucketInitializer(S3Client s3Client, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public void run(String... args) {
        log.info("Checking S3/MinIO bucket status for: {}", bucketName);
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("S3/MinIO bucket '{}' already exists.", bucketName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.info("S3/MinIO bucket '{}' does not exist. Creating bucket...", bucketName);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("S3/MinIO bucket '{}' created successfully.", bucketName);
            } else {
                log.error("Failed to check or create S3/MinIO bucket '{}'. Error: {}", bucketName, e.getMessage(), e);
                throw e;
            }
        }
    }
}
