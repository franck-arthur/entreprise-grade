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
class CleanupFilesTaskletTest {

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
    private CleanupFilesTasklet cleanupFilesTasklet;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        executionContext = new ExecutionContext();
        executionContext.putString("fileJobId", "test-job-id-123");
        executionContext.putString("uploadedS3Keys", "key1.txt,key2.csv,key3.json");

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
    }

    @Test
    void execute_ShouldCleanupFilesSuccessfully() throws Exception {
        // Given
        String jobId = "test-job-id-123";

        // When
        RepeatStatus result = cleanupFilesTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager).cleanupLocalFiles(jobId);
    }

    @Test
    void execute_ShouldThrowException_WhenJobIdIsMissing() {
        // Given
        executionContext.remove("fileJobId");

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cleanupFilesTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("fileJobId not found in execution context", exception.getMessage());
        verifyNoInteractions(fileWorkflowManager);
    }

    @Test
    void execute_ShouldHandleFileWorkflowManagerException() {
        // Given
        String jobId = "test-job-id-123";
        doThrow(new RuntimeException("Cleanup failed"))
                .when(fileWorkflowManager)
                .cleanupLocalFiles(eq(jobId));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cleanupFilesTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("Cleanup failed", exception.getMessage());
        verify(fileWorkflowManager).cleanupLocalFiles(jobId);
    }

    @Test
    void execute_ShouldHandleMissingUploadedKeysGracefully() throws Exception {
        // Given
        String jobId = "test-job-id-123";
        executionContext.remove("uploadedS3Keys"); // Pas de clés S3 dans le contexte

        // When
        RepeatStatus result = cleanupFilesTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager).cleanupLocalFiles(jobId);
        // Le tasklet devrait quand même fonctionner même sans les clés S3
    }

    @Test
    void execute_ShouldLogUploadedKeys() throws Exception {
        // Given
        String jobId = "test-job-id-123";
        String expectedKeys = "key1.txt,key2.csv,key3.json";

        // When
        RepeatStatus result = cleanupFilesTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager).cleanupLocalFiles(jobId);

        // Vérifier que les clés sont présentes dans le contexte (pour le logging)
        String storedKeys = executionContext.getString("uploadedS3Keys");
        assertEquals(expectedKeys, storedKeys);
    }

    @Test
    void execute_ShouldHandleNullJobIdInContext() {
        // Given
        executionContext.putString("fileJobId", null);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cleanupFilesTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("fileJobId not found in execution context", exception.getMessage());
        verifyNoInteractions(fileWorkflowManager);
    }

    @Test
    void execute_ShouldHandleEmptyUploadedKeys() throws Exception {
        // Given
        String jobId = "test-job-id-123";
        executionContext.putString("uploadedS3Keys", "");

        // When
        RepeatStatus result = cleanupFilesTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager).cleanupLocalFiles(jobId);
    }
}