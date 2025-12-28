package com.enterprise.app.application.service;

import com.enterprise.app.infrastructure.config.S3Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StorageService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StorageService Tests")
class StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Config.S3Properties s3Properties;

    @InjectMocks
    private StorageService storageService;

    private MockMultipartFile testFile;
    private String testBucketName = "test-bucket";
    private String testKey = "2024/12/16/12345678-testfile.txt";

    @BeforeEach
    void setUp() {
        when(s3Properties.getBucketName()).thenReturn(testBucketName);

        testFile = new MockMultipartFile(
                "file",
                "testfile.txt",
                "text/plain",
                "Test file content".getBytes()
        );
    }

    @AfterEach
    void tearDown() {
        reset(s3Client, s3Properties);
    }

    @Test
    @DisplayName("Should upload file successfully with folder")
    void shouldUploadFileSuccessfullyWithFolder() {
        // Given
        String folder = "documents";
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        String result = storageService.uploadFile(testFile, folder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains(folder);
        assertThat(result).contains("testfile.txt");

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should upload file successfully without folder")
    void shouldUploadFileSuccessfullyWithoutFolder() {
        // Given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        String result = storageService.uploadFile(testFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("testfile.txt");

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should throw exception when upload fails")
    void shouldThrowExceptionWhenUploadFails() {
        // Given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Upload failed").build());

        // When & Then
        assertThatThrownBy(() -> storageService.uploadFile(testFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 error during file upload");
    }

    @Test
    @DisplayName("Should download file successfully")
    void shouldDownloadFileSuccessfully() {
        // Given
        byte[] testContent = "Test content".getBytes();
        GetObjectResponse mockResponse = GetObjectResponse.builder().build();
        ResponseInputStream<GetObjectResponse> mockResponseStream =
                new ResponseInputStream<>(mockResponse, new ByteArrayInputStream(testContent));

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(mockResponseStream);

        // When
        InputStream result = storageService.downloadFile(testKey);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockResponseStream);

        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when download fails")
    void shouldThrowExceptionWhenDownloadFails() {
        // Given
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("File not found").build());

        // When & Then
        assertThatThrownBy(() -> storageService.downloadFile(testKey))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to download file from S3");
    }

    @Test
    @DisplayName("Should delete file successfully")
    void shouldDeleteFileSuccessfully() {
        // Given
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        // When & Then
        assertThatCode(() -> storageService.deleteFile(testKey))
                .doesNotThrowAnyException();

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when delete fails")
    void shouldThrowExceptionWhenDeleteFails() {
        // Given
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Delete failed").build());

        // When & Then
        assertThatThrownBy(() -> storageService.deleteFile(testKey))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete file from S3");
    }

    @Test
    @DisplayName("Should list files in folder successfully")
    void shouldListFilesInFolderSuccessfully() {
        // Given
        String folder = "documents";
        S3Object object1 = S3Object.builder().key(folder + "/file1.txt").build();
        S3Object object2 = S3Object.builder().key(folder + "/file2.txt").build();
        List<S3Object> objects = Arrays.asList(object1, object2);

        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(objects)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(response);

        // When
        List<String> result = storageService.listFiles(folder);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(folder + "/file1.txt", folder + "/file2.txt");

        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    @DisplayName("Should list all files successfully")
    void shouldListAllFilesSuccessfully() {
        // Given
        S3Object object1 = S3Object.builder().key("file1.txt").build();
        S3Object object2 = S3Object.builder().key("docs/file2.txt").build();
        List<S3Object> objects = Arrays.asList(object1, object2);

        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(objects)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(response);

        // When
        List<String> result = storageService.listAllFiles();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains("file1.txt", "docs/file2.txt");
    }

    @Test
    @DisplayName("Should get file metadata successfully")
    void shouldGetFileMetadataSuccessfully() {
        // Given
        HeadObjectResponse metadata = HeadObjectResponse.builder()
                .contentLength(1024L)
                .contentType("text/plain")
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(metadata);

        // When
        HeadObjectResponse result = storageService.getFileMetadata(testKey);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.contentLength()).isEqualTo(1024L);
        assertThat(result.contentType()).isEqualTo("text/plain");
    }

    @Test
    @DisplayName("Should return true when file exists")
    void shouldReturnTrueWhenFileExists() {
        // Given
        HeadObjectResponse metadata = HeadObjectResponse.builder().build();
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(metadata);

        // When
        boolean result = storageService.fileExists(testKey);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when file does not exist")
    void shouldReturnFalseWhenFileDoesNotExist() {
        // Given
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Not found").build());

        // When
        boolean result = storageService.fileExists(testKey);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should clean filename properly")
    void shouldCleanFilenameProperly() {
        // Given
        MockMultipartFile fileWithSpecialChars = new MockMultipartFile(
                "file",
                "test file@#$%^&*().txt",
                "text/plain",
                "Test content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        String result = storageService.uploadFile(fileWithSpecialChars);

        // Then
        assertThat(result).contains("test_file");
        assertThat(result).doesNotContain("@", "#", "$", "%", "^", "&", "*", "(", ")");
    }
}