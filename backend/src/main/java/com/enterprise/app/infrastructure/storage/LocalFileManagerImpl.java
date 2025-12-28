package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.LocalFileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@Slf4j
public class LocalFileManagerImpl implements LocalFileManager {

    private final Path tempWorkingDirectory;
    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public LocalFileManagerImpl(@Value("${app.storage.temp.directory:/tmp/enterprise-app}") String tempDirectory) {
        this.tempWorkingDirectory = Paths.get(tempDirectory);
        initializeTempDirectory();
    }

    private void initializeTempDirectory() {
        try {
            if (!Files.exists(tempWorkingDirectory)) {
                Files.createDirectories(tempWorkingDirectory);
                log.info("Created temp working directory: {}", tempWorkingDirectory);
            }
        } catch (IOException e) {
            log.error("Failed to initialize temp working directory: {}", tempWorkingDirectory, e);
            throw new RuntimeException("Failed to initialize temp working directory", e);
        }
    }

    @Override
    public Path createDirectoryWithEmptyFiles(String directoryPath, Set<String> fileNames) {
        log.info("Creating directory with {} empty files: {}", fileNames.size(), directoryPath);

        try {
            Path fullPath = resolveDirectoryPath(directoryPath);

            // Créer le répertoire
            Files.createDirectories(fullPath);
            log.debug("Created directory: {}", fullPath);

            // Créer les fichiers vides
            for (String fileName : fileNames) {
                Path filePath = fullPath.resolve(cleanFileName(fileName));
                Files.createFile(filePath);
                log.debug("Created empty file: {}", filePath);
            }

            log.info("Successfully created directory with {} empty files: {}", fileNames.size(), fullPath);
            return fullPath;

        } catch (IOException e) {
            log.error("Failed to create directory with empty files: {}", directoryPath, e);
            throw new RuntimeException("Failed to create directory with empty files", e);
        }
    }

    @Override
    public Path createEmptyFile(String directoryPath, String fileName) {
        log.info("Creating empty file {} in directory: {}", fileName, directoryPath);

        try {
            Path dirPath = resolveDirectoryPath(directoryPath);
            Path filePath = dirPath.resolve(cleanFileName(fileName));

            // Créer le répertoire si nécessaire
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Files.createFile(filePath);
            log.info("Successfully created empty file: {}", filePath);
            return filePath;

        } catch (IOException e) {
            log.error("Failed to create empty file {} in directory: {}", fileName, directoryPath, e);
            throw new RuntimeException("Failed to create empty file", e);
        }
    }

    @Override
    public boolean directoryExists(String directoryPath) {
        Path path = resolveDirectoryPath(directoryPath);
        return Files.exists(path) && Files.isDirectory(path);
    }

    @Override
    public boolean fileExists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    @Override
    public List<Path> listFiles(String directoryPath) {
        log.debug("Listing files in directory: {}", directoryPath);

        try {
            Path dirPath = resolveDirectoryPath(directoryPath);

            if (!Files.exists(dirPath)) {
                log.warn("Directory does not exist: {}", dirPath);
                return List.of();
            }

            try (Stream<Path> stream = Files.list(dirPath)) {
                return stream
                    .filter(Files::isRegularFile)
                    .toList();
            }

        } catch (IOException e) {
            log.error("Failed to list files in directory: {}", directoryPath, e);
            throw new RuntimeException("Failed to list files", e);
        }
    }

    @Override
    public void deleteDirectory(String directoryPath) {
        log.info("Deleting directory: {}", directoryPath);

        try {
            Path dirPath = resolveDirectoryPath(directoryPath);

            if (!Files.exists(dirPath)) {
                log.warn("Directory does not exist, nothing to delete: {}", dirPath);
                return;
            }

            // Suppression récursive
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

            log.info("Successfully deleted directory: {}", dirPath);

        } catch (IOException e) {
            log.error("Failed to delete directory: {}", directoryPath, e);
            throw new RuntimeException("Failed to delete directory", e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        log.info("Deleting file: {}", filePath);

        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                log.warn("File does not exist, nothing to delete: {}", path);
                return;
            }

            Files.delete(path);
            log.info("Successfully deleted file: {}", path);

        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Override
    public long getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("File does not exist: " + filePath);
            }
            return Files.size(path);
        } catch (IOException e) {
            log.error("Failed to get file size: {}", filePath, e);
            throw new RuntimeException("Failed to get file size", e);
        }
    }

    @Override
    public boolean isFileEmpty(String filePath) {
        return getFileSize(filePath) == 0;
    }

    @Override
    public Path getTempWorkingDirectory() {
        return tempWorkingDirectory;
    }

    private Path resolveDirectoryPath(String directoryPath) {
        if (Paths.get(directoryPath).isAbsolute()) {
            return Paths.get(directoryPath);
        }

        // Si le chemin n'est pas absolu, le résoudre relativement au répertoire de travail temporaire
        return tempWorkingDirectory.resolve(directoryPath);
    }

    private String cleanFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "unknown-file-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // Nettoyer le nom de fichier en gardant seulement les caractères sûrs
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}