package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.repository.csvexport.ExcelDefinitionStagingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Tasklet pour nettoyer la table de staging avant un nouvel import Excel.
 */
@Slf4j
@Component
@StepScope
public class CleanupStagingTasklet implements Tasklet {

    private final ExcelDefinitionStagingRepository stagingRepository;

    public CleanupStagingTasklet(ExcelDefinitionStagingRepository stagingRepository) {
        this.stagingRepository = stagingRepository;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Nettoyage de la table de staging...");

        long countBefore = stagingRepository.count();
        log.info("Nombre d'enregistrements avant nettoyage: {}", countBefore);

        // Supprimer les anciens enregistrements (plus de 24h)
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);
        int deletedOld = stagingRepository.deleteOlderThan(cutoffDate);
        log.info("Supprimé {} anciens enregistrements (> 24h)", deletedOld);

        // Optionnel: supprimer tous les enregistrements pour un import complet
        // stagingRepository.deleteAll();

        long countAfter = stagingRepository.count();
        log.info("Nombre d'enregistrements après nettoyage: {}", countAfter);

        contribution.getStepExecution().getExecutionContext()
                .putLong("staging.recordsDeletedOld", deletedOld);
        contribution.getStepExecution().getExecutionContext()
                .putLong("staging.recordsRemaining", countAfter);

        log.info("Nettoyage de la table de staging terminé");
        return RepeatStatus.FINISHED;
    }
}