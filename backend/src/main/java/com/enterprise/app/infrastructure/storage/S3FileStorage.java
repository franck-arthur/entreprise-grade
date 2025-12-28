package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.FileMetadata;
import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.infrastructure.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Config.S3Properties s3Properties;

    @Override
    public String uploadFile(InputStream inputStream, String key, String contentType, long contentLength) {
        try {
            log.info("Uploading file to S3 with key: {}", key);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .metadata(Map.of("uploaded-at", LocalDateTime.now().toString()))
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

            log.info("Successfully uploaded file to S3 with key: {}", key);
            return key;

        } catch (S3Exception e) {
            log.error("S3 error while uploading file with key: {}", key, e);
            throw new RuntimeException("S3 error during file upload", e);
        }
    }

    @Override
    public InputStream downloadFile(String key) {
        try {
            log.info("Downloading file from S3 with key: {}", key);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            return s3Client.getObject(request);

        } catch (S3Exception e) {
            log.error("Failed to download file with key: {}", key, e);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            log.info("Deleting file from S3 with key: {}", key);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);

            log.info("Successfully deleted file from S3 with key: {}", key);

        } catch (S3Exception e) {
            log.error("Failed to delete file with key: {}", key, e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    @Override
    public List<String> listFiles(String prefix) {
        try {
            log.info("Listing files in S3 with prefix: {}", prefix);

            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(s3Properties.getBucketName());

            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());

        } catch (S3Exception e) {
            log.error("Failed to list files with prefix: {}", prefix, e);
            throw new RuntimeException("Failed to list files from S3", e);
        }
    }

    @Override
    public boolean fileExists(String key) {
        try {
            getFileMetadata(key);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public FileMetadata getFileMetadata(String key) {
        try {
            log.info("Getting metadata for file with key: {}", key);

            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);

            return new FileMetadata(
                    key,
                    response.contentType(),
                    response.contentLength(),
                    LocalDateTime.ofInstant(response.lastModified(), ZoneOffset.UTC),
                    response.metadata()
            );

        } catch (S3Exception e) {
            log.error("Failed to get metadata for file with key: {}", key, e);
            throw new RuntimeException("Failed to get file metadata from S3", e);
        }
    }
}