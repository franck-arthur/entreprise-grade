package com.enterprise.app.infrastructure.batch.tasklet;

import com.enterprise.app.domain.storage.FileWorkflowManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tasklet pour uploader les fichiers vers S3
 *
 * Cette étape upload tous les fichiers préparés vers le stockage S3.
 * Les clés S3 des fichiers uploadés sont stockées dans le contexte d'exécution.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UploadFilesTasklet implements Tasklet {

    private final FileWorkflowManager fileWorkflowManager;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Executing UploadFilesTasklet");

        // Récupération du jobId du contexte d'exécution
        String fileJobId = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .getString("fileJobId");

        if (fileJobId == null) {
            throw new IllegalStateException("fileJobId not found in execution context");
        }

        log.info("Uploading files to S3 for job: {}", fileJobId);

        // Upload des fichiers vers S3
        List<String> uploadedKeys = fileWorkflowManager.uploadFilesToS3(fileJobId);

        log.info("Successfully uploaded {} files to S3 for job: {}", uploadedKeys.size(), fileJobId);

        // Stockage des clés S3 dans le contexte d'exécution pour référence future
        String uploadedKeysStr = String.join(",", uploadedKeys);
        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .putString("uploadedS3Keys", uploadedKeysStr);

        log.info("UploadFilesTasklet completed successfully");
        return RepeatStatus.FINISHED;
    }
}