package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.FileData;
import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.testing.config.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("S3DirectoryManager Tests")
class S3DirectoryManagerTest extends BaseUnitTest {

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private S3DirectoryManager s3DirectoryManager;

    @Override
    protected Object[] getAllMocks() {
        return new Object[]{fileStorage};
    }

    @Test
    @DisplayName("Should create directory successfully")
    void shouldCreateDirectorySuccessfully() {
        String directoryPath = "test/directory";
        when(fileStorage.uploadFile(any(InputStream.class), eq("test/directory/.directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn("test/directory/.directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(directoryPath))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq("test/directory/.directory"),
                                     eq("application/directory"), eq(0L));
    }

    @Test
    @DisplayName("Should create directory with trailing slash")
    void shouldCreateDirectoryWithTrailingSlash() {
        String directoryPath = "test/directory/";
        when(fileStorage.uploadFile(any(InputStream.class), eq("test/directory/.directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn("test/directory/.directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(directoryPath))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq("test/directory/.directory"),
                                     eq("application/directory"), eq(0L));
    }

    @Test
    @DisplayName("Should handle empty directory path")
    void shouldHandleEmptyDirectoryPath() {
        String directoryPath = "";
        when(fileStorage.uploadFile(any(InputStream.class), eq(".directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn(".directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(directoryPath))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq(".directory"),
                                     eq("application/directory"), eq(0L));
    }

    @Test
    @DisplayName("Should throw exception when create directory fails")
    void shouldThrowExceptionWhenCreateDirectoryFails() {
        String directoryPath = "test/directory";
        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> s3DirectoryManager.createDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create directory");
    }

    @Test
    @DisplayName("Should add file to directory successfully")
    void shouldAddFileToDirectorySuccessfully() {
        String directoryPath = "test/directory";
        String fileName = "test.txt";
        InputStream content = new ByteArrayInputStream("test content".getBytes());
        String contentType = "text/plain";

        when(fileStorage.uploadFile(eq(content), eq("test/directory/test.txt"),
                                  eq(contentType), anyLong()))
                .thenReturn("test/directory/test.txt");

        assertThatCode(() -> s3DirectoryManager.addFileToDirectory(directoryPath, fileName, content, contentType))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(eq(content), eq("test/directory/test.txt"),
                                     eq(contentType), anyLong());
    }

    @Test
    @DisplayName("Should throw exception when add file to directory fails")
    void shouldThrowExceptionWhenAddFileToDirectoryFails() {
        String directoryPath = "test/directory";
        String fileName = "test.txt";
        InputStream content = new ByteArrayInputStream("test content".getBytes());
        String contentType = "text/plain";

        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> s3DirectoryManager.addFileToDirectory(directoryPath, fileName, content, contentType))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to add file to directory");
    }

    @Test
    @DisplayName("Should add multiple files to directory successfully")
    void shouldAddMultipleFilesToDirectorySuccessfully() {
        String directoryPath = "test/directory";
        Map<String, FileData> files = Map.of(
                "file1.txt", new FileData(new ByteArrayInputStream("content1".getBytes()), "text/plain", 8L),
                "file2.txt", new FileData(new ByteArrayInputStream("content2".getBytes()), "text/plain", 8L)
        );

        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
                .thenReturn("uploaded-key");

        assertThatCode(() -> s3DirectoryManager.addFilesToDirectory(directoryPath, files))
                .doesNotThrowAnyException();

        verify(fileStorage, times(2)).uploadFile(any(InputStream.class), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Should check if directory exists")
    void shouldCheckIfDirectoryExists() {
        String directoryPath = "test/directory";
        when(fileStorage.fileExists("test/directory/.directory")).thenReturn(true);

        boolean result = s3DirectoryManager.directoryExists(directoryPath);

        assertThat(result).isTrue();
        verify(fileStorage).fileExists("test/directory/.directory");
    }

    @Test
    @DisplayName("Should return false when directory does not exist")
    void shouldReturnFalseWhenDirectoryDoesNotExist() {
        String directoryPath = "test/directory";
        when(fileStorage.fileExists("test/directory/.directory")).thenReturn(false);

        boolean result = s3DirectoryManager.directoryExists(directoryPath);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should delete directory successfully")
    void shouldDeleteDirectorySuccessfully() {
        String directoryPath = "test/directory";
        List<String> files = List.of(
                "test/directory/.directory",
                "test/directory/file1.txt",
                "test/directory/file2.txt"
        );

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        doNothing().when(fileStorage).deleteFile(anyString());

        assertThatCode(() -> s3DirectoryManager.deleteDirectory(directoryPath))
                .doesNotThrowAnyException();

        verify(fileStorage).listFiles("test/directory/");
        verify(fileStorage, times(3)).deleteFile(anyString());
    }

    @Test
    @DisplayName("Should throw exception when delete directory fails")
    void shouldThrowExceptionWhenDeleteDirectoryFails() {
        String directoryPath = "test/directory";
        when(fileStorage.listFiles("test/directory/"))
                .thenThrow(new RuntimeException("List failed"));

        assertThatThrownBy(() -> s3DirectoryManager.deleteDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete directory");
    }

    @Test
    @DisplayName("Should normalize directory paths correctly")
    void shouldNormalizeDirectoryPathsCorrectly() {
        // Test with backslashes
        String windowsPath = "test\\directory\\subdirectory";
        when(fileStorage.uploadFile(any(InputStream.class), eq("test/directory/subdirectory/.directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn("test/directory/subdirectory/.directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(windowsPath))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq("test/directory/subdirectory/.directory"),
                                     eq("application/directory"), eq(0L));
    }

    @Test
    @DisplayName("Should handle null directory path")
    void shouldHandleNullDirectoryPath() {
        when(fileStorage.uploadFile(any(InputStream.class), eq(".directory"),
                                  eq("application/directory"), eq(0L)))
                .thenReturn(".directory");

        assertThatCode(() -> s3DirectoryManager.createDirectory(null))
                .doesNotThrowAnyException();

        verify(fileStorage).uploadFile(any(InputStream.class), eq(".directory"),
                                     eq("application/directory"), eq(0L));
    }
}