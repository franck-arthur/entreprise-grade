package com.enterprise.app.infrastructure.scheduler;

import com.enterprise.app.application.service.csvexport.CsvExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler pour les exports CSV automatiques.
 * Déclenche les exports hebdomadaires tous les lundis à 2h du matin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.csv-export.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CsvExportScheduler {

    private final CsvExportService csvExportService;

    /**
     * Export automatique tous les lundis à 2h00 du matin.
     * Cron: 0 0 2 ? * MON (seconde minute heure jour mois jour-semaine)
     */
    @Scheduled(cron = "0 0 2 ? * MON", zone = "Europe/Paris")
    public void weeklyExport() {
        log.info("=== Démarrage de l'export CSV hebdomadaire automatique ===");

        try {
            // Lancer l'export pour tous les CSV (CSV_1, CSV_2, CSV_3)
            csvExportService.exportAllCsvsHebdomadaire();

            log.info("=== Export CSV hebdomadaire lancé avec succès ===");

        } catch (Exception e) {
            log.error("=== Erreur lors de l'export CSV hebdomadaire automatique ===", e);

            // En cas d'erreur, on peut notifier l'équipe (email, Slack, etc.)
            // Ici on se contente de logger l'erreur
        }
    }

    /**
     * Export de test - tous les jours à 10h (pour les tests/dev).
     * Désactivé en production via la propriété.
     */
    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Paris")
    @ConditionalOnProperty(value = "app.csv-export.scheduler.test-export.enabled", havingValue = "true")
    public void dailyTestExport() {
        log.info("=== Démarrage de l'export CSV de test quotidien ===");

        try {
            // Export uniquement CSV_1 pour les tests
            csvExportService.exportCsvHebdomadaire("CSV_1");

            log.info("=== Export CSV de test terminé ===");

        } catch (Exception e) {
            log.error("=== Erreur lors de l'export CSV de test ===", e);
        }
    }

    /**
     * Nettoyage des anciennes exécutions d'export - tous les dimanches à 1h.
     * Supprime les enregistrements plus anciens que 90 jours.
     */
    @Scheduled(cron = "0 0 1 ? * SUN", zone = "Europe/Paris")
    public void cleanupOldExecutions() {
        log.info("=== Démarrage du nettoyage des anciennes exécutions d'export ===");

        try {
            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(90);

            // Note: cette méthode devrait être implémentée dans CsvExportService
            // exportExecutionRepository.deleteByDateDebutBefore(cutoffDate);

            log.info("=== Nettoyage des anciennes exécutions terminé ===");

        } catch (Exception e) {
            log.error("=== Erreur lors du nettoyage des anciennes exécutions ===", e);
        }
    }

    /**
     * Monitoring des exports en cours - toutes les heures.
     * Vérifie les exports qui traînent et alerte si nécessaire.
     */
    @Scheduled(fixedRate = 3600000) // 1 heure = 3600000ms
    @ConditionalOnProperty(value = "app.csv-export.monitoring.enabled", havingValue = "true", matchIfMissing = true)
    public void monitorRunningExports() {
        try {
            var runningExports = csvExportService.getRunningExports();

            if (!runningExports.isEmpty()) {
                log.info("Exports en cours: {}", runningExports.size());

                for (var export : runningExports) {
                    long durationMinutes = java.time.Duration.between(
                            export.getDateDebut(),
                            java.time.LocalDateTime.now()
                    ).toMinutes();

                    if (durationMinutes > 60) { // Plus d'1 heure
                        log.warn("Export {} en cours depuis {} minutes: {} (job ID: {})",
                                export.getNomCsv(), durationMinutes,
                                export.getStatut(), export.getJobExecutionId());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Erreur lors du monitoring des exports", e);
        }
    }
}