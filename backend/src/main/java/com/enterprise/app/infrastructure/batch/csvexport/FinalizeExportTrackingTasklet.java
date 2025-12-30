package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.model.csvexport.ExportExecution;
import com.enterprise.app.domain.repository.csvexport.ExportExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Tasklet pour finaliser le tracking d'un export CSV.
 */
@Slf4j
@Component
@StepScope
public class FinalizeExportTrackingTasklet implements Tasklet {

    private final ExportExecutionRepository exportExecutionRepository;

    public FinalizeExportTrackingTasklet(ExportExecutionRepository exportExecutionRepository) {
        this.exportExecutionRepository = exportExecutionRepository;
    }

    @Value("#{jobParameters['nomCsv']}")
    private String nomCsv;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Finalisation du tracking pour l'export CSV: {}", nomCsv);

        // Récupérer l'ID de tracking depuis le contexte du job
        Long trackingId = chunkContext.getStepContext().getStepExecution()
                .getJobExecution().getExecutionContext().getLong("export.trackingId", -1L);

        if (trackingId == -1L) {
            log.error("Aucun ID de tracking trouvé dans le contexte");
            return RepeatStatus.FINISHED;
        }

        Optional<ExportExecution> optionalExecution = exportExecutionRepository.findById(trackingId);
        if (optionalExecution.isEmpty()) {
            log.error("Aucun enregistrement de tracking trouvé avec l'ID: {}", trackingId);
            return RepeatStatus.FINISHED;
        }

        ExportExecution exportExecution = optionalExecution.get();

        // Récupérer les informations de l'export depuis les contextes des steps
        BatchStatus jobStatus = chunkContext.getStepContext().getStepExecution()
                .getJobExecution().getStatus();

        // Récupérer le nombre de lignes exportées depuis le contexte du step CSV
        Integer totalLinesWritten = null;
        String outputFile = null;

        try {
            totalLinesWritten = chunkContext.getStepContext().getStepExecution()
                    .getJobExecution().getExecutionContext().getInt("csv.totalLinesWritten", 0);
            outputFile = chunkContext.getStepContext().getStepExecution()
                    .getJobExecution().getExecutionContext().getString("csv.outputFile");
        } catch (Exception e) {
            log.warn("Impossible de récupérer les métriques d'export", e);
        }

        // Finaliser selon le statut du job
        if (jobStatus == BatchStatus.COMPLETED) {
            exportExecution.markAsCompleted(totalLinesWritten, outputFile);
            log.info("Export CSV terminé avec succès: {} lignes exportées vers {}",
                    totalLinesWritten, outputFile);
        } else {
            String errorMessage = "Job terminé avec le statut: " + jobStatus;
            exportExecution.markAsFailed(errorMessage);
            log.error("Export CSV échoué: {}", errorMessage);
        }

        exportExecutionRepository.save(exportExecution);
        log.info("Tracking d'export mis à jour: {}", exportExecution.getStatut());

        return RepeatStatus.FINISHED;
    }
}