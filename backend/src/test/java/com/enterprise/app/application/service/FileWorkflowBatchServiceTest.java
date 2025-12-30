package com.enterprise.app.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobRestartException;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileWorkflowBatchServiceTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job fileWorkflowJob;

    @InjectMocks
    private FileWorkflowBatchService fileWorkflowBatchService;

    private JobExecution mockJobExecution;

    @BeforeEach
    void setUp() {
        // Création d'une exécution de job mock
        JobInstance jobInstance = new JobInstance(1L, "fileWorkflowJob");
        mockJobExecution = new JobExecution(jobInstance, 1L, new JobParameters());
        mockJobExecution.setStatus(BatchStatus.STARTED);
        mockJobExecution.setStartTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("Doit lancer un job de workflow de fichiers avec succès")
    void launchFileWorkflowJob_ShouldLaunchJobSuccessfully() throws Exception {
        // Given
        String projectName = "test-project";
        Set<String> fileNames = Set.of("file1.txt", "file2.csv");

        mockJobExecution.setStatus(BatchStatus.COMPLETED);

        when(jobLauncher.run(eq(fileWorkflowJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        JobExecution result = fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames);

        // Then
        assertNotNull(result);
        assertEquals(BatchStatus.COMPLETED, result.getStatus());
        assertEquals(1L, result.getId());

        verify(jobLauncher).run(eq(fileWorkflowJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Doit gérer l'exception quand le job est déjà en cours d'exécution")
    void launchFileWorkflowJob_ShouldHandleJobExecutionAlreadyRunningException() throws Exception {
        // Given
        String projectName = "running-project";
        Set<String> fileNames = Set.of("file1.txt");

        when(jobLauncher.run(eq(fileWorkflowJob), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("Job is already running"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames));

        assertTrue(exception.getMessage().contains("Job is already running for project: running-project"));
        assertEquals(JobExecutionAlreadyRunningException.class, exception.getCause().getClass());
    }

    @Test
    @DisplayName("Doit gérer l'exception lors du redémarrage du job")
    void launchFileWorkflowJob_ShouldHandleJobRestartException() throws Exception {
        // Given
        String projectName = "restart-fail-project";
        Set<String> fileNames = Set.of("file1.txt");

        when(jobLauncher.run(eq(fileWorkflowJob), any(JobParameters.class)))
                .thenThrow(new JobRestartException("Job restart failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames));

        assertTrue(exception.getMessage().contains("Job restart failed for project: restart-fail-project"));
        assertEquals(JobRestartException.class, exception.getCause().getClass());
    }

    @Test
    @DisplayName("Doit gérer l'exception quand l'instance du job est déjà terminée")
    void launchFileWorkflowJob_ShouldHandleJobInstanceAlreadyCompleteException() throws Exception {
        // Given
        String projectName = "completed-project";
        Set<String> fileNames = Set.of("file1.txt");

        when(jobLauncher.run(eq(fileWorkflowJob), any(JobParameters.class)))
                .thenThrow(new JobInstanceAlreadyCompleteException("Job instance already completed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames));

        assertTrue(exception.getMessage().contains("Job instance already completed for project: completed-project"));
        assertEquals(JobInstanceAlreadyCompleteException.class, exception.getCause().getClass());
    }

    @Test
    @DisplayName("Doit gérer l'exception pour des paramètres de job invalides")
    void launchFileWorkflowJob_ShouldHandleJobParametersInvalidException() throws Exception {
        // Given
        String projectName = "invalid-params-project";
        Set<String> fileNames = Set.of("file1.txt");

        when(jobLauncher.run(eq(fileWorkflowJob), any(JobParameters.class)))
                .thenThrow(new JobParametersInvalidException("Invalid job parameters"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames));

        assertTrue(exception.getMessage().contains("Invalid job parameters for project: invalid-params-project"));
        assertEquals(JobParametersInvalidException.class, exception.getCause().getClass());
    }

    @Test
    @DisplayName("Doit inclure les paramètres de job corrects")
    void launchFileWorkflowJob_ShouldIncludeCorrectJobParameters() throws Exception {
        // Given
        String projectName = "param-test-project";
        Set<String> fileNames = Set.of("param1.txt", "param2.csv", "param3.json");

        when(jobLauncher.run(eq(fileWorkflowJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        JobExecution result = fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames);

        // Then
        assertNotNull(result);
        verify(jobLauncher).run(eq(fileWorkflowJob), argThat(params -> {
            assertEquals("param-test-project", params.getString("projectName"));
            assertEquals("param1.txt,param2.csv,param3.json", params.getString("fileNames"));
            assertNotNull(params.getString("timestamp"));
            return true;
        }));
    }

    @Test
    @DisplayName("Doit appeler le lancement de job de workflow de fichiers en mode asynchrone")
    void launchFileWorkflowJobAsync_ShouldCallLaunchFileWorkflowJob() throws Exception {
        // Given
        String projectName = "async-project";
        Set<String> fileNames = Set.of("async.txt");

        when(jobLauncher.run(eq(fileWorkflowJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        JobExecution result = fileWorkflowBatchService.launchFileWorkflowJobAsync(projectName, fileNames);

        // Then
        assertNotNull(result);
        verify(jobLauncher).run(eq(fileWorkflowJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Doit retourner un statut inconnu pour l'exécution du job")
    void getJobExecutionStatus_ShouldReturnUnknownStatus() {
        // Given
        Long jobExecutionId = 123L;

        // When
        BatchStatus status = fileWorkflowBatchService.getJobExecutionStatus(jobExecutionId);

        // Then
        assertEquals(BatchStatus.UNKNOWN, status);
    }

    @Test
    @DisplayName("Doit retourner un message en attente pour le résumé d'exécution du job")
    void getJobExecutionSummary_ShouldReturnPendingMessage() {
        // Given
        Long jobExecutionId = 456L;

        // When
        String summary = fileWorkflowBatchService.getJobExecutionSummary(jobExecutionId);

        // Then
        assertTrue(summary.contains("Job execution summary for ID: 456"));
        assertTrue(summary.contains("implementation pending"));
    }

    @Test
    @DisplayName("Doit gérer des noms de fichiers vides")
    void launchFileWorkflowJob_ShouldHandleEmptyFileNames() throws Exception {
        // Given
        String projectName = "empty-files-project";
        Set<String> fileNames = Set.of();

        when(jobLauncher.run(eq(fileWorkflowJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        JobExecution result = fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames);

        // Then
        assertNotNull(result);
        verify(jobLauncher).run(eq(fileWorkflowJob), argThat(params -> {
            assertEquals("", params.getString("fileNames"));
            return true;
        }));
    }

    @Test
    @DisplayName("Doit générer des horodatages uniques")
    void launchFileWorkflowJob_ShouldGenerateUniqueTimestamps() throws Exception {
        // Given
        String projectName = "timestamp-project";
        Set<String> fileNames = Set.of("file.txt");

        when(jobLauncher.run(eq(fileWorkflowJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When - lancer le job deux fois rapidement
        JobExecution result1 = fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames);
        JobExecution result2 = fileWorkflowBatchService.launchFileWorkflowJob(projectName, fileNames);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        verify(jobLauncher, times(2)).run(eq(fileWorkflowJob), any(JobParameters.class));
    }
}