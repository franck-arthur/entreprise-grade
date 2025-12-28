package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.testing.config.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class LocalFileManagerImplTest extends BaseUnitTest {

    @TempDir
    Path tempDir;

    private LocalFileManagerImpl localFileManager;

    @Override
    protected Object[] getAllMocks() {
        return new Object[0]; // Pas de mocks dans ce test
    }

    @BeforeEach
    void setUp() {
        localFileManager = new LocalFileManagerImpl(tempDir.toString());
    }

    @Test
    void shouldCreateDirectoryWithEmptyFiles() throws IOException {
        // Given
        String directoryName = "test-project";
        Set<String> fileNames = Set.of("file1.txt", "file2.csv", "file3.json");

        // When
        Path createdDirectory = localFileManager.createDirectoryWithEmptyFiles(directoryName, fileNames);

        // Then
        assertThat(createdDirectory).exists().isDirectory();
        assertThat(createdDirectory.getParent()).isEqualTo(tempDir);

        for (String fileName : fileNames) {
            Path filePath = createdDirectory.resolve(fileName);
            assertThat(filePath).exists().isRegularFile();
            assertThat(Files.size(filePath)).isZero();
        }
    }

    @Test
    void shouldCreateEmptyFileInExistingDirectory() throws IOException {
        // Given
        String directoryName = "existing-dir";
        Files.createDirectories(tempDir.resolve(directoryName));

        // When
        Path createdFile = localFileManager.createEmptyFile(directoryName, "new-file.txt");

        // Then
        assertThat(createdFile).exists().isRegularFile();
        assertThat(Files.size(createdFile)).isZero();
        assertThat(createdFile.getParent().getFileName().toString()).isEqualTo(directoryName);
    }

    @Test
    void shouldCreateEmptyFileAndDirectoryIfNotExists() {
        // Given
        String directoryName = "non-existing-dir";
        String fileName = "test-file.txt";

        // When
        Path createdFile = localFileManager.createEmptyFile(directoryName, fileName);

        // Then
        assertThat(createdFile).exists().isRegularFile();
        assertThat(createdFile.getParent()).exists().isDirectory();
        assertThat(localFileManager.isFileEmpty(createdFile.toString())).isTrue();
    }

    @Test
    void shouldDetectDirectoryExists() throws IOException {
        // Given
        String directoryName = "test-directory";
        Files.createDirectories(tempDir.resolve(directoryName));

        // When & Then
        assertThat(localFileManager.directoryExists(directoryName)).isTrue();
        assertThat(localFileManager.directoryExists("non-existing-dir")).isFalse();
    }

    @Test
    void shouldDetectFileExists() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test-file.txt");
        Files.createFile(testFile);

        // When & Then
        assertThat(localFileManager.fileExists(testFile.toString())).isTrue();
        assertThat(localFileManager.fileExists("/non/existing/file.txt")).isFalse();
    }

    @Test
    void shouldListFilesInDirectory() throws IOException {
        // Given
        String directoryName = "test-dir";
        Path directory = tempDir.resolve(directoryName);
        Files.createDirectories(directory);

        Files.createFile(directory.resolve("file1.txt"));
        Files.createFile(directory.resolve("file2.csv"));
        Files.createDirectories(directory.resolve("sub-dir"));

        // When
        List<Path> files = localFileManager.listFiles(directoryName);

        // Then
        assertThat(files).hasSize(2);
        assertThat(files).extracting(Path::getFileName).extracting(Path::toString)
            .containsExactlyInAnyOrder("file1.txt", "file2.csv");
    }

    @Test
    void shouldReturnEmptyListForNonExistingDirectory() {
        // When
        List<Path> files = localFileManager.listFiles("non-existing-dir");

        // Then
        assertThat(files).isEmpty();
    }

    @Test
    void shouldDeleteDirectory() throws IOException {
        // Given
        String directoryName = "dir-to-delete";
        Set<String> fileNames = Set.of("file1.txt", "file2.txt");
        Path createdDirectory = localFileManager.createDirectoryWithEmptyFiles(directoryName, fileNames);

        // When
        localFileManager.deleteDirectory(directoryName);

        // Then
        assertThat(createdDirectory).doesNotExist();
    }

    @Test
    void shouldDeleteDirectoryRecursively() throws IOException {
        // Given
        String parentDir = "parent";
        Path parentPath = tempDir.resolve(parentDir);
        Files.createDirectories(parentPath);
        Files.createDirectories(parentPath.resolve("sub-dir"));
        Files.createFile(parentPath.resolve("file1.txt"));
        Files.createFile(parentPath.resolve("sub-dir").resolve("file2.txt"));

        // When
        localFileManager.deleteDirectory(parentDir);

        // Then
        assertThat(parentPath).doesNotExist();
    }

    @Test
    void shouldNotFailWhenDeletingNonExistingDirectory() {
        // When & Then
        assertThatCode(() -> localFileManager.deleteDirectory("non-existing-dir"))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldDeleteFile() throws IOException {
        // Given
        Path testFile = tempDir.resolve("file-to-delete.txt");
        Files.createFile(testFile);

        // When
        localFileManager.deleteFile(testFile.toString());

        // Then
        assertThat(testFile).doesNotExist();
    }

    @Test
    void shouldNotFailWhenDeletingNonExistingFile() {
        // When & Then
        assertThatCode(() -> localFileManager.deleteFile("/non/existing/file.txt"))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldGetFileSize() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test-file.txt");
        String content = "Hello, World!";
        Files.writeString(testFile, content);

        // When
        long fileSize = localFileManager.getFileSize(testFile.toString());

        // Then
        assertThat(fileSize).isEqualTo(content.length());
    }

    @Test
    void shouldThrowExceptionWhenGettingSizeOfNonExistingFile() {
        // When & Then
        assertThatThrownBy(() -> localFileManager.getFileSize("/non/existing/file.txt"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("File does not exist: /non/existing/file.txt");
    }

    @Test
    void shouldDetectEmptyFile() throws IOException {
        // Given
        Path emptyFile = tempDir.resolve("empty.txt");
        Path nonEmptyFile = tempDir.resolve("non-empty.txt");
        Files.createFile(emptyFile);
        Files.writeString(nonEmptyFile, "content");

        // When & Then
        assertThat(localFileManager.isFileEmpty(emptyFile.toString())).isTrue();
        assertThat(localFileManager.isFileEmpty(nonEmptyFile.toString())).isFalse();
    }

    @Test
    void shouldReturnTempWorkingDirectory() {
        // When
        Path workingDir = localFileManager.getTempWorkingDirectory();

        // Then
        assertThat(workingDir).isEqualTo(tempDir);
    }

    @Test
    void shouldCleanSpecialCharactersInFileName() {
        // Given
        String directoryName = "test-dir";
        String problematicFileName = "file with spaces & special chars!.txt";

        // When
        Path createdFile = localFileManager.createEmptyFile(directoryName, problematicFileName);

        // Then
        assertThat(createdFile.getFileName().toString()).isEqualTo("file_with_spaces___special_chars_.txt");
        assertThat(createdFile).exists();
    }

    @Test
    void shouldHandleAbsolutePaths() throws IOException {
        // Given
        Path absoluteDir = tempDir.resolve("absolute-test");
        Files.createDirectories(absoluteDir);

        // When
        boolean exists = localFileManager.directoryExists(absoluteDir.toString());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldHandleRelativePaths() throws IOException {
        // Given
        String relativePath = "relative/path/test";

        // When
        Path createdFile = localFileManager.createEmptyFile(relativePath, "test.txt");

        // Then
        assertThat(createdFile.toString()).startsWith(tempDir.toString());
        assertThat(createdFile).exists();
    }
}