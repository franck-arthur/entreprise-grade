package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.DirectoryManager;
import com.enterprise.app.domain.storage.FileData;
import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.domain.storage.ZipService;
import com.enterprise.app.infrastructure.config.S3Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest
@Testcontainers
@DisplayName("S3 Storage Integration Tests")
class S3StorageIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:2.3.0"))
            .withServices(S3)
            .withEnv("DEBUG", "1");

    private FileStorage fileStorage;
    private DirectoryManager directoryManager;
    private ZipService zipService;
    private S3Client s3Client;

    private static final String BUCKET_NAME = "test-bucket";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.s3.endpoint", () -> localstack.getEndpointOverride(S3).toString());
        registry.add("app.s3.bucket-name", () -> BUCKET_NAME);
        registry.add("app.s3.access-key", () -> localstack.getAccessKey());
        registry.add("app.s3.secret-key", () -> localstack.getSecretKey());
        registry.add("app.s3.region", () -> localstack.getRegion());
    }

    @BeforeEach
    void setUp() {
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .region(Region.of(localstack.getRegion()))
                .build();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

        S3Config.S3Properties s3Properties = new S3Config.S3Properties();
        s3Properties.setBucketName(BUCKET_NAME);

        fileStorage = new S3FileStorage(s3Client, s3Properties);
        directoryManager = new S3DirectoryManager(fileStorage);
        zipService = new ZipServiceImpl(fileStorage);
    }

    @Test
    @DisplayName("Should upload and download file successfully")
    void shouldUploadAndDownloadFileSuccessfully() throws IOException {
        String key = "test/integration-file.txt";
        String content = "Test content for integration";
        String contentType = "text/plain";

        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes())) {
            String uploadedKey = fileStorage.uploadFile(inputStream, key, contentType, content.length());

            assertThat(uploadedKey).isEqualTo(key);

            try (InputStream downloadedStream = fileStorage.downloadFile(key)) {
                String downloadedContent = new String(downloadedStream.readAllBytes());
                assertThat(downloadedContent).isEqualTo(content);
            }
        }
    }

    @Test
    @DisplayName("Should create directory and add files")
    void shouldCreateDirectoryAndAddFiles() throws IOException {
        String directoryPath = "test-project/integration";

        directoryManager.createDirectory(directoryPath);

        assertThat(directoryManager.directoryExists(directoryPath)).isTrue();

        Map<String, FileData> files = Map.of(
                "file1.txt", new FileData(
                        new ByteArrayInputStream("Content 1".getBytes()),
                        "text/plain",
                        9L
                ),
                "file2.json", new FileData(
                        new ByteArrayInputStream("{\"test\": true}".getBytes()),
                        "application/json",
                        14L
                )
        );

        directoryManager.addFilesToDirectory(directoryPath, files);

        List<String> filesInDirectory = fileStorage.listFiles(directoryPath + "/");
        assertThat(filesInDirectory).hasSize(3);
        assertThat(filesInDirectory).anyMatch(f -> f.endsWith("file1.txt"));
        assertThat(filesInDirectory).anyMatch(f -> f.endsWith("file2.json"));
        assertThat(filesInDirectory).anyMatch(f -> f.endsWith(".directory"));
    }

    @Test
    @DisplayName("Should create ZIP from directory")
    void shouldCreateZipFromDirectory() throws IOException {
        String directoryPath = "zip-test/project";

        directoryManager.createDirectory(directoryPath);

        Map<String, FileData> files = Map.of(
                "readme.txt", new FileData(
                        new ByteArrayInputStream("Project README".getBytes()),
                        "text/plain",
                        14L
                ),
                "config.json", new FileData(
                        new ByteArrayInputStream("{\"version\": \"1.0\"}".getBytes()),
                        "application/json",
                        18L
                )
        );

        directoryManager.addFilesToDirectory(directoryPath, files);

        try (InputStream zipStream = zipService.createZipFromDirectory(directoryPath);
             ZipInputStream zis = new ZipInputStream(zipStream)) {

            ZipEntry entry1 = zis.getNextEntry();
            assertThat(entry1).isNotNull();
            assertThat(entry1.getName()).isIn("readme.txt", "config.json");

            ZipEntry entry2 = zis.getNextEntry();
            assertThat(entry2).isNotNull();
            assertThat(entry2.getName()).isIn("readme.txt", "config.json");
            assertThat(entry2.getName()).isNotEqualTo(entry1.getName());

            ZipEntry entry3 = zis.getNextEntry();
            assertThat(entry3).isNull();
        }
    }

    @Test
    @DisplayName("Should upload zipped directory to S3")
    void shouldUploadZippedDirectoryToS3() throws IOException {
        String directoryPath = "upload-zip-test/project";
        String zipFileName = "project-archive";

        directoryManager.createDirectory(directoryPath);

        Map<String, FileData> files = Map.of(
                "test-file.txt", new FileData(
                        new ByteArrayInputStream("Test file content".getBytes()),
                        "text/plain",
                        17L
                )
        );

        directoryManager.addFilesToDirectory(directoryPath, files);

        String zipKey = zipService.uploadZippedDirectory(directoryPath, zipFileName);

        assertThat(zipKey).isNotNull();
        assertThat(zipKey).contains("archives/");
        assertThat(zipKey).contains("project-archive.zip");

        assertThat(fileStorage.fileExists(zipKey)).isTrue();

        try (InputStream downloadedZip = fileStorage.downloadFile(zipKey);
             ZipInputStream zis = new ZipInputStream(downloadedZip)) {

            ZipEntry entry = zis.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test-file.txt");

            byte[] entryContent = zis.readAllBytes();
            assertThat(new String(entryContent)).isEqualTo("Test file content");
        }
    }

    @Test
    @DisplayName("Should delete directory and all its files")
    void shouldDeleteDirectoryAndAllFiles() throws IOException {
        String directoryPath = "delete-test/project";

        directoryManager.createDirectory(directoryPath);

        Map<String, FileData> files = Map.of(
                "file-to-delete.txt", new FileData(
                        new ByteArrayInputStream("Content to delete".getBytes()),
                        "text/plain",
                        17L
                )
        );

        directoryManager.addFilesToDirectory(directoryPath, files);

        assertThat(directoryManager.directoryExists(directoryPath)).isTrue();
        assertThat(fileStorage.listFiles(directoryPath + "/")).hasSize(2);

        directoryManager.deleteDirectory(directoryPath);

        assertThat(fileStorage.listFiles(directoryPath + "/")).isEmpty();
    }

    @Test
    @DisplayName("Should handle file metadata correctly")
    void shouldHandleFileMetadataCorrectly() throws IOException {
        String key = "metadata-test/file.txt";
        String content = "File with metadata";
        String contentType = "text/plain";

        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes())) {
            fileStorage.uploadFile(inputStream, key, contentType, content.length());
        }

        var metadata = fileStorage.getFileMetadata(key);

        assertThat(metadata.key()).isEqualTo(key);
        assertThat(metadata.contentType()).isEqualTo(contentType);
        assertThat(metadata.contentLength()).isEqualTo(content.length());
        assertThat(metadata.lastModified()).isNotNull();
        assertThat(metadata.userMetadata()).containsKey("uploaded-at");
    }

    @Test
    @DisplayName("Should list files with prefix correctly")
    void shouldListFilesWithPrefixCorrectly() throws IOException {
        String prefix = "list-test/";

        try (InputStream stream1 = new ByteArrayInputStream("Content 1".getBytes())) {
            fileStorage.uploadFile(stream1, prefix + "file1.txt", "text/plain", 9L);
        }

        try (InputStream stream2 = new ByteArrayInputStream("Content 2".getBytes())) {
            fileStorage.uploadFile(stream2, prefix + "file2.txt", "text/plain", 9L);
        }

        try (InputStream stream3 = new ByteArrayInputStream("Content 3".getBytes())) {
            fileStorage.uploadFile(stream3, "other/file3.txt", "text/plain", 9L);
        }

        List<String> filesWithPrefix = fileStorage.listFiles(prefix);
        List<String> allFiles = fileStorage.listFiles(null);

        assertThat(filesWithPrefix).hasSize(2);
        assertThat(filesWithPrefix).allMatch(key -> key.startsWith(prefix));

        assertThat(allFiles).hasSize(3);
    }

    @Test
    @DisplayName("Should handle nested directory structure")
    void shouldHandleNestedDirectoryStructure() throws IOException {
        String baseDir = "nested-test";
        String subDir1 = baseDir + "/level1";
        String subDir2 = subDir1 + "/level2";

        directoryManager.createDirectory(subDir2);

        Map<String, FileData> files = Map.of(
                "deep-file.txt", new FileData(
                        new ByteArrayInputStream("Deep nested file".getBytes()),
                        "text/plain",
                        16L
                )
        );

        directoryManager.addFilesToDirectory(subDir2, files);

        assertThat(directoryManager.directoryExists(subDir2)).isTrue();

        List<String> allFiles = fileStorage.listFiles(baseDir + "/");
        assertThat(allFiles).hasSize(1);
        assertThat(allFiles.get(0)).contains("level1/level2");

        try (InputStream zipStream = zipService.createZipFromDirectory(subDir2);
             ZipInputStream zis = new ZipInputStream(zipStream)) {

            ZipEntry entry = zis.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("deep-file.txt");
        }
    }
}