package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.domain.storage.ZipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZipServiceImpl implements ZipService {

    private final FileStorage fileStorage;

    @Override
    public InputStream createZipFromDirectory(String directoryPath) {
        log.info("Creating ZIP from directory: {}", directoryPath);

        try {
            String prefix = ensureDirectoryPath(directoryPath);
            List<String> files = fileStorage.listFiles(prefix);

            if (files.isEmpty()) {
                log.warn("No files found in directory: {}", directoryPath);
                return new ByteArrayInputStream(new byte[0]);
            }

            ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();

            try (ZipOutputStream zipOut = new ZipOutputStream(zipBuffer)) {
                for (String fileKey : files) {
                    if (fileKey.endsWith(".directory")) {
                        continue;
                    }

                    String entryName = fileKey.substring(prefix.length());

                    log.debug("Adding file to ZIP: {}", entryName);

                    ZipEntry entry = new ZipEntry(entryName);
                    zipOut.putNextEntry(entry);

                    try (InputStream fileContent = fileStorage.downloadFile(fileKey)) {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = fileContent.read(buffer)) > 0) {
                            zipOut.write(buffer, 0, length);
                        }
                    }

                    zipOut.closeEntry();
                }
            }

            log.info("Successfully created ZIP from directory: {}", directoryPath);
            return new ByteArrayInputStream(zipBuffer.toByteArray());

        } catch (Exception e) {
            log.error("Failed to create ZIP from directory: {}", directoryPath, e);
            throw new RuntimeException("Failed to create ZIP archive", e);
        }
    }

    @Override
    public String uploadZippedDirectory(String directoryPath, String zipFileName) {
        log.info("Creating and uploading ZIP for directory: {}", directoryPath);

        try (InputStream zipContent = createZipFromDirectory(directoryPath)) {
            String zipKey = generateZipKey(zipFileName);

            long contentLength = zipContent.available();
            String uploadedKey = fileStorage.uploadFile(zipContent, zipKey, "application/zip", contentLength);

            log.info("Successfully uploaded ZIP file with key: {}", uploadedKey);
            return uploadedKey;

        } catch (Exception e) {
            log.error("Failed to upload zipped directory: {}", directoryPath, e);
            throw new RuntimeException("Failed to upload zipped directory", e);
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

    private String generateZipKey(String zipFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        String cleanFileName = zipFileName;
        if (!cleanFileName.toLowerCase().endsWith(".zip")) {
            cleanFileName += ".zip";
        }

        cleanFileName = cleanFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

        return String.format("archives/%s/%s-%s", timestamp, uniqueId, cleanFileName);
    }
}