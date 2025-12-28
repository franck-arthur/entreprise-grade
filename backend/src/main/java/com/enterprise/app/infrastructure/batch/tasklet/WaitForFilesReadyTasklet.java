package com.enterprise.app.infrastructure.batch.tasklet;

import com.enterprise.app.domain.storage.FileWorkflowManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Tasklet pour attendre que les fichiers soient prêts
 *
 * Cette étape vérifie que tous les fichiers du job ont été remplis par un processus externe.
 * Elle utilise une logique de polling avec un délai configurable entre les vérifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WaitForFilesReadyTasklet implements Tasklet {

    private final FileWorkflowManager fileWorkflowManager;

    private static final int MAX_RETRY_ATTEMPTS = 30;
    private static final long INITIAL_DELAY_MS = 2000;
    private static final double MULTIPLIER = 1.5;
    private static final long MAX_DELAY_MS = 30000; // 30 secondes max

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Executing WaitForFilesReadyTasklet");

        String fileJobId = getFileJobId(chunkContext);
        log.info("Waiting for files to be ready for job: {}", fileJobId);

        checkFilesReadiness(fileJobId);
        log.info("Files are ready for job: {}", fileJobId);
        return RepeatStatus.FINISHED;
    }

    private String getFileJobId(ChunkContext chunkContext) {
        String fileJobId = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .getString("fileJobId");

        if (fileJobId == null) {
            throw new IllegalStateException("fileJobId not found in execution context");
        }
        return fileJobId;
    }

    @Retryable(
            retryFor = {RuntimeException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(
                    delay = INITIAL_DELAY_MS,
                    multiplier = MULTIPLIER,
                    maxDelay = MAX_DELAY_MS
            )
    )
    public void checkFilesReadiness(String fileJobId) {
        log.debug("Checking files readiness for job: {}", fileJobId);

        if (!fileWorkflowManager.areFilesReady(fileJobId)) {
            throw new RuntimeException("Files not ready yet for job: " + fileJobId);
        }
    }

    @Recover
    public void recoverFromFilesNotReady(RuntimeException ex, String fileJobId) {
        String errorMsg = String.format("Files not ready for job %s after %d attempts", fileJobId, MAX_RETRY_ATTEMPTS);
        log.error(errorMsg, ex);
        throw new RuntimeException(errorMsg, ex);
    }
}