package com.enterprise.app.infrastructure.batch;

import com.enterprise.app.application.service.FileWorkflowBatchService;
import com.enterprise.app.domain.storage.FileWorkflowManager;
import com.enterprise.app.infrastructure.batch.job.FileWorkflowJobConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests d'intégration pour le workflow complet Spring Batch
 */
@SpringBatchTest
@SpringBootTest(classes = {
        BatchConfiguration.class,
        FileWorkflowJobConfiguration.class,
        FileWorkflowBatchService.class
})
@ActiveProfiles("test")
@Transactional
class FileWorkflowBatchIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private FileWorkflowBatchService fileWorkflowBatchService;

    @MockBean
    private FileWorkflowManager fileWorkflowManager;

    @Test
    void testCompleteFileWorkflowJobSuccess() throws Exception {
        // Given
        String projectName = "integration-test-project";
        Set<String> fileNames = Set.of("test1.txt", "test2.csv", "test3.json");

        FileWorkflowManager.FileJob mockFileJob = new FileWorkflowManager.FileJob(
                "integration-job-id-123",
                projectName,
                Paths.get("/tmp/integration-test"),
                fileNames,
                FileWorkflowManager.FileJobStatus.CREATED
        );

        List<String> expectedUploadedKeys = List.of(
                "projects/2024/01/01/integration-test-project-12345678/test1.txt",
                "projects/2024/01/01/integration-test-project-12345678/test2.csv",
                "projects/2024/01/01/integration-test-project-12345678/test3.json"
        );

        // Configuration des mocks
        when(fileWorkflowManager.createFileJob(eq(projectName), eq(fileNames)))
                .thenReturn(mockFileJob);
        when(fileWorkflowManager.areFilesReady(eq("integration-job-id-123")))
                .thenReturn(true);
        when(fileWorkflowManager.uploadFilesToS3(eq("integration-job-id-123")))
                .thenReturn(expectedUploadedKeys);
        doNothing().when(fileWorkflowManager).cleanupLocalFiles(eq("integration-job-id-123"));

        // When
        JobExecution jobExecution = fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames);

        // Then
        assertNotNull(jobExecution);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Vérifier que toutes les étapes ont été exécutées dans l'ordre
        verify(fileWorkflowManager).createFileJob(projectName, fileNames);
        verify(fileWorkflowManager).areFilesReady("integration-job-id-123");
        verify(fileWorkflowManager).uploadFilesToS3("integration-job-id-123");
        verify(fileWorkflowManager).cleanupLocalFiles("integration-job-id-123");

        // Vérifier que le contexte d'exécution contient les bonnes informations
        String fileJobId = jobExecution.getExecutionContext().getString("fileJobId");
        assertEquals("integration-job-id-123", fileJobId);

        String uploadedKeys = jobExecution.getExecutionContext().getString("uploadedS3Keys");
        assertEquals(String.join(",", expectedUploadedKeys), uploadedKeys);
    }

    @Test
    void testFileWorkflowJobFailureOnFileNotReady() throws Exception {
        // Given
        String projectName = "failing-test-project";
        Set<String> fileNames = Set.of("fail.txt");

        FileWorkflowManager.FileJob mockFileJob = new FileWorkflowManager.FileJob(
                "failing-job-id-456",
                projectName,
                Paths.get("/tmp/failing-test"),
                fileNames,
                FileWorkflowManager.FileJobStatus.CREATED
        );

        // Configuration des mocks - les fichiers ne sont jamais prêts
        when(fileWorkflowManager.createFileJob(eq(projectName), eq(fileNames)))
                .thenReturn(mockFileJob);
        when(fileWorkflowManager.areFilesReady(eq("failing-job-id-456")))
                .thenReturn(false); // Les fichiers ne sont jamais prêts

        // When
        JobExecution jobExecution = fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames);

        // Then
        assertNotNull(jobExecution);
        assertEquals(BatchStatus.FAILED, jobExecution.getStatus());

        // Vérifier que seules les premières étapes ont été exécutées
        verify(fileWorkflowManager).createFileJob(projectName, fileNames);
        verify(fileWorkflowManager, times(30)).areFilesReady("failing-job-id-456"); // Max tentatives
        verifyNoMoreInteractions(fileWorkflowManager);
    }

    @Test
    void testFileWorkflowJobFailureOnS3Upload() throws Exception {
        // Given
        String projectName = "s3-fail-project";
        Set<String> fileNames = Set.of("s3fail.txt");

        FileWorkflowManager.FileJob mockFileJob = new FileWorkflowManager.FileJob(
                "s3-fail-job-id-789",
                projectName,
                Paths.get("/tmp/s3-fail-test"),
                fileNames,
                FileWorkflowManager.FileJobStatus.CREATED
        );

        // Configuration des mocks - échec sur S3
        when(fileWorkflowManager.createFileJob(eq(projectName), eq(fileNames)))
                .thenReturn(mockFileJob);
        when(fileWorkflowManager.areFilesReady(eq("s3-fail-job-id-789")))
                .thenReturn(true);
        when(fileWorkflowManager.uploadFilesToS3(eq("s3-fail-job-id-789")))
                .thenThrow(new RuntimeException("S3 upload failed"));

        // When
        JobExecution jobExecution = fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames);

        // Then
        assertNotNull(jobExecution);
        assertEquals(BatchStatus.FAILED, jobExecution.getStatus());

        // Vérifier que les étapes se sont arrêtées à l'upload S3
        verify(fileWorkflowManager).createFileJob(projectName, fileNames);
        verify(fileWorkflowManager).areFilesReady("s3-fail-job-id-789");
        verify(fileWorkflowManager).uploadFilesToS3("s3-fail-job-id-789");
        verify(fileWorkflowManager, never()).cleanupLocalFiles(anyString());
    }

    @Test
    void testFileWorkflowJobWithInvalidParameters() {
        // Given - paramètres invalides
        Set<String> emptyFileNames = Set.of();

        // When & Then
        assertThrows(RuntimeException.class, () ->
                fileWorkflowBatchService.launchFileWorkflowJob("test-project", emptyFileNames));

        verifyNoInteractions(fileWorkflowManager);
    }
}