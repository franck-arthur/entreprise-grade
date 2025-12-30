package com.enterprise.app.application.service;

import com.enterprise.app.domain.storage.DirectoryManager;
import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.domain.storage.FileWorkflowManager;
import com.enterprise.app.domain.storage.LocalFileManager;
import com.enterprise.app.domain.storage.ZipService;
import com.enterprise.app.infrastructure.config.S3Config;
import com.enterprise.app.infrastructure.storage.FileWorkflowManagerImpl;
import com.enterprise.app.infrastructure.storage.LocalFileManagerImpl;
import com.enterprise.app.infrastructure.storage.S3DirectoryManager;
import com.enterprise.app.infrastructure.storage.S3FileStorage;
import com.enterprise.app.infrastructure.storage.ZipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest
@Testcontainers
@DisplayName("Tests d'intégration EnhancedStorageService")
class EnhancedStorageServiceIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:2.3.0"))
            .withServices(S3)
            .withEnv("DEBUG", "1");

    private EnhancedStorageService enhancedStorageService;
    private S3Client s3Client;

    private static final String BUCKET_NAME = "integration-test-bucket";

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

        FileStorage fileStorage = new S3FileStorage(s3Client, s3Properties);
        DirectoryManager directoryManager = new S3DirectoryManager(fileStorage);
        ZipService zipService = new ZipServiceImpl(fileStorage);
        LocalFileManager localFileManager = new LocalFileManagerImpl("/tmp/enterprise-app-test");
        FileWorkflowManager workflowManager = new FileWorkflowManagerImpl(localFileManager, fileStorage);

        enhancedStorageService = new EnhancedStorageService(fileStorage, directoryManager, zipService, workflowManager, localFileManager);
    }

    @Test
    @DisplayName("Doit créer un répertoire avec plusieurs fichiers avec succès")
    void shouldCreateDirectoryWithMultipleFilesSuccessfully() {
        String projectName = "integration-test-project";
        MockMultipartFile file1 = new MockMultipartFile(
                "file1", "readme.txt", "text/plain", "# Project README".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file2", "config.json", "application/json", "{\"version\": \"1.0\"}".getBytes());
        MockMultipartFile file3 = new MockMultipartFile(
                "file3", "script.py", "text/x-python", "print('Hello World')".getBytes());

        Map<String, MockMultipartFile> files = Map.of(
                "readme.txt", file1,
                "config.json", file2,
                "script.py", file3
        );

        String directoryPath = enhancedStorageService.createDirectoryWithFiles(projectName, Map.copyOf(files));

        assertThat(directoryPath).isNotNull();
        assertThat(directoryPath).startsWith("projects/");
        assertThat(directoryPath).contains("integration-test-project");

        try (InputStream downloadedFile = enhancedStorageService.downloadFile(directoryPath + "/readme.txt")) {
            String content = new String(downloadedFile.readAllBytes());
            assertThat(content).isEqualTo("# Project README");
        } catch (IOException e) {
            fail("Failed to download file", e);
        }
    }

    @Test
    @DisplayName("Doit créer, remplir et zipper un répertoire en une seule opération")
    void shouldCreatePopulateAndZipDirectoryInOneOperation() throws IOException {
        String projectName = "zip-integration-project";
        String zipFileName = "complete-project-archive";

        MockMultipartFile sourceFile = new MockMultipartFile(
                "source", "main.java", "text/x-java-source",
                "public class Main { public static void main(String[] args) {} }".getBytes());
        MockMultipartFile docFile = new MockMultipartFile(
                "doc", "documentation.md", "text/markdown",
                "# Documentation\nThis is the project documentation.".getBytes());

        Map<String, MockMultipartFile> files = Map.of(
                "src/main.java", sourceFile,
                "docs/documentation.md", docFile
        );

        String zipKey = enhancedStorageService.createAndZipDirectory(projectName, Map.copyOf(files), zipFileName);

        assertThat(zipKey).isNotNull();
        assertThat(zipKey).contains("archives/");
        assertThat(zipKey).endsWith("complete-project-archive.zip");

        try (InputStream zipStream = enhancedStorageService.downloadFile(zipKey);
             ZipInputStream zis = new ZipInputStream(zipStream)) {

            ZipEntry entry1 = zis.getNextEntry();
            assertThat(entry1).isNotNull();

            ZipEntry entry2 = zis.getNextEntry();
            assertThat(entry2).isNotNull();

            String[] entryNames = {entry1.getName(), entry2.getName()};
            assertThat(entryNames).containsExactlyInAnyOrder("src/main.java", "docs/documentation.md");
        }
    }

    @Test
    @DisplayName("Doit ajouter des fichiers à un répertoire existant")
    void shouldAddFilesToExistingDirectory() throws IOException {
        String projectName = "expandable-project";
        MockMultipartFile initialFile = new MockMultipartFile(
                "initial", "initial.txt", "text/plain", "Initial content".getBytes());

        Map<String, MockMultipartFile> initialFiles = Map.of("initial.txt", initialFile);
        String directoryPath = enhancedStorageService.createDirectoryWithFiles(projectName, Map.copyOf(initialFiles));

        MockMultipartFile additionalFile = new MockMultipartFile(
                "additional", "additional.txt", "text/plain", "Additional content".getBytes());

        String fileKey = enhancedStorageService.addFileToExistingDirectory(
                directoryPath, "additional.txt", additionalFile);

        assertThat(fileKey).isEqualTo(directoryPath + "/additional.txt");

        try (InputStream downloadedFile = enhancedStorageService.downloadFile(fileKey)) {
            String content = new String(downloadedFile.readAllBytes());
            assertThat(content).isEqualTo("Additional content");
        }
    }

    @Test
    @DisplayName("Doit créer un ZIP à partir d'un répertoire existant")
    void shouldCreateZipFromExistingDirectory() throws IOException {
        String projectName = "existing-project";
        MockMultipartFile file1 = new MockMultipartFile(
                "file1", "component.js", "application/javascript", "export default {}".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file2", "style.css", "text/css", "body { margin: 0; }".getBytes());

        Map<String, MockMultipartFile> files = Map.of(
                "component.js", file1,
                "style.css", file2
        );

        String directoryPath = enhancedStorageService.createDirectoryWithFiles(projectName, Map.copyOf(files));

        try (InputStream zipStream = enhancedStorageService.downloadZippedDirectory(directoryPath);
             ZipInputStream zis = new ZipInputStream(zipStream)) {

            ZipEntry entry1 = zis.getNextEntry();
            assertThat(entry1).isNotNull();
            String content1 = new String(zis.readAllBytes());
            zis.closeEntry();

            ZipEntry entry2 = zis.getNextEntry();
            assertThat(entry2).isNotNull();
            String content2 = new String(zis.readAllBytes());
            zis.closeEntry();

            if (entry1.getName().equals("component.js")) {
                assertThat(content1).isEqualTo("export default {}");
                assertThat(entry2.getName()).isEqualTo("style.css");
                assertThat(content2).isEqualTo("body { margin: 0; }");
            } else {
                assertThat(entry1.getName()).isEqualTo("style.css");
                assertThat(content1).isEqualTo("body { margin: 0; }");
                assertThat(entry2.getName()).isEqualTo("component.js");
                assertThat(content2).isEqualTo("export default {}");
            }
        }
    }

    @Test
    @DisplayName("Doit uploader un fichier unique avec organisation par dossier")
    void shouldUploadSingleFileWithFolderOrganization() throws IOException {
        MockMultipartFile singleFile = new MockMultipartFile(
                "single", "document.pdf", "application/pdf", "PDF content".getBytes());

        String fileKey = enhancedStorageService.uploadSingleFile(singleFile, "documents");

        assertThat(fileKey).isNotNull();
        assertThat(fileKey).startsWith("documents/");
        assertThat(fileKey).contains("document.pdf");

        try (InputStream downloadedFile = enhancedStorageService.downloadFile(fileKey)) {
            String content = new String(downloadedFile.readAllBytes());
            assertThat(content).isEqualTo("PDF content");
        }
    }

    @Test
    @DisplayName("Doit gérer un workflow complet - créer, ajouter, zipper, supprimer")
    void shouldHandleCompleteWorkflow() throws IOException {
        String projectName = "workflow-test-project";

        MockMultipartFile initialFile = new MockMultipartFile(
                "init", "app.js", "application/javascript", "console.log('Hello');".getBytes());
        Map<String, MockMultipartFile> initialFiles = Map.of("app.js", initialFile);

        String directoryPath = enhancedStorageService.createDirectoryWithFiles(projectName, Map.copyOf(initialFiles));

        MockMultipartFile configFile = new MockMultipartFile(
                "config", "package.json", "application/json", "{\"name\": \"test\"}".getBytes());
        enhancedStorageService.addFileToExistingDirectory(directoryPath, "package.json", configFile);

        String zipKey = enhancedStorageService.uploadZippedDirectory(directoryPath, "final-archive.zip");
        assertThat(zipKey).contains("final-archive.zip");

        try (InputStream zipStream = enhancedStorageService.downloadFile(zipKey);
             ZipInputStream zis = new ZipInputStream(zipStream)) {

            int entryCount = 0;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                assertThat(entry.getName()).isIn("app.js", "package.json");
                zis.closeEntry();
            }
            assertThat(entryCount).isEqualTo(2);
        }

        assertThatCode(() -> enhancedStorageService.deleteDirectory(directoryPath))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Doit gérer les caractères spéciaux dans les noms de fichiers")
    void shouldHandleSpecialCharactersInFilenames() throws IOException {
        String projectName = "special-chars-project";
        MockMultipartFile fileWithSpecialName = new MockMultipartFile(
                "special", "file with spaces & symbols!@#.txt", "text/plain", "content".getBytes());

        Map<String, MockMultipartFile> files = Map.of("file with spaces & symbols!@#.txt", fileWithSpecialName);
        String directoryPath = enhancedStorageService.createDirectoryWithFiles(projectName, Map.copyOf(files));

        String zipKey = enhancedStorageService.createAndZipDirectory(
                projectName + "-copy", Map.copyOf(files), "archive with spaces!@#.zip");

        assertThat(zipKey).contains("archive_with_spaces___");
        assertThat(zipKey).endsWith(".zip");

        try (InputStream zipStream = enhancedStorageService.downloadFile(zipKey);
             ZipInputStream zis = new ZipInputStream(zipStream)) {

            ZipEntry entry = zis.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("file with spaces & symbols!@#.txt");
        }
    }

    @Test
    @DisplayName("Doit lancer une exception lors de l'ajout à un répertoire inexistant")
    void shouldThrowExceptionWhenAddingToNonExistentDirectory() {
        String nonExistentPath = "projects/2024/12/27/non-existent-directory";
        MockMultipartFile file = new MockMultipartFile(
                "test", "test.txt", "text/plain", "content".getBytes());

        assertThatThrownBy(() ->
                enhancedStorageService.addFileToExistingDirectory(nonExistentPath, "test.txt", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Directory does not exist");
    }

    @Test
    @DisplayName("Doit gérer les uploads de fichiers vides")
    void shouldHandleEmptyFileUploads() {
        String projectName = "empty-files-project";
        MockMultipartFile emptyFile = new MockMultipartFile(
                "empty", "empty.txt", "text/plain", new byte[0]);

        Map<String, MockMultipartFile> files = Map.of("empty.txt", emptyFile);

        assertThatCode(() -> enhancedStorageService.createDirectoryWithFiles(projectName, Map.copyOf(files)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Doit générer des chemins de répertoire uniques pour le même nom de projet")
    void shouldGenerateUniqueDirectoryPathsForSameProjectName() {
        String projectName = "duplicate-name-project";
        MockMultipartFile file = new MockMultipartFile(
                "test", "test.txt", "text/plain", "content".getBytes());
        Map<String, MockMultipartFile> files = Map.of("test.txt", file);

        String path1 = enhancedStorageService.createDirectoryWithFiles(projectName, Map.copyOf(files));
        String path2 = enhancedStorageService.createDirectoryWithFiles(projectName, Map.copyOf(files));

        assertThat(path1).isNotEqualTo(path2);
        assertThat(path1).startsWith("projects/");
        assertThat(path2).startsWith("projects/");
        assertThat(path1).contains(projectName);
        assertThat(path2).contains(projectName);
    }
}