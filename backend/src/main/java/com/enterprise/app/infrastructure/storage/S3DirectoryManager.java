package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.DirectoryManager;
import com.enterprise.app.domain.storage.FileData;
import com.enterprise.app.domain.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3DirectoryManager implements DirectoryManager {

    private final FileStorage fileStorage;

    @Override
    public void createDirectory(String directoryPath) {
        log.info("Creating directory: {}", directoryPath);

        String directoryKey = ensureDirectoryPath(directoryPath) + ".directory";

        try (InputStream emptyContent = new ByteArrayInputStream(new byte[0])) {
            fileStorage.uploadFile(emptyContent, directoryKey, "application/directory", 0L);
            log.info("Successfully created directory: {}", directoryPath);
        } catch (Exception e) {
            log.error("Failed to create directory: {}", directoryPath, e);
            throw new RuntimeException("Failed to create directory", e);
        }
    }

    @Override
    public void addFileToDirectory(String directoryPath, String fileName, InputStream content, String contentType) {
        log.info("Adding file {} to directory: {}", fileName, directoryPath);

        String fileKey = ensureDirectoryPath(directoryPath) + fileName;

        try {
            long contentLength = content.available();
            fileStorage.uploadFile(content, fileKey, contentType, contentLength);
            log.info("Successfully added file {} to directory: {}", fileName, directoryPath);
        } catch (Exception e) {
            log.error("Failed to add file {} to directory: {}", fileName, directoryPath, e);
            throw new RuntimeException("Failed to add file to directory", e);
        }
    }

    @Override
    public void addFilesToDirectory(String directoryPath, Map<String, FileData> files) {
        log.info("Adding {} files to directory: {}", files.size(), directoryPath);

        for (Map.Entry<String, FileData> entry : files.entrySet()) {
            String fileName = entry.getKey();
            FileData fileData = entry.getValue();

            addFileToDirectory(directoryPath, fileName, fileData.content(), fileData.contentType());
        }

        log.info("Successfully added all files to directory: {}", directoryPath);
    }

    @Override
    public boolean directoryExists(String directoryPath) {
        String directoryKey = ensureDirectoryPath(directoryPath) + ".directory";
        return fileStorage.fileExists(directoryKey);
    }

    @Override
    public void deleteDirectory(String directoryPath) {
        log.info("Deleting directory: {}", directoryPath);

        String prefix = ensureDirectoryPath(directoryPath);

        try {
            var files = fileStorage.listFiles(prefix);

            for (String fileKey : files) {
                fileStorage.deleteFile(fileKey);
            }

            log.info("Successfully deleted directory: {}", directoryPath);
        } catch (Exception e) {
            log.error("Failed to delete directory: {}", directoryPath, e);
            throw new RuntimeException("Failed to delete directory", e);
        }
    }

    private String ensureDirectoryPath(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return "";
        }

        String normalizedPath = directoryPath.trim().replace("\\", "/");

        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }

        return normalizedPath;
    }
}