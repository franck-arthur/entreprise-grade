package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.FileStorage;
import com.enterprise.app.domain.storage.FileWorkflowManager;
import com.enterprise.app.domain.storage.LocalFileManager;
import com.enterprise.app.testing.config.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileWorkflowManagerImplTest extends BaseUnitTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileStorage fileStorage;

    private LocalFileManager localFileManager;
    private FileWorkflowManagerImpl workflowManager;

    @Override
    protected Object[] getAllMocks() {
        return new Object[]{fileStorage};
    }

    @BeforeEach
    void setUp() {
        localFileManager = new LocalFileManagerImpl(tempDir.toString());
        workflowManager = new FileWorkflowManagerImpl(localFileManager, fileStorage);
    }

    @Test
    void shouldCreateFileJob() {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt", "file2.csv");

        // When
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        // Then
        assertThat(job.jobId()).isNotNull();
        assertThat(job.projectName()).isEqualTo(projectName);
        assertThat(job.fileNames()).isEqualTo(fileNames);
        assertThat(job.status()).isEqualTo(FileWorkflowManager.FileJobStatus.CREATED);
        assertThat(job.localDirectory()).exists().isDirectory();

        // Vérifier que les fichiers vides ont été créés
        for (String fileName : fileNames) {
            Path filePath = job.localDirectory().resolve(fileName);
            assertThat(filePath).exists().isRegularFile();
            assertThat(localFileManager.isFileEmpty(filePath.toString())).isTrue();
        }
    }

    @Test
    void shouldDetectFilesNotReady() {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt", "file2.csv");
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        // When
        boolean filesReady = workflowManager.areFilesReady(job.jobId());

        // Then
        assertThat(filesReady).isFalse();
    }

    @Test
    void shouldDetectFilesReady() throws IOException {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt", "file2.csv");
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        // Remplir les fichiers avec du contenu
        for (String fileName : fileNames) {
            Path filePath = job.localDirectory().resolve(fileName);
            Files.writeString(filePath, "Some content for " + fileName);
        }

        // When
        boolean filesReady = workflowManager.areFilesReady(job.jobId());

        // Then
        assertThat(filesReady).isTrue();

        // Vérifier que le status a été mis à jour
        FileWorkflowManager.FileJob updatedJob = workflowManager.getJobInfo(job.jobId());
        assertThat(updatedJob.status()).isEqualTo(FileWorkflowManager.FileJobStatus.FILES_READY);
    }

    @Test
    void shouldUploadFilesToS3() throws IOException {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt", "file2.csv");
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        // Remplir les fichiers avec du contenu
        for (String fileName : fileNames) {
            Path filePath = job.localDirectory().resolve(fileName);
            Files.writeString(filePath, "Content for " + fileName);
        }

        // Marquer les fichiers comme prêts
        workflowManager.areFilesReady(job.jobId());

        // Mock des uploads S3
        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
            .thenReturn("s3-key-1", "s3-key-2");

        // When
        List<String> uploadedKeys = workflowManager.uploadFilesToS3(job.jobId());

        // Then
        assertThat(uploadedKeys).hasSize(2);
        assertThat(uploadedKeys).containsExactlyInAnyOrder("s3-key-1", "s3-key-2");

        // Vérifier que le status a été mis à jour
        FileWorkflowManager.FileJob updatedJob = workflowManager.getJobInfo(job.jobId());
        assertThat(updatedJob.status()).isEqualTo(FileWorkflowManager.FileJobStatus.UPLOADED_TO_S3);

        // Vérifier les appels à S3
        verify(fileStorage, times(2)).uploadFile(
            any(InputStream.class),
            anyString(),
            anyString(),
            anyLong()
        );
    }

    @Test
    void shouldFailToUploadIfFilesNotReady() {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt");
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        // When & Then
        assertThatThrownBy(() -> workflowManager.uploadFilesToS3(job.jobId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Files are not ready for job: " + job.jobId());
    }

    @Test
    void shouldCleanupLocalFiles() throws IOException {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt");
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        // Préparer le job pour le nettoyage
        Path filePath = job.localDirectory().resolve("file1.txt");
        Files.writeString(filePath, "Content");
        workflowManager.areFilesReady(job.jobId());

        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
            .thenReturn("s3-key");
        workflowManager.uploadFilesToS3(job.jobId());

        // When
        workflowManager.cleanupLocalFiles(job.jobId());

        // Then
        assertThat(job.localDirectory()).doesNotExist();

        // Vérifier que le status a été mis à jour
        FileWorkflowManager.FileJob updatedJob = workflowManager.getJobInfo(job.jobId());
        assertThat(updatedJob.status()).isEqualTo(FileWorkflowManager.FileJobStatus.CLEANED_UP);
    }

    @Test
    void shouldProcessCompleteWorkflow() throws IOException {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt", "file2.csv");
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        // Remplir les fichiers avec du contenu
        for (String fileName : fileNames) {
            Path filePath = job.localDirectory().resolve(fileName);
            Files.writeString(filePath, "Content for " + fileName);
        }

        // Mock des uploads S3
        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
            .thenReturn("s3-key-1", "s3-key-2");

        // When
        List<String> uploadedKeys = workflowManager.processCompleteWorkflow(job.jobId());

        // Then
        assertThat(uploadedKeys).hasSize(2);

        // Vérifier que le workflow complet s'est bien déroulé
        FileWorkflowManager.FileJob finalJob = workflowManager.getJobInfo(job.jobId());
        assertThat(finalJob.status()).isEqualTo(FileWorkflowManager.FileJobStatus.CLEANED_UP);
        assertThat(finalJob.localDirectory()).doesNotExist();
    }

    @Test
    void shouldFailCompleteWorkflowIfFilesNotReady() {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt");
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        // When & Then
        assertThatThrownBy(() -> workflowManager.processCompleteWorkflow(job.jobId()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to process complete ");
    }

    @Test
    void shouldDeleteJob() {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt");
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        Path localDirectory = job.localDirectory();
        assertThat(localDirectory).exists();

        // When
        workflowManager.deleteJob(job.jobId());

        // Then
        assertThat(localDirectory).doesNotExist();

        assertThatThrownBy(() -> workflowManager.getJobInfo(job.jobId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Job not found: " + job.jobId());
    }

    @Test
    void shouldNotFailWhenDeletingNonExistingJob() {
        // When & Then
        assertThatCode(() -> workflowManager.deleteJob("non-existing-job-id"))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldListActiveJobs() {
        // Given
        FileWorkflowManager.FileJob job1 = workflowManager.createFileJob("project1", Set.of("file1.txt"));
        FileWorkflowManager.FileJob job2 = workflowManager.createFileJob("project2", Set.of("file2.txt"));

        // When
        List<FileWorkflowManager.FileJob> activeJobs = workflowManager.listActiveJobs();

        // Then
        assertThat(activeJobs).hasSize(2);
        assertThat(activeJobs).extracting(FileWorkflowManager.FileJob::jobId)
            .containsExactlyInAnyOrder(job1.jobId(), job2.jobId());
    }

    @Test
    void shouldThrowExceptionForNonExistingJob() {
        // When & Then
        assertThatThrownBy(() -> workflowManager.getJobInfo("non-existing-job-id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Job not found: non-existing-job-id");
    }

    @Test
    void shouldHandleS3UploadFailure() throws IOException {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt");
        FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);

        // Préparer le fichier
        Path filePath = job.localDirectory().resolve("file1.txt");
        Files.writeString(filePath, "Content");
        workflowManager.areFilesReady(job.jobId());

        // Mock d'échec S3
        when(fileStorage.uploadFile(any(InputStream.class), anyString(), anyString(), anyLong()))
            .thenThrow(new RuntimeException("S3 upload failed"));

        // When & Then
        assertThatThrownBy(() -> workflowManager.uploadFilesToS3(job.jobId()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to upload files to S3");

        // Vérifier que le job est marqué comme échoué
        FileWorkflowManager.FileJob failedJob = workflowManager.getJobInfo(job.jobId());
        assertThat(failedJob.status()).isEqualTo(FileWorkflowManager.FileJobStatus.FAILED);
    }

    @Test
    void shouldGenerateUniqueJobIds() {
        // Given
        Set<String> jobIds = Set.of();

        // When
        for (int i = 0; i < 10; i++) {
            FileWorkflowManager.FileJob job = workflowManager.createFileJob("project" + i, Set.of("file.txt"));
            jobIds = Set.copyOf(List.of(jobIds.toArray(new String[0])).stream()
                .collect(java.util.stream.Collectors.toSet()));
            jobIds.add(job.jobId());
        }

        // Then
        assertThat(jobIds).hasSize(10); // Tous les IDs doivent être uniques
    }
}