package com.enterprise.app.application.service.csvexport;

import com.enterprise.app.domain.model.csvexport.ExportExecution;
import com.enterprise.app.domain.model.csvexport.StatutExecution;
import com.enterprise.app.domain.repository.csvexport.ExportExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;
import io.micrometer.observation.annotation.Observed;
import org.springframework.scheduling.annotation.Async;

/**
 * Service pour gérer les exports CSV.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportService {

    // Virtual Thread Executor pour les tâches asynchrones (Java 21)
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final JobLauncher jobLauncher;
    private final Job csvExportJob;
    private final ExportExecutionRepository exportExecutionRepository;

    /**
     * Lance l'export d'un CSV pour une période donnée.
     * Observé par Micrometer pour les métriques et traces.
     *
     * @param nomCsv Nom du CSV à exporter (CSV_1, CSV_2, CSV_3)
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @param outputDirectory Répertoire de sortie (optionnel)
     * @return L'ID de l'exécution du job
     */
    @Observed(
        name = "csv.export",
        contextualName = "csv-export-execution",
        lowCardinalityKeyValues = {"operation", "export", "format", "csv"}
    )
    public Long exportCsv(String nomCsv, LocalDate dateDebut, LocalDate dateFin, String outputDirectory) {
        log.info("Lancement de l'export CSV: {} pour la période {} - {}", nomCsv, dateDebut, dateFin);

        try {
            // Vérifier qu'il n'y a pas d'export en cours pour ce CSV
            List<ExportExecution> exportsEnCours = exportExecutionRepository
                    .findByStatutAndDateFinIsNull(StatutExecution.EN_COURS);

            Optional<ExportExecution> exportEnCours = exportsEnCours.stream()
                    .filter(exp -> exp.getNomCsv().equals(nomCsv))
                    .findFirst();

            if (exportEnCours.isPresent()) {
                log.warn("Un export est déjà en cours pour le CSV: {}", nomCsv);
                throw new IllegalStateException("Export déjà en cours pour: " + nomCsv);
            }

            // Construire les paramètres du job
            JobParametersBuilder builder = new JobParametersBuilder()
                    .addString("nomCsv", nomCsv)
                    .addLocalDate("dateDebut", dateDebut)
                    .addLocalDate("dateFin", dateFin)
                    .addLong("timestamp", System.currentTimeMillis()); // Pour assurer l'unicité

            if (outputDirectory != null && !outputDirectory.trim().isEmpty()) {
                builder.addString("outputDirectory", outputDirectory);
            }

            JobParameters jobParameters = builder.toJobParameters();

            // Lancer le job
            var jobExecution = jobLauncher.run(csvExportJob, jobParameters);
            Long jobExecutionId = jobExecution.getId();

            log.info("Export CSV lancé avec succès: job ID = {}", jobExecutionId);
            return jobExecutionId;

        } catch (Exception e) {
            log.error("Erreur lors du lancement de l'export CSV: {}", nomCsv, e);
            throw new IllegalStateException("Erreur export CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Lance l'export hebdomadaire (7 derniers jours).
     * Observé pour le monitoring des exports automatiques.
     */
    @Observed(
        name = "csv.export.weekly",
        contextualName = "weekly-csv-export",
        lowCardinalityKeyValues = {"operation", "export", "schedule", "weekly"}
    )
    public Long exportCsvHebdomadaire(String nomCsv) {
        LocalDate dateFin = LocalDate.now();
        LocalDate dateDebut = dateFin.minusDays(7);
        return exportCsv(nomCsv, dateDebut, dateFin, null);
    }

    /**
     * Lance l'export pour tous les CSV (CSV_1, CSV_2, CSV_3).
     * Utilise les Virtual Threads Java 21 pour l'exécution parallèle.
     * Observé pour le monitoring des exports en lot.
     */
    @Observed(
        name = "csv.export.batch",
        contextualName = "batch-csv-export",
        lowCardinalityKeyValues = {"operation", "export", "type", "batch"}
    )
    public void exportAllCsvs(LocalDate dateDebut, LocalDate dateFin, String outputDirectory) {
        log.info("Lancement de l'export de tous les CSV pour la période {} - {} (Virtual Threads)",
                dateDebut, dateFin);

        String[] csvs = {"CSV_1", "CSV_2", "CSV_3"};

        // Utilisation des Virtual Threads pour l'exécution parallèle
        var futures = List.of(csvs).stream()
                .map(nomCsv -> CompletableFuture.runAsync(() -> {
                    try {
                        exportCsv(nomCsv, dateDebut, dateFin, outputDirectory);
                        log.info("Export terminé pour: {}", nomCsv);
                    } catch (Exception e) {
                        log.error("Erreur lors de l'export pour: {}", nomCsv, e);
                    }
                }, virtualThreadExecutor))
                .toList();

        // Attendre que tous les exports se terminent
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.MINUTES); // Timeout de 30 minutes
            log.info("Tous les exports CSV terminés");
        } catch (Exception e) {
            log.error("Erreur lors de l'attente des exports parallèles", e);
        }
    }

    /**
     * Lance l'export hebdomadaire pour tous les CSV de manière asynchrone.
     * Retourne un CompletableFuture pour permettre l'exécution non-bloquante.
     * Observé pour le monitoring des tâches asynchrones.
     */
    @Async
    @Observed(
        name = "csv.export.weekly.async",
        contextualName = "async-weekly-csv-export",
        lowCardinalityKeyValues = {"operation", "export", "schedule", "weekly", "async", "true"}
    )
    public CompletableFuture<Void> exportAllCsvsHebdomadaireAsync() {
        LocalDate dateFin = LocalDate.now();
        LocalDate dateDebut = dateFin.minusDays(7);

        return CompletableFuture.runAsync(() -> {
            exportAllCsvs(dateDebut, dateFin, null);
        }, virtualThreadExecutor);
    }

    /**
     * Lance l'export hebdomadaire pour tous les CSV (version synchrone).
     */
    public void exportAllCsvsHebdomadaire() {
        LocalDate dateFin = LocalDate.now();
        LocalDate dateDebut = dateFin.minusDays(7);
        exportAllCsvs(dateDebut, dateFin, null);
    }

    /**
     * Retourne le statut d'un export.
     * Observé pour le monitoring des consultations de statut.
     */
    @Observed(
        name = "csv.export.status",
        contextualName = "export-status-query",
        lowCardinalityKeyValues = {"operation", "query", "type", "status"}
    )
    public Optional<ExportExecution> getExportStatus(Long jobExecutionId) {
        return exportExecutionRepository.findByJobExecutionId(jobExecutionId);
    }

    /**
     * Retourne l'historique des exports pour un CSV donné.
     * Observé pour le monitoring des consultations d'historique.
     */
    @Observed(
        name = "csv.export.history",
        contextualName = "export-history-query",
        lowCardinalityKeyValues = {"operation", "query", "type", "history"}
    )
    public List<ExportExecution> getExportHistory(String nomCsv) {
        return exportExecutionRepository.findTop10ByNomCsvOrderByDateDebutDesc(nomCsv);
    }

    /**
     * Retourne les exports en cours.
     */
    public List<ExportExecution> getRunningExports() {
        return exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS);
    }

    /**
     * Retourne les statistiques d'export pour un CSV donné de manière asynchrone.
     */
    public CompletableFuture<Object[]> getExportStatisticsAsync(String nomCsv, LocalDateTime depuis) {
        return CompletableFuture.supplyAsync(() ->
            exportExecutionRepository.getStatistiquesExport(nomCsv, depuis),
            virtualThreadExecutor
        );
    }

    /**
     * Retourne les statistiques d'export pour un CSV donné (version synchrone).
     */
    public Object[] getExportStatistics(String nomCsv, LocalDateTime depuis) {
        return exportExecutionRepository.getStatistiquesExport(nomCsv, depuis);
    }

    /**
     * Nettoyage des ressources au shutdown.
     */
    @PreDestroy
    public void cleanup() {
        log.info("Fermeture du Virtual Thread Executor");
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}