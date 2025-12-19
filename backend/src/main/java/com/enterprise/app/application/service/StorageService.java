package com.enterprise.app.application.service;

import com.enterprise.app.infrastructure.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling file storage operations using S3-compatible storage (Garage).
 * Provides upload, download, delete, and list operations for files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;
    private final S3Config.S3Properties s3Properties;

    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Upload a file to S3 storage.
     *
     * @param file The file to upload
     * @param folder Optional folder path (e.g., "documents", "images")
     * @return The S3 key of the uploaded file
     * @throws RuntimeException if upload fails
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String key = generateFileKey(file.getOriginalFilename(), folder);

            log.info("Uploading file {} to S3 with key: {}", file.getOriginalFilename(), key);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(java.util.Map.of(
                        "original-filename", file.getOriginalFilename(),
                        "uploaded-at", LocalDateTime.now().toString()
                    ))
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Successfully uploaded file to S3 with key: {}", key);
            return key;

        } catch (IOException e) {
            log.error("Failed to upload file {} to S3", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file to S3", e);
        } catch (S3Exception e) {
            log.error("S3 error while uploading file {}", file.getOriginalFilename(), e);
            throw new RuntimeException("S3 error during file upload", e);
        }
    }

    /**
     * Upload a file to S3 storage without folder organization.
     *
     * @param file The file to upload
     * @return The S3 key of the uploaded file
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    /**
     * Download a file from S3 storage.
     *
     * @param key The S3 key of the file
     * @return InputStream of the file content
     * @throws RuntimeException if download fails
     */
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

    /**
     * Delete a file from S3 storage.
     *
     * @param key The S3 key of the file to delete
     * @throws RuntimeException if deletion fails
     */
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

    /**
     * List files in a specific folder.
     *
     * @param folder The folder path to list files from
     * @return List of file keys in the folder
     */
    public List<String> listFiles(String folder) {
        try {
            String prefix = folder != null ? folder + "/" : "";

            log.info("Listing files in S3 folder: {}", prefix);

            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(s3Properties.getBucketName())
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());

        } catch (S3Exception e) {
            log.error("Failed to list files in folder: {}", folder, e);
            throw new RuntimeException("Failed to list files from S3", e);
        }
    }

    /**
     * List all files in the bucket.
     *
     * @return List of all file keys
     */
    public List<String> listAllFiles() {
        return listFiles(null);
    }

    /**
     * Get file metadata.
     *
     * @param key The S3 key of the file
     * @return File metadata
     */
    public HeadObjectResponse getFileMetadata(String key) {
        try {
            log.info("Getting metadata for file with key: {}", key);

            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();

            return s3Client.headObject(request);

        } catch (S3Exception e) {
            log.error("Failed to get metadata for file with key: {}", key, e);
            throw new RuntimeException("Failed to get file metadata from S3", e);
        }
    }

    /**
     * Check if a file exists in S3.
     *
     * @param key The S3 key to check
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String key) {
        try {
            getFileMetadata(key);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Generate a unique file key with optional folder organization.
     *
     * @param originalFilename The original filename
     * @param folder Optional folder path
     * @return Generated S3 key
     */
    private String generateFileKey(String originalFilename, String folder) {
        String timestamp = LocalDateTime.now().format(FOLDER_FORMAT);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String filename = cleanFilename(originalFilename);

        StringBuilder keyBuilder = new StringBuilder();

        if (folder != null && !folder.trim().isEmpty()) {
            keyBuilder.append(folder).append("/");
        }

        keyBuilder.append(timestamp)
                .append("/")
                .append(uniqueId)
                .append("-")
                .append(filename);

        return keyBuilder.toString();
    }

    /**
     * Clean filename to be S3-compatible.
     *
     * @param filename The original filename
     * @return Cleaned filename
     */
    private String cleanFilename(String filename) {
        if (filename == null) {
            return "unknown-file";
        }

        // Remove or replace characters that might cause issues in S3
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}