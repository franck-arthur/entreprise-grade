package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.FileMetadata;
import com.enterprise.app.infrastructure.config.S3Config;
import com.enterprise.app.testing.config.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("S3FileStorage Tests")
class S3FileStorageTest extends BaseUnitTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Config.S3Properties s3Properties;

    @InjectMocks
    private S3FileStorage s3FileStorage;

    private final String testBucketName = "test-bucket";
    private final String testKey = "test/file.txt";
    private final String testContentType = "text/plain";
    private final long testContentLength = 100L;

    @BeforeEach
    void setUp() {
        when(s3Properties.getBucketName()).thenReturn(testBucketName);
    }

    @Override
    protected Object[] getAllMocks() {
        return new Object[]{s3Client, s3Properties};
    }

    @Test
    @DisplayName("Should upload file successfully")
    void shouldUploadFileSuccessfully() {
        InputStream content = new ByteArrayInputStream("test content".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String result = s3FileStorage.uploadFile(content, testKey, testContentType, testContentLength);

        assertThat(result).isEqualTo(testKey);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Should throw exception when upload fails")
    void shouldThrowExceptionWhenUploadFails() {
        InputStream content = new ByteArrayInputStream("test content".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Upload failed").build());

        assertThatThrownBy(() -> s3FileStorage.uploadFile(content, testKey, testContentType, testContentLength))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 error during file upload");
    }

    @Test
    @DisplayName("Should download file successfully")
    void shouldDownloadFileSuccessfully() {
        GetObjectResponse response = GetObjectResponse.builder().build();
        InputStream expectedContent = new ByteArrayInputStream("test content".getBytes());
        ResponseInputStream<GetObjectResponse> responseInputStream =
                new ResponseInputStream<>(response, expectedContent);
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseInputStream);

        InputStream result = s3FileStorage.downloadFile(testKey);

        assertThat(result).isEqualTo(responseInputStream);
        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when download fails")
    void shouldThrowExceptionWhenDownloadFails() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("File not found").build());

        assertThatThrownBy(() -> s3FileStorage.downloadFile(testKey))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to download file from S3");
    }

    @Test
    @DisplayName("Should delete file successfully")
    void shouldDeleteFileSuccessfully() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        assertThatCode(() -> s3FileStorage.deleteFile(testKey))
                .doesNotThrowAnyException();

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when delete fails")
    void shouldThrowExceptionWhenDeleteFails() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Delete failed").build());

        assertThatThrownBy(() -> s3FileStorage.deleteFile(testKey))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete file from S3");
    }

    @Test
    @DisplayName("Should list files with prefix successfully")
    void shouldListFilesWithPrefixSuccessfully() {
        String prefix = "test/";
        S3Object object1 = S3Object.builder().key("test/file1.txt").build();
        S3Object object2 = S3Object.builder().key("test/file2.txt").build();
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(Arrays.asList(object1, object2))
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(response);

        List<String> result = s3FileStorage.listFiles(prefix);

        assertThat(result).hasSize(2);
        assertThat(result).contains("test/file1.txt", "test/file2.txt");
    }

    @Test
    @DisplayName("Should list files without prefix successfully")
    void shouldListFilesWithoutPrefixSuccessfully() {
        S3Object object1 = S3Object.builder().key("file1.txt").build();
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(List.of(object1))
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(response);

        List<String> result = s3FileStorage.listFiles(null);

        assertThat(result).hasSize(1);
        assertThat(result).contains("file1.txt");
    }

    @Test
    @DisplayName("Should throw exception when list files fails")
    void shouldThrowExceptionWhenListFilesFails() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message("List failed").build());

        assertThatThrownBy(() -> s3FileStorage.listFiles("test/"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to list files from S3");
    }

    @Test
    @DisplayName("Should return true when file exists")
    void shouldReturnTrueWhenFileExists() {
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentType(testContentType)
                .contentLength(testContentLength)
                .lastModified(Instant.now())
                .metadata(Map.of("test-key", "test-value"))
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headResponse);

        boolean result = s3FileStorage.fileExists(testKey);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when file does not exist")
    void shouldReturnFalseWhenFileDoesNotExist() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Not found").build());

        boolean result = s3FileStorage.fileExists(testKey);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should get file metadata successfully")
    void shouldGetFileMetadataSuccessfully() {
        Instant lastModified = Instant.now();
        Map<String, String> metadata = Map.of("test-key", "test-value");
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentType(testContentType)
                .contentLength(testContentLength)
                .lastModified(lastModified)
                .metadata(metadata)
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headResponse);

        FileMetadata result = s3FileStorage.getFileMetadata(testKey);

        assertThat(result.key()).isEqualTo(testKey);
        assertThat(result.contentType()).isEqualTo(testContentType);
        assertThat(result.contentLength()).isEqualTo(testContentLength);
        assertThat(result.lastModified()).isEqualTo(LocalDateTime.ofInstant(lastModified, ZoneOffset.UTC));
        assertThat(result.userMetadata()).isEqualTo(metadata);
    }

    @Test
    @DisplayName("Should throw exception when get metadata fails")
    void shouldThrowExceptionWhenGetMetadataFails() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Metadata failed").build());

        assertThatThrownBy(() -> s3FileStorage.getFileMetadata(testKey))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get file metadata from S3");
    }
}