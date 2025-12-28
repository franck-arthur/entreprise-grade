package com.enterprise.app.infrastructure.batch.tasklet;

import com.enterprise.app.domain.storage.FileWorkflowManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateFileJobTaskletTest {

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
    private CreateFileJobTasklet createFileJobTasklet;

    private JobParameters jobParameters;

    @BeforeEach
    void setUp() {
        jobParameters = new JobParametersBuilder()
                .addString("projectName", "test-project")
                .addString("fileNames", "file1.txt,file2.csv,file3.json")
                .toJobParameters();

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
    }

    @Test
    void execute_ShouldCreateFileJobSuccessfully() throws Exception {
        // Given
        String expectedJobId = "test-job-id-123";
        Set<String> expectedFileNames = Set.of("file1.txt", "file2.csv", "file3.json");

        FileWorkflowManager.FileJob mockFileJob = new FileWorkflowManager.FileJob(
                expectedJobId,
                "test-project",
                Paths.get("/tmp/test-project"),
                expectedFileNames,
                FileWorkflowManager.FileJobStatus.CREATED
        );

        when(fileWorkflowManager.createFileJob(eq("test-project"), eq(expectedFileNames)))
                .thenReturn(mockFileJob);

        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(new org.springframework.batch.item.ExecutionContext());
        // When
        RepeatStatus result = createFileJobTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager).createFileJob("test-project", expectedFileNames);

        // Vérifier que le jobId est stocké dans le contexte d'exécution
        String storedJobId = jobExecution.getExecutionContext().getString("fileJobId");
        assertEquals(expectedJobId, storedJobId);
    }

    @Test
    void execute_ShouldThrowException_WhenProjectNameIsMissing() {
        // Given
        JobParameters incompleteParams = new JobParametersBuilder()
                .addString("fileNames", "file1.txt,file2.csv")
                .toJobParameters();

        when(stepExecution.getJobParameters()).thenReturn(incompleteParams);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createFileJobTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("Missing required parameters: projectName and/or fileNames", exception.getMessage());
        verifyNoInteractions(fileWorkflowManager);
    }

    @Test
    void execute_ShouldThrowException_WhenFileNamesAreMissing() {
        // Given
        JobParameters incompleteParams = new JobParametersBuilder()
                .addString("projectName", "test-project")
                .toJobParameters();

        when(stepExecution.getJobParameters()).thenReturn(incompleteParams);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createFileJobTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("Missing required parameters: projectName and/or fileNames", exception.getMessage());
        verifyNoInteractions(fileWorkflowManager);
    }

    @Test
    void execute_ShouldHandleFileWorkflowManagerException() {
        // Given
        Set<String> expectedFileNames = Set.of("file1.txt", "file2.csv", "file3.json");

        when(fileWorkflowManager.createFileJob(eq("test-project"), eq(expectedFileNames)))
                .thenThrow(new RuntimeException("Storage error"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> createFileJobTasklet.execute(stepContribution, chunkContext)
        );

        assertEquals("Storage error", exception.getMessage());
        verify(fileWorkflowManager).createFileJob("test-project", expectedFileNames);
    }

    @Test
    void execute_ShouldHandleSingleFileName() throws Exception {
        // Given
        JobParameters singleFileParams = new JobParametersBuilder()
                .addString("projectName", "single-file-project")
                .addString("fileNames", "single.txt")
                .toJobParameters();

        when(stepExecution.getJobParameters()).thenReturn(singleFileParams);

        String expectedJobId = "single-file-job-id";
        Set<String> expectedFileNames = Set.of("single.txt");

        FileWorkflowManager.FileJob mockFileJob = new FileWorkflowManager.FileJob(
                expectedJobId,
                "single-file-project",
                Paths.get("/tmp/single-file-project"),
                expectedFileNames,
                FileWorkflowManager.FileJobStatus.CREATED
        );

        when(fileWorkflowManager.createFileJob(eq("single-file-project"), eq(expectedFileNames)))
                .thenReturn(mockFileJob);

        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(new org.springframework.batch.item.ExecutionContext());
        // When
        RepeatStatus result = createFileJobTasklet.execute(stepContribution, chunkContext);

        // Then
        assertEquals(RepeatStatus.FINISHED, result);
        verify(fileWorkflowManager).createFileJob("single-file-project", expectedFileNames);
    }
}