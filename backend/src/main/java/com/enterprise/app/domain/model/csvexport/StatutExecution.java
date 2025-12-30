package com.enterprise.app.domain.model.csvexport;

/**
 * Énumération des statuts d'exécution d'un export CSV.
 * Utilise les nouvelles fonctionnalités Java 21 pour les méthodes utilitaires.
 */
public enum StatutExecution {
    EN_COURS("En cours d'exécution", false, true),
    TERMINE("Terminé avec succès", true, false),
    ECHEC("Échec d'exécution", true, false),
    ANNULE("Annulé par l'utilisateur", true, false);

    private final String description;
    private final boolean completed;
    private final boolean active;

    StatutExecution(String description, boolean completed, boolean active) {
        this.description = description;
        this.completed = completed;
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Vérifie si le statut indique un succès.
     * Utilise pattern matching Java 21.
     */
    public boolean isSuccessful() {
        return switch (this) {
            case TERMINE -> true;
            case EN_COURS, ECHEC, ANNULE -> false;
        };
    }

    /**
     * Vérifie si le statut indique un échec.
     * Utilise pattern matching avec conditions.
     */
    public boolean isFailed() {
        return switch (this) {
            case ECHEC, ANNULE -> true;
            case EN_COURS, TERMINE -> false;
        };
    }

    /**
     * Retourne le niveau de priorité pour l'affichage UI.
     */
    public Priority getPriority() {
        return switch (this) {
            case EN_COURS -> Priority.INFO;
            case TERMINE -> Priority.SUCCESS;
            case ECHEC -> Priority.ERROR;
            case ANNULE -> Priority.WARNING;
        };
    }

    /**
     * Définit les niveaux de priorité pour l'UI.
     */
    public enum Priority {
        INFO, SUCCESS, WARNING, ERROR
    }

    /**
     * Méthode utilitaire pour obtenir tous les statuts terminés.
     */
    public static StatutExecution[] getCompletedStatuses() {
        return switch (1) {
            case 1 -> new StatutExecution[]{TERMINE, ECHEC, ANNULE};
            default -> new StatutExecution[0];
        };
    }

    /**
     * Factory method pour créer un statut basé sur une condition.
     * Utilise la logique conditionnelle moderne Java 21.
     */
    public static StatutExecution fromCondition(boolean success, boolean cancelled) {
        if (cancelled) {
            return ANNULE;
        }
        return success ? TERMINE : ECHEC;
    }
}