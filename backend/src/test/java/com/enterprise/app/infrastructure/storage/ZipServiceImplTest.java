package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.testing.config.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ZipServiceImpl Tests")
class ZipServiceImplTest extends BaseUnitTest {

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private ZipServiceImpl zipService;

    @Override
    protected Object[] getAllMocks() {
        return new Object[]{fileStorage};
    }

    @Test
    @DisplayName("Should create ZIP from directory successfully")
    void shouldCreateZipFromDirectorySuccessfully() throws IOException {
        String directoryPath = "test/directory";
        List<String> files = Arrays.asList(
                "test/directory/file1.txt",
                "test/directory/file2.txt",
                "test/directory/.directory"
        );

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        when(fileStorage.downloadFile("test/directory/file1.txt"))
                .thenReturn(new ByteArrayInputStream("content1".getBytes()));
        when(fileStorage.downloadFile("test/directory/file2.txt"))
                .thenReturn(new ByteArrayInputStream("content2".getBytes()));

        InputStream zipStream = zipService.createZipFromDirectory(directoryPath);

        assertThat(zipStream).isNotNull();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry1 = zis.getNextEntry();
            assertThat(entry1.getName()).isEqualTo("file1.txt");

            ZipEntry entry2 = zis.getNextEntry();
            assertThat(entry2.getName()).isEqualTo("file2.txt");

            ZipEntry entry3 = zis.getNextEntry();
            assertThat(entry3).isNull();
        }

        verify(fileStorage).listFiles("test/directory/");
        verify(fileStorage).downloadFile("test/directory/file1.txt");
        verify(fileStorage).downloadFile("test/directory/file2.txt");
        verify(fileStorage, never()).downloadFile("test/directory/.directory");
    }

    @Test
    @DisplayName("Should return empty ZIP when no files in directory")
    void shouldReturnEmptyZipWhenNoFilesInDirectory() {
        String directoryPath = "test/empty";
        when(fileStorage.listFiles("test/empty/")).thenReturn(List.of());

        InputStream zipStream = zipService.createZipFromDirectory(directoryPath);

        assertThat(zipStream).isNotNull();
        assertThat(zipStream).isInstanceOf(ByteArrayInputStream.class);

        verify(fileStorage).listFiles("test/empty/");
        verify(fileStorage, never()).downloadFile(anyString());
    }

    @Test
    @DisplayName("Should handle directory with only directory marker")
    void shouldHandleDirectoryWithOnlyDirectoryMarker() throws IOException {
        String directoryPath = "test/directory";
        List<String> files = List.of("test/directory/.directory");

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);

        InputStream zipStream = zipService.createZipFromDirectory(directoryPath);

        assertThat(zipStream).isNotNull();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry = zis.getNextEntry();
            assertThat(entry).isNull();
        }

        verify(fileStorage).listFiles("test/directory/");
        verify(fileStorage, never()).downloadFile(anyString());
    }

    @Test
    @DisplayName("Should throw exception when file listing fails")
    void shouldThrowExceptionWhenFileListingFails() {
        String directoryPath = "test/directory";
        when(fileStorage.listFiles("test/directory/"))
                .thenThrow(new RuntimeException("Listing failed"));

        assertThatThrownBy(() -> zipService.createZipFromDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create ZIP archive");
    }

    @Test
    @DisplayName("Should throw exception when file download fails")
    void shouldThrowExceptionWhenFileDownloadFails() {
        String directoryPath = "test/directory";
        List<String> files = List.of("test/directory/file1.txt");

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        when(fileStorage.downloadFile("test/directory/file1.txt"))
                .thenThrow(new RuntimeException("Download failed"));

        assertThatThrownBy(() -> zipService.createZipFromDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create ZIP archive");
    }

    @Test
    @DisplayName("Should upload zipped directory successfully")
    void shouldUploadZippedDirectorySuccessfully() {
        String directoryPath = "test/directory";
        String zipFileName = "test-archive";
        List<String> files = List.of("test/directory/file1.txt");

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        when(fileStorage.downloadFile("test/directory/file1.txt"))
                .thenReturn(new ByteArrayInputStream("content1".getBytes()));
        when(fileStorage.uploadFile(any(InputStream.class), anyString(), eq("application/zip"), anyLong()))
                .thenReturn("archives/2024/12/27/12345678-test-archive.zip");

        String result = zipService.uploadZippedDirectory(directoryPath, zipFileName);

        assertThat(result).isEqualTo("archives/2024/12/27/12345678-test-archive.zip");
        verify(fileStorage).uploadFile(any(InputStream.class), anyString(), eq("application/zip"), anyLong());
    }

    @Test
    @DisplayName("Should add .zip extension if not present")
    void shouldAddZipExtensionIfNotPresent() {
        String directoryPath = "test/directory";
        String zipFileName = "test-archive";
        when(fileStorage.listFiles("test/directory/")).thenReturn(List.of());
        when(fileStorage.uploadFile(any(InputStream.class), contains("test-archive.zip"), eq("application/zip"), anyLong()))
                .thenReturn("archives/2024/12/27/12345678-test-archive.zip");

        String result = zipService.uploadZippedDirectory(directoryPath, zipFileName);

        assertThat(result).contains("test-archive.zip");
        verify(fileStorage).uploadFile(any(InputStream.class), contains("test-archive.zip"), eq("application/zip"), anyLong());
    }

    @Test
    @DisplayName("Should not add .zip extension if already present")
    void shouldNotAddZipExtensionIfAlreadyPresent() {
        String directoryPath = "test/directory";
        String zipFileName = "test-archive.zip";
        when(fileStorage.listFiles("test/directory/")).thenReturn(List.of());
        when(fileStorage.uploadFile(any(InputStream.class), contains("test-archive.zip"), eq("application/zip"), anyLong()))
                .thenReturn("archives/2024/12/27/12345678-test-archive.zip");

        String result = zipService.uploadZippedDirectory(directoryPath, zipFileName);

        assertThat(result).contains("test-archive.zip");
        verify(fileStorage).uploadFile(any(InputStream.class), contains("test-archive.zip"), eq("application/zip"), anyLong());
    }

    @Test
    @DisplayName("Should clean filename with special characters")
    void shouldCleanFilenameWithSpecialCharacters() {
        String directoryPath = "test/directory";
        String zipFileName = "test@#$%archive.zip";
        when(fileStorage.listFiles("test/directory/")).thenReturn(List.of());
        when(fileStorage.uploadFile(any(InputStream.class), contains("test____archive.zip"), eq("application/zip"), anyLong()))
                .thenReturn("archives/2024/12/27/12345678-test____archive.zip");

        String result = zipService.uploadZippedDirectory(directoryPath, zipFileName);

        assertThat(result).contains("test____archive.zip");
        verify(fileStorage).uploadFile(any(InputStream.class), contains("test____archive.zip"), eq("application/zip"), anyLong());
    }

    @Test
    @DisplayName("Should throw exception when upload fails")
    void shouldThrowExceptionWhenUploadFails() {
        String directoryPath = "test/directory";
        String zipFileName = "test-archive";
        when(fileStorage.listFiles("test/directory/")).thenReturn(List.of());
        when(fileStorage.uploadFile(any(InputStream.class), anyString(), eq("application/zip"), anyLong()))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> zipService.uploadZippedDirectory(directoryPath, zipFileName))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload zipped directory");
    }

    @Test
    @DisplayName("Should handle nested directory structure")
    void shouldHandleNestedDirectoryStructure() throws IOException {
        String directoryPath = "test/directory";
        List<String> files = Arrays.asList(
                "test/directory/subdir/file1.txt",
                "test/directory/file2.txt"
        );

        when(fileStorage.listFiles("test/directory/")).thenReturn(files);
        when(fileStorage.downloadFile("test/directory/subdir/file1.txt"))
                .thenReturn(new ByteArrayInputStream("content1".getBytes()));
        when(fileStorage.downloadFile("test/directory/file2.txt"))
                .thenReturn(new ByteArrayInputStream("content2".getBytes()));

        InputStream zipStream = zipService.createZipFromDirectory(directoryPath);

        assertThat(zipStream).isNotNull();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry1 = zis.getNextEntry();
            assertThat(entry1.getName()).isEqualTo("subdir/file1.txt");

            ZipEntry entry2 = zis.getNextEntry();
            assertThat(entry2.getName()).isEqualTo("file2.txt");
        }
    }

    @Test
    @DisplayName("Should handle empty directory path")
    void shouldHandleEmptyDirectoryPath() {
        String directoryPath = "";
        when(fileStorage.listFiles("")).thenReturn(List.of("file.txt"));
        when(fileStorage.downloadFile("file.txt"))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

        InputStream result = zipService.createZipFromDirectory(directoryPath);

        assertThat(result).isNotNull();
        verify(fileStorage).listFiles("");
    }

    @Test
    @DisplayName("Should handle null directory path")
    void shouldHandleNullDirectoryPath() {
        String directoryPath = null;
        when(fileStorage.listFiles("")).thenReturn(List.of("file.txt"));
        when(fileStorage.downloadFile("file.txt"))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

        InputStream result = zipService.createZipFromDirectory(directoryPath);

        assertThat(result).isNotNull();
        verify(fileStorage).listFiles("");
    }
}