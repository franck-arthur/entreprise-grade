package com.enterprise.app.infrastructure.batch.tasklet;

import com.enterprise.app.domain.storage.FileWorkflowManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Tasklet pour nettoyer les fichiers locaux
 *
 * Cette étape finale supprime tous les fichiers locaux temporaires après leur upload réussi vers S3.
 * Elle marque également le job comme terminé avec succès.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupFilesTasklet implements Tasklet {

    private final FileWorkflowManager fileWorkflowManager;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Executing CleanupFilesTasklet");

        // Récupération du jobId du contexte d'exécution
        String fileJobId = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .getString("fileJobId");

        if (fileJobId == null) {
            throw new IllegalStateException("fileJobId not found in execution context");
        }

        log.info("Cleaning up local files for job: {}", fileJobId);

        // Nettoyage des fichiers locaux
        fileWorkflowManager.cleanupLocalFiles(fileJobId);

        log.info("Successfully cleaned up local files for job: {}", fileJobId);

        // Récupération des clés S3 pour le log final
        String uploadedKeys = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .getString("uploadedS3Keys");

        if (uploadedKeys != null) {
            log.info("Job {} completed successfully. Files uploaded to S3: {}", fileJobId, uploadedKeys);
        }

        log.info("CleanupFilesTasklet completed successfully");
        return RepeatStatus.FINISHED;
    }
}