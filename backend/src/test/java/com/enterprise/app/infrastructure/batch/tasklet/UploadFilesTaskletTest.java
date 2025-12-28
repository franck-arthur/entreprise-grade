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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadFilesTaskletTest {

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
    private UploadFilesTasklet uploadFilesTasklet;

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
    void execute_ShouldUploadFilesSuccessfully() throws Exception {
        // Given
        String jobId = "test-job-id-123";
        List<String> expectedUploadedKeys = List.of(
                "projects/2024/01/01/test-project-12345678/file1.txt",
                "projects/2024/01/01/test-project-12345678/file2.csv",
                "projects/2024/01/01/test-project-12345678/file3.json"
        );

        when(fileWorkflowManager.uploadFilesToS3(eq(jobId))).thenReturn(expectedUploadedKeys);

        // When
        RepeatStatus result = uploadFilesTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager).uploadFilesToS3(jobId);

        // Vérifier que les clés S3 sont stockées dans le contexte d'exécution
        String storedKeys = executionContext.getString("uploadedS3Keys");
        assertEquals(String.join(",", expectedUploadedKeys), storedKeys);
    }

    @Test
    void execute_ShouldThrowException_WhenJobIdIsMissing() {
        // Given
        executionContext.remove("fileJobId");

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> uploadFilesTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("fileJobId not found in execution context", exception.getMessage());
        verifyNoInteractions(fileWorkflowManager);
    }

    @Test
    void execute_ShouldHandleFileWorkflowManagerException() {
        // Given
        String jobId = "test-job-id-123";
        when(fileWorkflowManager.uploadFilesToS3(eq(jobId)))
                .thenThrow(new RuntimeException("S3 upload failed"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> uploadFilesTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("S3 upload failed", exception.getMessage());
        verify(fileWorkflowManager).uploadFilesToS3(jobId);
    }

    @Test
    void execute_ShouldHandleEmptyUploadedKeysList() throws Exception {
        // Given
        String jobId = "test-job-id-123";
        List<String> emptyUploadedKeys = List.of();

        when(fileWorkflowManager.uploadFilesToS3(eq(jobId))).thenReturn(emptyUploadedKeys);

        // When
        RepeatStatus result = uploadFilesTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager).uploadFilesToS3(jobId);

        // Vérifier que même une liste vide est gérée correctement
        String storedKeys = executionContext.getString("uploadedS3Keys");
        assertEquals("", storedKeys);
    }

    @Test
    void execute_ShouldHandleSingleFileUpload() throws Exception {
        // Given
        String jobId = "test-job-id-123";
        List<String> singleUploadedKey = List.of("projects/2024/01/01/single-project-87654321/single.txt");

        when(fileWorkflowManager.uploadFilesToS3(eq(jobId))).thenReturn(singleUploadedKey);

        // When
        RepeatStatus result = uploadFilesTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager).uploadFilesToS3(jobId);

        String storedKeys = executionContext.getString("uploadedS3Keys");
        assertEquals("projects/2024/01/01/single-project-87654321/single.txt", storedKeys);
    }

    @Test
    void execute_ShouldHandleNullJobIdInContext() {
        // Given
        executionContext.putString("fileJobId", null);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> uploadFilesTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("fileJobId not found in execution context", exception.getMessage());
        verifyNoInteractions(fileWorkflowManager);
    }
}