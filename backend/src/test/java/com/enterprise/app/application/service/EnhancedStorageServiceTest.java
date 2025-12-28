package com.enterprise.app.application.service;

import com.enterprise.app.domain.storage.DirectoryManager;
import com.enterprise.app.domain.storage.FileData;
import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.domain.storage.ZipService;
import com.enterprise.app.testing.config.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("EnhancedStorageService Tests")
class EnhancedStorageServiceTest extends BaseUnitTest {

    @Mock
    private FileStorage fileStorage;

    @Mock
    private DirectoryManager directoryManager;

    @Mock
    private ZipService zipService;

    @InjectMocks
    private EnhancedStorageService enhancedStorageService;

    @Override
    protected Object[] getAllMocks() {
        return new Object[]{fileStorage, directoryManager, zipService};
    }

    @Test
    @DisplayName("Should create directory with files successfully")
    void shouldCreateDirectoryWithFilesSuccessfully() throws IOException {
        String baseDirectoryName = "test-project";
        MockMultipartFile file1 = new MockMultipartFile("file1", "file1.txt", "text/plain", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "file2.txt", "text/plain", "content2".getBytes());
        Map<String, MultipartFile> files = Map.of("file1.txt", file1, "file2.txt", file2);

        doNothing().when(directoryManager).createDirectory(anyString());
        doNothing().when(directoryManager).addFilesToDirectory(anyString(), any(Map.class));

        String result = enhancedStorageService.createDirectoryWithFiles(baseDirectoryName, files);

        assertThat(result).isNotNull();
        assertThat(result).contains("test-project");
        assertThat(result).startsWith("projects/");

        verify(directoryManager).createDirectory(anyString());
        verify(directoryManager).addFilesToDirectory(anyString(), any(Map.class));
    }

    @Test
    @DisplayName("Should throw exception when create directory with files fails")
    void shouldThrowExceptionWhenCreateDirectoryWithFilesFails() throws IOException {
        String baseDirectoryName = "test-project";
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", "content".getBytes());
        Map<String, MultipartFile> files = Map.of("file.txt", file);

        doThrow(new RuntimeException("Directory creation failed"))
                .when(directoryManager).createDirectory(anyString());

        assertThatThrownBy(() -> enhancedStorageService.createDirectoryWithFiles(baseDirectoryName, files))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create directory with files");
    }

    @Test
    @DisplayName("Should create and zip directory successfully")
    void shouldCreateAndZipDirectorySuccessfully() throws IOException {
        String baseDirectoryName = "test-project";
        String zipFileName = "project-archive.zip";
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", "content".getBytes());
        Map<String, MultipartFile> files = Map.of("file.txt", file);

        doNothing().when(directoryManager).createDirectory(anyString());
        doNothing().when(directoryManager).addFilesToDirectory(anyString(), any(Map.class));
        when(zipService.uploadZippedDirectory(anyString(), eq(zipFileName)))
                .thenReturn("archives/2025/12/27/12345678-project-archive.zip");

        String result = enhancedStorageService.createAndZipDirectory(baseDirectoryName, files, zipFileName);

        assertThat(result).isEqualTo("archives/2025/12/27/12345678-project-archive.zip");

        verify(directoryManager).createDirectory(anyString());
        verify(directoryManager).addFilesToDirectory(anyString(), any(Map.class));
        verify(zipService).uploadZippedDirectory(anyString(), eq(zipFileName));
    }

    @Test
    @DisplayName("Should throw exception when create and zip directory fails")
    void shouldThrowExceptionWhenCreateAndZipDirectoryFails() throws IOException {
        String baseDirectoryName = "test-project";
        String zipFileName = "project-archive.zip";
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", "content".getBytes());
        Map<String, MultipartFile> files = Map.of("file.txt", file);

        doNothing().when(directoryManager).createDirectory(anyString());
        doNothing().when(directoryManager).addFilesToDirectory(anyString(), any(Map.class));
        when(zipService.uploadZippedDirectory(anyString(), anyString()))
                .thenThrow(new RuntimeException("Zip failed"));

        assertThatThrownBy(() -> enhancedStorageService.createAndZipDirectory(baseDirectoryName, files, zipFileName))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create and zip directory");
    }

    @Test
    @DisplayName("Should add file to existing directory successfully")
    void shouldAddFileToExistingDirectorySuccessfully() throws IOException {
        String directoryPath = "projects/2025/12/27/12345678-test-project";
        String fileName = "new-file.txt";
        MockMultipartFile file = new MockMultipartFile("file", "original-name.txt", "text/plain", "content".getBytes());

        when(directoryManager.directoryExists(directoryPath)).thenReturn(true);
        doNothing().when(directoryManager).addFileToDirectory(eq(directoryPath), eq(fileName), any(InputStream.class), eq("text/plain"));

        String result = enhancedStorageService.addFileToExistingDirectory(directoryPath, fileName, file);

        assertThat(result).isEqualTo(directoryPath + "/" + fileName);

        verify(directoryManager).directoryExists(directoryPath);
        verify(directoryManager).addFileToDirectory(eq(directoryPath), eq(fileName), any(InputStream.class), eq("text/plain"));
    }

    @Test
    @DisplayName("Should throw exception when directory does not exist")
    void shouldThrowExceptionWhenDirectoryDoesNotExist() throws IOException {
        String directoryPath = "projects/2025/12/27/12345678-test-project";
        String fileName = "new-file.txt";
        MockMultipartFile file = new MockMultipartFile("file", "original-name.txt", "text/plain", "content".getBytes());

        when(directoryManager.directoryExists(directoryPath)).thenReturn(false);

        assertThatThrownBy(() -> enhancedStorageService.addFileToExistingDirectory(directoryPath, fileName, file))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to add file");
    }

    @Test
    @DisplayName("Should download zipped directory successfully")
    void shouldDownloadZippedDirectorySuccessfully() {
        String directoryPath = "projects/2025/12/27/12345678-test-project";
        InputStream expectedStream = new ByteArrayInputStream("zip content".getBytes());

        when(zipService.createZipFromDirectory(directoryPath)).thenReturn(expectedStream);

        InputStream result = enhancedStorageService.downloadZippedDirectory(directoryPath);

        assertThat(result).isEqualTo(expectedStream);
        verify(zipService).createZipFromDirectory(directoryPath);
    }

    @Test
    @DisplayName("Should throw exception when download zipped directory fails")
    void shouldThrowExceptionWhenDownloadZippedDirectoryFails() {
        String directoryPath = "projects/2025/12/27/12345678-test-project";

        when(zipService.createZipFromDirectory(directoryPath))
                .thenThrow(new RuntimeException("Zip creation failed"));

        assertThatThrownBy(() -> enhancedStorageService.downloadZippedDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to download zipped directory");
    }

    @Test
    @DisplayName("Should delete directory successfully")
    void shouldDeleteDirectorySuccessfully() {
        String directoryPath = "projects/2025/12/27/12345678-test-project";

        doNothing().when(directoryManager).deleteDirectory(directoryPath);

        assertThatCode(() -> enhancedStorageService.deleteDirectory(directoryPath))
                .doesNotThrowAnyException();

        verify(directoryManager).deleteDirectory(directoryPath);
    }

    @Test
    @DisplayName("Should throw exception when delete directory fails")
    void shouldThrowExceptionWhenDeleteDirectoryFails() {
        String directoryPath = "projects/2025/12/27/12345678-test-project";

        doThrow(new RuntimeException("Delete failed"))
                .when(directoryManager).deleteDirectory(directoryPath);

        assertThatThrownBy(() -> enhancedStorageService.deleteDirectory(directoryPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete directory");
    }

    @Test
    @DisplayName("Should upload single file successfully")
    void shouldUploadSingleFileSuccessfully() throws IOException {
        String folder = "documents";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        when(fileStorage.uploadFile(any(InputStream.class), anyString(), eq("text/plain"), eq(7L)))
                .thenReturn("documents/2025/12/27/12345678-test.txt");

        String result = enhancedStorageService.uploadSingleFile(file, folder);

        assertThat(result).isEqualTo("documents/2025/12/27/12345678-test.txt");

        verify(fileStorage).uploadFile(any(InputStream.class), anyString(), eq("text/plain"), eq(7L));
    }

    @Test
    @DisplayName("Should download file successfully")
    void shouldDownloadFileSuccessfully() {
        String key = "documents/2025/12/27/12345678-test.txt";
        InputStream expectedStream = new ByteArrayInputStream("file content".getBytes());

        when(fileStorage.downloadFile(key)).thenReturn(expectedStream);

        InputStream result = enhancedStorageService.downloadFile(key);

        assertThat(result).isEqualTo(expectedStream);
        verify(fileStorage).downloadFile(key);
    }

    @Test
    @DisplayName("Should generate unique directory paths")
    void shouldGenerateUniqueDirectoryPaths() throws IOException {
        String baseDirectoryName = "test-project";
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", "content".getBytes());
        Map<String, MultipartFile> files = Map.of("file.txt", file);

        doNothing().when(directoryManager).createDirectory(anyString());
        doNothing().when(directoryManager).addFilesToDirectory(anyString(), any(Map.class));

        String result1 = enhancedStorageService.createDirectoryWithFiles(baseDirectoryName, files);
        String result2 = enhancedStorageService.createDirectoryWithFiles(baseDirectoryName, files);

        assertThat(result1).isNotEqualTo(result2);
        assertThat(result1).startsWith("projects/");
        assertThat(result2).startsWith("projects/");
        assertThat(result1).contains("test-project");
        assertThat(result2).contains("test-project");
    }

    @Test
    @DisplayName("Should clean special characters from filenames")
    void shouldCleanSpecialCharactersFromFilenames() throws IOException {
        String folder = "documents";
        MockMultipartFile file = new MockMultipartFile("file", "test@#$%file.txt", "text/plain", "content".getBytes());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(fileStorage.uploadFile(any(InputStream.class), keyCaptor.capture(), eq("text/plain"), eq(7L)))
                .thenReturn("documents/2025/12/27/12345678-test____file.txt");

        enhancedStorageService.uploadSingleFile(file, folder);

        String capturedKey = keyCaptor.getValue();
        assertThat(capturedKey).contains("test____file.txt");
        assertThat(capturedKey).doesNotContain("@", "#", "$", "%");
    }

    @Test
    @DisplayName("Should handle null folder in single file upload")
    void shouldHandleNullFolderInSingleFileUpload() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(fileStorage.uploadFile(any(InputStream.class), keyCaptor.capture(), eq("text/plain"), eq(7L)))
                .thenReturn("2025/12/27/12345678-test.txt");

        enhancedStorageService.uploadSingleFile(file, null);

        String capturedKey = keyCaptor.getValue();
        assertThat(capturedKey).startsWith("2025/");
        assertThat(capturedKey).contains("test.txt");
    }

    @Test
    @DisplayName("Should verify correct FileData mapping")
    void shouldVerifyCorrectFileDataMapping() throws IOException {
        String baseDirectoryName = "test-project";
        MockMultipartFile file1 = new MockMultipartFile("file1", "file1.txt", "text/plain", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "file2.json", "application/json", "{}".getBytes());
        Map<String, MultipartFile> files = Map.of("file1.txt", file1, "file2.json", file2);

        ArgumentCaptor<Map<String, FileData>> fileDataCaptor = ArgumentCaptor.forClass(Map.class);
        doNothing().when(directoryManager).createDirectory(anyString());
        doNothing().when(directoryManager).addFilesToDirectory(anyString(), fileDataCaptor.capture());

        enhancedStorageService.createDirectoryWithFiles(baseDirectoryName, files);

        Map<String, FileData> capturedFileData = fileDataCaptor.getValue();
        assertThat(capturedFileData).hasSize(2);
        assertThat(capturedFileData).containsKey("file1.txt");
        assertThat(capturedFileData).containsKey("file2.json");

        FileData fileData1 = capturedFileData.get("file1.txt");
        assertThat(fileData1.contentType()).isEqualTo("text/plain");
        assertThat(fileData1.contentLength()).isEqualTo(8L);

        FileData fileData2 = capturedFileData.get("file2.json");
        assertThat(fileData2.contentType()).isEqualTo("application/json");
        assertThat(fileData2.contentLength()).isEqualTo(2L);
    }
}