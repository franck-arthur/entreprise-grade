package com.enterprise.app.application.service;

import com.enterprise.app.domain.storage.*;
import com.enterprise.app.testing.config.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnhancedStorageServiceWorkflowTest extends BaseUnitTest {

    @Mock
    private FileStorage fileStorage;

    @Mock
    private DirectoryManager directoryManager;

    @Mock
    private ZipService zipService;

    @Mock
    private FileWorkflowManager workflowManager;

    @Mock
    private LocalFileManager localFileManager;

    private EnhancedStorageService enhancedStorageService;

    @Override
    protected Object[] getAllMocks() {
        return new Object[]{
            fileStorage,
            directoryManager,
            zipService,
            workflowManager,
            localFileManager
        };
    }

    @BeforeEach
    void setUp() {
        enhancedStorageService = new EnhancedStorageService(
            fileStorage,
            directoryManager,
            zipService,
            workflowManager,
            localFileManager
        );
    }

    @Test
    @DisplayName("Doit créer un job de fichiers avec des fichiers vides")
    void shouldCreateFileJobWithEmptyFiles() {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt", "file2.csv");

        FileWorkflowManager.FileJob expectedJob = new FileWorkflowManager.FileJob(
            "job-123",
            projectName,
            Paths.get("/tmp/test"),
            fileNames,
            FileWorkflowManager.FileJobStatus.CREATED
        );

        when(workflowManager.createFileJob(projectName, fileNames)).thenReturn(expectedJob);

        // When
        FileWorkflowManager.FileJob result = enhancedStorageService.createFileJobWithEmptyFiles(projectName, fileNames);

        // Then
        assertThat(result).isEqualTo(expectedJob);
        verify(workflowManager).createFileJob(projectName, fileNames);
    }

    @Test
    @DisplayName("Doit vérifier si les fichiers du job sont prêts")
    void shouldCheckIfJobFilesAreReady() {
        // Given
        String jobId = "job-123";
        when(workflowManager.areFilesReady(jobId)).thenReturn(true);

        // When
        boolean result = enhancedStorageService.areJobFilesReady(jobId);

        // Then
        assertThat(result).isTrue();
        verify(workflowManager).areFilesReady(jobId);
    }

    @Test
    @DisplayName("Doit uploader les fichiers du job vers S3")
    void shouldUploadJobFilesToS3() {
        // Given
        String jobId = "job-123";
        List<String> expectedKeys = List.of("s3-key-1", "s3-key-2");

        when(workflowManager.uploadFilesToS3(jobId)).thenReturn(expectedKeys);

        // When
        List<String> result = enhancedStorageService.uploadJobFilesToS3(jobId);

        // Then
        assertThat(result).isEqualTo(expectedKeys);
        verify(workflowManager).uploadFilesToS3(jobId);
    }

    @Test
    @DisplayName("Doit nettoyer les fichiers locaux du job")
    void shouldCleanupJobLocalFiles() {
        // Given
        String jobId = "job-123";

        // When
        enhancedStorageService.cleanupJobLocalFiles(jobId);

        // Then
        verify(workflowManager).cleanupLocalFiles(jobId);
    }

    @Test
    @DisplayName("Doit traiter le workflow complet")
    void shouldProcessCompleteWorkflow() {
        // Given
        String jobId = "job-123";
        List<String> expectedKeys = List.of("s3-key-1", "s3-key-2");

        when(workflowManager.processCompleteWorkflow(jobId)).thenReturn(expectedKeys);

        // When
        List<String> result = enhancedStorageService.processCompleteWorkflow(jobId);

        // Then
        assertThat(result).isEqualTo(expectedKeys);
        verify(workflowManager).processCompleteWorkflow(jobId);
    }

    @Test
    @DisplayName("Doit récupérer les informations du job")
    void shouldGetJobInfo() {
        // Given
        String jobId = "job-123";
        FileWorkflowManager.FileJob expectedJob = new FileWorkflowManager.FileJob(
            jobId,
            "test-project",
            Paths.get("/tmp/test"),
            Set.of("file.txt"),
            FileWorkflowManager.FileJobStatus.CREATED
        );

        when(workflowManager.getJobInfo(jobId)).thenReturn(expectedJob);

        // When
        FileWorkflowManager.FileJob result = enhancedStorageService.getJobInfo(jobId);

        // Then
        assertThat(result).isEqualTo(expectedJob);
        verify(workflowManager).getJobInfo(jobId);
    }

    @Test
    @DisplayName("Doit supprimer un job")
    void shouldDeleteJob() {
        // Given
        String jobId = "job-123";

        // When
        enhancedStorageService.deleteJob(jobId);

        // Then
        verify(workflowManager).deleteJob(jobId);
    }

    @Test
    @DisplayName("Doit lister les jobs actifs")
    void shouldListActiveJobs() {
        // Given
        List<FileWorkflowManager.FileJob> expectedJobs = List.of(
            new FileWorkflowManager.FileJob(
                "job-1",
                "project-1",
                Paths.get("/tmp/test1"),
                Set.of("file1.txt"),
                FileWorkflowManager.FileJobStatus.CREATED
            ),
            new FileWorkflowManager.FileJob(
                "job-2",
                "project-2",
                Paths.get("/tmp/test2"),
                Set.of("file2.txt"),
                FileWorkflowManager.FileJobStatus.FILES_READY
            )
        );

        when(workflowManager.listActiveJobs()).thenReturn(expectedJobs);

        // When
        List<FileWorkflowManager.FileJob> result = enhancedStorageService.listActiveJobs();

        // Then
        assertThat(result).isEqualTo(expectedJobs);
        verify(workflowManager).listActiveJobs();
    }

    @Test
    @DisplayName("Doit récupérer le répertoire de travail temporaire")
    void shouldGetTempWorkingDirectory() {
        // Given
        Path tempDir = Paths.get("/tmp/enterprise-app");
        when(localFileManager.getTempWorkingDirectory()).thenReturn(tempDir);

        // When
        String result = enhancedStorageService.getTempWorkingDirectory();

        // Then
        assertThat(result).isEqualTo(tempDir.toString());
        verify(localFileManager).getTempWorkingDirectory();
    }

    @Test
    @DisplayName("Doit gérer l'exception lors de la création du job de fichiers")
    void shouldHandleExceptionInCreateFileJob() {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file.txt");

        when(workflowManager.createFileJob(projectName, fileNames))
            .thenThrow(new RuntimeException("Creation failed"));

        // When & Then
        assertThatThrownBy(() -> enhancedStorageService.createFileJobWithEmptyFiles(projectName, fileNames))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to create file job");
    }

    @Test
    @DisplayName("Doit gérer l'exception lors de l'upload des fichiers vers S3")
    void shouldHandleExceptionInUploadFilesToS3() {
        // Given
        String jobId = "job-123";

        when(workflowManager.uploadFilesToS3(jobId))
            .thenThrow(new RuntimeException("Upload failed"));

        // When & Then
        assertThatThrownBy(() -> enhancedStorageService.uploadJobFilesToS3(jobId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to upload job files to S3");
    }

    @Test
    @DisplayName("Doit gérer l'exception lors du nettoyage des fichiers locaux")
    void shouldHandleExceptionInCleanupLocalFiles() {
        // Given
        String jobId = "job-123";

        doThrow(new RuntimeException("Cleanup failed"))
            .when(workflowManager).cleanupLocalFiles(jobId);

        // When & Then
        assertThatThrownBy(() -> enhancedStorageService.cleanupJobLocalFiles(jobId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to cleanup local files");
    }

    @Test
    @DisplayName("Doit gérer l'exception lors du traitement du workflow complet")
    void shouldHandleExceptionInProcessCompleteWorkflow() {
        // Given
        String jobId = "job-123";

        when(workflowManager.processCompleteWorkflow(jobId))
            .thenThrow(new RuntimeException("Workflow failed"));

        // When & Then
        assertThatThrownBy(() -> enhancedStorageService.processCompleteWorkflow(jobId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to process complete workflow");
    }

    @Test
    @DisplayName("Doit gérer l'exception lors de la suppression du job")
    void shouldHandleExceptionInDeleteJob() {
        // Given
        String jobId = "job-123";

        doThrow(new RuntimeException("Delete failed"))
            .when(workflowManager).deleteJob(jobId);

        // When & Then
        assertThatThrownBy(() -> enhancedStorageService.deleteJob(jobId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to delete job");
    }

}