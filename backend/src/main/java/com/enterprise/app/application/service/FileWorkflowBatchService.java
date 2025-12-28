package com.enterprise.app.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Service d'application pour lancer les jobs de traitement de fichiers via Spring Batch
 *
 * Ce service fournit une interface simplifiée pour démarrer et monitorer
 * les jobs de traitement de fichiers basés sur le FileWorkflowManager.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileWorkflowBatchService {

    private final JobLauncher jobLauncher;
    private final Job fileWorkflowJob;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * Lance un job de traitement de fichiers
     *
     * @param projectName le nom du projet
     * @param fileNames   les noms des fichiers à traiter
     * @return les informations d'exécution du job
     */
    public JobExecution launchFileWorkflowJob(String projectName, Set<String> fileNames) {
        log.info("Launching file workflow job for project: {} with files: {}", projectName, fileNames);

        try {
            // Création des paramètres du job avec timestamp unique pour permettre les re-exécutions
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String fileNamesParam = String.join(",", fileNames);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("projectName", projectName)
                    .addString("fileNames", fileNamesParam)
                    .addString("timestamp", timestamp) // Garantit l'unicité pour les re-exécutions
                    .toJobParameters();

            // Lancement du job
            JobExecution jobExecution = jobLauncher.run(fileWorkflowJob, jobParameters);

            log.info("File workflow job launched successfully. JobExecutionId: {}, Status: {}",
                    jobExecution.getId(), jobExecution.getStatus());

            return jobExecution;

        } catch (JobExecutionAlreadyRunningException e) {
            String msg = "Job is already running for project: " + projectName;
            log.error(msg, e);
            throw new RuntimeException(msg, e);

        } catch (JobRestartException e) {
            String msg = "Job restart failed for project: " + projectName;
            log.error(msg, e);
            throw new RuntimeException(msg, e);

        } catch (JobInstanceAlreadyCompleteException e) {
            String msg = "Job instance already completed for project: " + projectName;
            log.error(msg, e);
            throw new RuntimeException(msg, e);

        } catch (JobParametersInvalidException e) {
            String msg = "Invalid job parameters for project: " + projectName;
            log.error(msg, e);
            throw new RuntimeException(msg, e);

        } catch (Exception e) {
            String msg = "Failed to launch file workflow job for project: " + projectName;
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Lance un job de traitement de fichiers de manière asynchrone
     *
     * @param projectName le nom du projet
     * @param fileNames   les noms des fichiers à traiter
     * @return les informations d'exécution du job (peut être en cours d'exécution)
     */
    public JobExecution launchFileWorkflowJobAsync(String projectName, Set<String> fileNames) {
        log.info("Launching file workflow job asynchronously for project: {} with files: {}", projectName, fileNames);

        JobExecution jobExecution = launchFileWorkflowJob(projectName, fileNames);

        log.info("Asynchronous file workflow job initiated. JobExecutionId: {}", jobExecution.getId());

        return jobExecution;
    }

    /**
     * Vérifie le statut d'une exécution de job
     *
     * @param jobExecutionId l'ID de l'exécution du job
     * @return le statut de l'exécution
     */
    public BatchStatus getJobExecutionStatus(Long jobExecutionId) {
        log.debug("Checking status for job execution: {}", jobExecutionId);

        // Note: Dans une implémentation réelle, il faudrait utiliser JobExplorer
        // pour récupérer le statut depuis la base de données
        // JobExplorer jobExplorer = ...;
        // JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
        // return jobExecution != null ? jobExecution.getStatus() : null;

        // Pour cet exemple, nous retournons un statut par défaut
        log.warn("Job status check not fully implemented. JobExecutionId: {}", jobExecutionId);
        return BatchStatus.UNKNOWN;
    }

    /**
     * Récupère les informations détaillées d'une exécution de job
     *
     * @param jobExecutionId l'ID de l'exécution du job
     * @return les informations détaillées du job ou null si non trouvé
     */
    public String getJobExecutionSummary(Long jobExecutionId) {
        log.debug("Getting execution summary for job: {}", jobExecutionId);

        // Note: Dans une implémentation réelle, il faudrait utiliser JobExplorer
        // pour récupérer les informations détaillées depuis la base de données

        return String.format("Job execution summary for ID: %d (implementation pending)", jobExecutionId);
    }
}