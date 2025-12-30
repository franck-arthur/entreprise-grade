package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.repository.csvexport.ExportExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

/**
 * Configuration Spring Batch pour l'export des fichiers CSV.
 *
 * Job complet:
 * 1. Initialiser le tracking d'export
 * 2. Exporter les données vers CSV
 * 3. Finaliser le tracking d'export
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CsvExportJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DynamicQueryItemReader dynamicQueryItemReader;
    private final CsvLineProcessor csvLineProcessor;
    private final DynamicCsvFileWriter dynamicCsvFileWriter;

    /**
     * Job principal d'export CSV.
     */
    @Bean
    public Job csvExportJob(ExportExecutionRepository exportExecutionRepository) {
        return new JobBuilder("csvExportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(initializeExportTrackingStep(exportExecutionRepository))
                .next(exportCsvStep())
                .next(finalizeExportTrackingStep(exportExecutionRepository))
                .build();
    }

    /**
     * Step 1: Initialiser le tracking d'export.
     */
    @Bean
    public Step initializeExportTrackingStep(ExportExecutionRepository exportExecutionRepository) {
        return new StepBuilder("initializeExportTrackingStep", jobRepository)
                .tasklet(initializeExportTrackingTasklet(exportExecutionRepository), transactionManager)
                .build();
    }

    /**
     * Step 2: Exporter les données vers CSV.
     */
    @Bean
    public Step exportCsvStep() {
        return new StepBuilder("exportCsvStep", jobRepository)
                .<Map<String, Object>, String[]>chunk(500, transactionManager)
                .reader(dynamicQueryItemReader)
                .processor(csvLineProcessor)
                .writer(dynamicCsvFileWriter)
                .build();
    }

    /**
     * Step 3: Finaliser le tracking d'export.
     */
    @Bean
    public Step finalizeExportTrackingStep(ExportExecutionRepository exportExecutionRepository) {
        return new StepBuilder("finalizeExportTrackingStep", jobRepository)
                .tasklet(finalizeExportTrackingTasklet(exportExecutionRepository), transactionManager)
                .build();
    }

    /**
     * Tasklet pour initialiser le tracking d'export.
     */
    @Bean
    @StepScope
    public InitializeExportTrackingTasklet initializeExportTrackingTasklet(ExportExecutionRepository exportExecutionRepository) {
        return new InitializeExportTrackingTasklet(exportExecutionRepository);
    }

    /**
     * Tasklet pour finaliser le tracking d'export.
     */
    @Bean
    @StepScope
    public FinalizeExportTrackingTasklet finalizeExportTrackingTasklet(ExportExecutionRepository exportExecutionRepository) {
        return new FinalizeExportTrackingTasklet(exportExecutionRepository);
    }
}