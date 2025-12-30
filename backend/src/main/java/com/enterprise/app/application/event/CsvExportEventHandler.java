package com.enterprise.app.application.event;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Gestionnaire d'événements pour les exports CSV.
 * Utilise Spring 6 Event Handling avec observabilité Micrometer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsvExportEventHandler {

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer.Sample> timerSamples = new ConcurrentHashMap<>();

    private Counter exportStartedCounter;
    private Counter exportCompletedCounter;
    private Counter exportFailedCounter;
    private Timer exportDurationTimer;

    @PostConstruct
    public void initMetrics() {
        exportStartedCounter = Counter.builder("csv.export.started")
            .description("Nombre d'exports CSV démarrés")
            .register(meterRegistry);

        exportCompletedCounter = Counter.builder("csv.export.completed")
            .description("Nombre d'exports CSV terminés avec succès")
            .register(meterRegistry);

        exportFailedCounter = Counter.builder("csv.export.failed")
            .description("Nombre d'exports CSV échoués")
            .register(meterRegistry);

        exportDurationTimer = Timer.builder("csv.export.duration")
            .description("Durée des exports CSV")
            .register(meterRegistry);
    }

    /**
     * Gère les événements d'export CSV de manière asynchrone.
     * Utilise les Virtual Threads via @Async et l'observabilité Spring 6.
     */
    @Async
    @EventListener
    @Observed(
        name = "csv.export.event",
        contextualName = "csv-export-event-handler",
        lowCardinalityKeyValues = {"component", "event-handler", "domain", "csv-export"}
    )
    public void handleCsvExportEvent(CsvExportEvent event) {
        log.info("Traitement event CSV export: {}", event.toLogString());

        switch (event.statut()) {
            case EN_COURS -> handleExportStarted(event);
            case TERMINE -> handleExportCompleted(event);
            case ECHEC -> handleExportFailed(event);
            case ANNULE -> handleExportCancelled(event);
        }
    }

    /**
     * Traite le démarrage d'un export.
     */
    @Observed(name = "csv.export.started.handler")
    private void handleExportStarted(CsvExportEvent event) {
        exportStartedCounter.increment();

        // Démarrer un timer pour mesurer la durée
        Timer.Sample sample = Timer.start(meterRegistry);
        timerSamples.put(event.jobExecutionId().toString(), sample);

        log.info("Export CSV démarré: {} (Job ID: {})",
                event.nomCsv(), event.jobExecutionId());
    }

    /**
     * Traite la réussite d'un export.
     */
    @Observed(name = "csv.export.completed.handler")
    private void handleExportCompleted(CsvExportEvent event) {
        exportCompletedCounter.increment();

        // Arrêter le timer et enregistrer la durée
        String jobId = event.jobExecutionId().toString();
        Timer.Sample sample = timerSamples.remove(jobId);
        if (sample != null) {
            sample.stop(exportDurationTimer);
        }

        log.info("Export CSV terminé avec succès: {} - {} lignes (Job ID: {})",
                event.nomCsv(), event.nbLignesExportees(), event.jobExecutionId());

        // Métrique spécifique pour le nombre de lignes
        if (event.nbLignesExportees() != null) {
            meterRegistry.gauge("csv.export.lines.exported", event.nbLignesExportees());
        }
    }

    /**
     * Traite l'échec d'un export.
     */
    @Observed(name = "csv.export.failed.handler")
    private void handleExportFailed(CsvExportEvent event) {
        exportFailedCounter.increment();

        // Nettoyer le timer en cours
        String jobId = event.jobExecutionId().toString();
        timerSamples.remove(jobId);

        log.error("Export CSV échoué: {} - {} (Job ID: {})",
                event.nomCsv(), event.message(), event.jobExecutionId());
    }

    /**
     * Traite l'annulation d'un export.
     */
    @Observed(name = "csv.export.cancelled.handler")
    private void handleExportCancelled(CsvExportEvent event) {
        // Nettoyer le timer en cours
        String jobId = event.jobExecutionId().toString();
        timerSamples.remove(jobId);

        log.warn("Export CSV annulé: {} (Job ID: {})",
                event.nomCsv(), event.jobExecutionId());
    }

    /**
     * Méthode utilitaire pour publier des events depuis d'autres services.
     * Pattern moderne Spring 6 avec ApplicationEventPublisher intégré.
     */
    public void publishExportEvent(CsvExportEvent event) {
        // Cette méthode pourrait être utilisée pour publier des événements
        // depuis d'autres composants si nécessaire
        log.debug("Publication d'un event CSV export: {}", event.toLogString());
    }
}