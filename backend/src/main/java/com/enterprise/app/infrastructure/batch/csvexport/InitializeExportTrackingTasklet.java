package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.model.csvexport.ExportExecution;
import com.enterprise.app.domain.repository.csvexport.ExportExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tasklet pour initialiser le tracking d'un export CSV.
 */
@Slf4j
@Component
@StepScope
public class InitializeExportTrackingTasklet implements Tasklet {

    private final ExportExecutionRepository exportExecutionRepository;

    public InitializeExportTrackingTasklet(ExportExecutionRepository exportExecutionRepository) {
        this.exportExecutionRepository = exportExecutionRepository;
    }

    @Value("#{jobParameters['nomCsv']}")
    private String nomCsv;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Initialisation du tracking pour l'export CSV: {}", nomCsv);

        Long jobExecutionId = chunkContext.getStepContext().getStepExecution()
                .getJobExecution().getId();

        // Créer un nouvel enregistrement de tracking
        ExportExecution exportExecution = ExportExecution.create(nomCsv, jobExecutionId);
        exportExecution = exportExecutionRepository.save(exportExecution);

        log.info("Tracking d'export créé avec l'ID: {}", exportExecution.getId());

        // Stocker l'ID dans le contexte pour les steps suivants
        contribution.getStepExecution().getExecutionContext()
                .putLong("export.trackingId", exportExecution.getId());

        return RepeatStatus.FINISHED;
    }
}