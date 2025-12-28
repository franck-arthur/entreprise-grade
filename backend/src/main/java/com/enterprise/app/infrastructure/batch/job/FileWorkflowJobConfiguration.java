package com.enterprise.app.infrastructure.batch.job;

import com.enterprise.app.infrastructure.batch.tasklet.CleanupFilesTasklet;
import com.enterprise.app.infrastructure.batch.tasklet.CreateFileJobTasklet;
import com.enterprise.app.infrastructure.batch.tasklet.UploadFilesTasklet;
import com.enterprise.app.infrastructure.batch.tasklet.WaitForFilesReadyTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration du Job Spring Batch pour le workflow de traitement de fichiers
 *
 * Ce job définit un workflow complet en 4 étapes :
 * 1. Création du job de fichiers avec répertoire et fichiers vides
 * 2. Attente que les fichiers soient remplis par un processus externe
 * 3. Upload des fichiers vers S3
 * 4. Nettoyage des fichiers locaux
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FileWorkflowJobConfiguration {

    private final CreateFileJobTasklet createFileJobTasklet;
    private final WaitForFilesReadyTasklet waitForFilesReadyTasklet;
    private final UploadFilesTasklet uploadFilesTasklet;
    private final CleanupFilesTasklet cleanupFilesTasklet;

    /**
     * Job principal de traitement des fichiers
     */
    @Bean
    public Job fileWorkflowJob(JobRepository jobRepository,
                              Step createFileJobStep,
                              Step waitForFilesReadyStep,
                              Step uploadFilesStep,
                              Step cleanupFilesStep) {
        return new JobBuilder("fileWorkflowJob", jobRepository)
                .start(createFileJobStep)
                .next(waitForFilesReadyStep)
                .next(uploadFilesStep)
                .next(cleanupFilesStep)
                .build();
    }

    /**
     * Étape 1 : Création du job de fichiers
     */
    @Bean
    public Step createFileJobStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        return new StepBuilder("createFileJobStep", jobRepository)
                .tasklet(createFileJobTasklet, transactionManager)
                .build();
    }

    /**
     * Étape 2 : Attente que les fichiers soient prêts
     */
    @Bean
    public Step waitForFilesReadyStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        return new StepBuilder("waitForFilesReadyStep", jobRepository)
                .tasklet(waitForFilesReadyTasklet, transactionManager)
                .build();
    }

    /**
     * Étape 3 : Upload des fichiers vers S3
     */
    @Bean
    public Step uploadFilesStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
        return new StepBuilder("uploadFilesStep", jobRepository)
                .tasklet(uploadFilesTasklet, transactionManager)
                .build();
    }

    /**
     * Étape 4 : Nettoyage des fichiers locaux
     */
    @Bean
    public Step cleanupFilesStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("cleanupFilesStep", jobRepository)
                .tasklet(cleanupFilesTasklet, transactionManager)
                .build();
    }
}