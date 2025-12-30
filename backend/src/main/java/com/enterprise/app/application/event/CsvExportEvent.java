package com.enterprise.app.application.event;

import com.enterprise.app.domain.model.csvexport.StatutExecution;

import java.time.LocalDateTime;

/**
 * Event déclenché lors des changements d'état d'un export CSV.
 * Utilise les Records Java 14+ pour les événements.
 */
public record CsvExportEvent(
    Long jobExecutionId,
    String nomCsv,
    StatutExecution statut,
    LocalDateTime timestamp,
    String message,
    Integer nbLignesExportees
) {

    /**
     * Factory method pour créer un event de démarrage d'export.
     */
    public static CsvExportEvent started(Long jobExecutionId, String nomCsv) {
        return new CsvExportEvent(
            jobExecutionId,
            nomCsv,
            StatutExecution.EN_COURS,
            LocalDateTime.now(),
            "Export démarré",
            null
        );
    }

    /**
     * Factory method pour créer un event de succès d'export.
     */
    public static CsvExportEvent completed(Long jobExecutionId, String nomCsv, Integer nbLignes) {
        return new CsvExportEvent(
            jobExecutionId,
            nomCsv,
            StatutExecution.TERMINE,
            LocalDateTime.now(),
            "Export terminé avec succès",
            nbLignes
        );
    }

    /**
     * Factory method pour créer un event d'échec d'export.
     */
    public static CsvExportEvent failed(Long jobExecutionId, String nomCsv, String errorMessage) {
        return new CsvExportEvent(
            jobExecutionId,
            nomCsv,
            StatutExecution.ECHEC,
            LocalDateTime.now(),
            "Export échoué: " + errorMessage,
            null
        );
    }

    /**
     * Vérifie si l'event indique un succès.
     */
    public boolean isSuccess() {
        return statut.isSuccessful();
    }

    /**
     * Vérifie si l'event indique un échec.
     */
    public boolean isFailure() {
        return statut.isFailed();
    }

    /**
     * Retourne une représentation pour les logs.
     */
    public String toLogString() {
        return "[%s] %s - %s (%s)".formatted(
            nomCsv, statut.getDescription(), message,
            nbLignesExportees != null ? nbLignesExportees + " lignes" : "N/A"
        );
    }
}