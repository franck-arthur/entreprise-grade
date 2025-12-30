package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.application.dto.csvexport.ExcelRowDTO;
import com.enterprise.app.domain.model.csvexport.ExcelDefinitionStaging;
import com.enterprise.app.domain.repository.csvexport.ExcelDefinitionStagingRepository;
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

import javax.sql.DataSource;
import java.util.List;

/**
 * Configuration Spring Batch pour l'import des fichiers Excel de définition CSV.
 *
 * Job complet:
 * 1. Vider la table de staging
 * 2. Importer le fichier Excel
 * 3. Normaliser les données (appel procédure PostgreSQL)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ExcelImportJobConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ExcelItemReader excelItemReader;
    private final ExcelRowProcessor excelRowProcessor;
    private final ExcelStagingWriter excelStagingWriter;

    /**
     * Job principal d'import Excel.
     */
    @Bean
    public Job excelImportJob(ExcelDefinitionStagingRepository stagingRepository, DataSource dataSource) {
        return new JobBuilder("excelImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cleanupStagingStep(stagingRepository))
                .next(importExcelStep())
                .next(normalizeDefinitionsStep(dataSource))
                .build();
    }

    /**
     * Step 1: Nettoyer la table de staging.
     */
    @Bean
    public Step cleanupStagingStep(ExcelDefinitionStagingRepository stagingRepository) {
        return new StepBuilder("cleanupStagingStep", jobRepository)
                .tasklet(cleanupStagingTasklet(stagingRepository), transactionManager)
                .build();
    }

    /**
     * Step 2: Importer le fichier Excel.
     */
    @Bean
    public Step importExcelStep() {
        return new StepBuilder("importExcelStep", jobRepository)
                .<ExcelRowDTO, List<ExcelDefinitionStaging>>chunk(50, transactionManager)
                .reader(excelItemReader)
                .processor(excelRowProcessor)
                .writer(excelStagingWriter)
                .build();
    }

    /**
     * Step 3: Normaliser les définitions (appel procédure PostgreSQL).
     */
    @Bean
    public Step normalizeDefinitionsStep(DataSource dataSource) {
        return new StepBuilder("normalizeDefinitionsStep", jobRepository)
                .tasklet(normalizeDefinitionsTasklet(dataSource), transactionManager)
                .build();
    }

    /**
     * Tasklet pour nettoyer la table de staging.
     */
    @Bean
    @StepScope
    public CleanupStagingTasklet cleanupStagingTasklet(ExcelDefinitionStagingRepository stagingRepository) {
        return new CleanupStagingTasklet(stagingRepository);
    }

    /**
     * Tasklet pour normaliser les définitions.
     */
    @Bean
    @StepScope
    public NormalizeDefinitionsTasklet normalizeDefinitionsTasklet(DataSource dataSource) {
        return new NormalizeDefinitionsTasklet(dataSource);
    }
}