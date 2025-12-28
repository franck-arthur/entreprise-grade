package com.enterprise.app.infrastructure.batch.tasklet;

import com.enterprise.app.domain.storage.FileWorkflowManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitForFilesReadyTaskletTest {

    @Mock
    private FileWorkflowManager fileWorkflowManager;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    @Mock
    private StepContext stepContext;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private JobExecution jobExecution;

    @InjectMocks
    private WaitForFilesReadyTasklet waitForFilesReadyTasklet;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        executionContext = new ExecutionContext();
        executionContext.putString("fileJobId", "test-job-id-123");

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
    }

    @Test
    void execute_ShouldReturnFinished_WhenFilesAreReadyImmediately() throws Exception {
        // Given
        String jobId = "test-job-id-123";
        when(fileWorkflowManager.areFilesReady(eq(jobId))).thenReturn(true);

        // When
        RepeatStatus result = waitForFilesReadyTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager, times(1)).areFilesReady(jobId);
    }

    @Test
    void execute_ShouldReturnFinished_WhenFilesAreReadyAfterSeveralAttempts() throws Exception {
        // Given
        String jobId = "test-job-id-123";
        when(fileWorkflowManager.areFilesReady(eq(jobId)))
                .thenReturn(false)  // première tentative
                .thenReturn(false)  // deuxième tentative
                .thenReturn(true);  // troisième tentative - succès

        // When
        RepeatStatus result = waitForFilesReadyTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager, times(3)).areFilesReady(jobId);
    }

    @Test
    void execute_ShouldThrowException_WhenJobIdIsMissing() {
        // Given
        executionContext.remove("fileJobId"); // Supprimer le jobId du contexte

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> waitForFilesReadyTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("fileJobId not found in execution context", exception.getMessage());
        verifyNoInteractions(fileWorkflowManager);
    }

    @Test
    void execute_ShouldThrowException_WhenMaxAttemptsReached() {
        // Given
        String jobId = "test-job-id-123";
        when(fileWorkflowManager.areFilesReady(eq(jobId))).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> waitForFilesReadyTasklet.execute(stepContribution, chunkContext)
        );

        assertTrue(exception.getMessage().contains("Files not ready for job test-job-id-123 after"));
        assertTrue(exception.getMessage().contains("attempts"));

        // Vérifier qu'on a bien fait le nombre maximum de tentatives
        verify(fileWorkflowManager, times(30)).areFilesReady(jobId);
    }

    @Test
    void execute_ShouldHandleFileWorkflowManagerException() {
        // Given
        String jobId = "test-job-id-123";
        when(fileWorkflowManager.areFilesReady(eq(jobId)))
                .thenThrow(new RuntimeException("Storage access error"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> waitForFilesReadyTasklet.execute(stepContribution, chunkContext)
        );

        assertTrue(exception.getMessage().contains("Files not ready for job test-job-id-123 after"));
        verify(fileWorkflowManager, times(30)).areFilesReady(jobId);
    }

    @Test
    void execute_ShouldHandleNullJobIdInContext() {
        // Given
        executionContext.putString("fileJobId", null);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> waitForFilesReadyTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("fileJobId not found in execution context", exception.getMessage());
        verifyNoInteractions(fileWorkflowManager);
    }
}