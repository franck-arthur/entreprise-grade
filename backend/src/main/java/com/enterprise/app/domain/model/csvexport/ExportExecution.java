package com.enterprise.app.domain.model.csvexport;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité pour tracker l'exécution des exports CSV.
 * Permet de suivre les métriques et statuts des exports.
 */
@Entity
@Table(
    name = "export_execution",
    schema = "export_csv",
    indexes = {
        @Index(name = "idx_export_exec_date", columnList = "date_debut"),
        @Index(name = "idx_export_exec_csv_date", columnList = "nom_csv, date_debut"),
        @Index(name = "idx_export_exec_statut", columnList = "statut")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"jobExecutionId", "nomCsv", "dateDebut"})
public class ExportExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    @Column(name = "nom_csv", nullable = false, length = 50)
    private String nomCsv;

    @CreatedDate
    @Column(name = "date_debut", nullable = false, updatable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20)
    private StatutExecution statut;

    @Column(name = "nb_lignes_exportees")
    private Integer nbLignesExportees;

    @Column(name = "chemin_fichier", length = 500)
    private String cheminFichier;

    @Column(name = "message_erreur", columnDefinition = "TEXT")
    private String messageErreur;

    @Column(name = "duree_secondes", precision = 10, scale = 3)
    private BigDecimal dureeSecondes;

    /**
     * Marque l'export comme démarré.
     */
    public void markAsStarted() {
        this.statut = StatutExecution.EN_COURS;
        this.dateDebut = LocalDateTime.now();
    }

    /**
     * Marque l'export comme terminé avec succès.
     */
    public void markAsCompleted(Integer nbLignes, String cheminFichier) {
        this.statut = StatutExecution.TERMINE;
        this.dateFin = LocalDateTime.now();
        this.nbLignesExportees = nbLignes;
        this.cheminFichier = cheminFichier;
        this.calculateDuration();
    }

    /**
     * Marque l'export comme échoué.
     */
    public void markAsFailed(String messageErreur) {
        this.statut = StatutExecution.ECHEC;
        this.dateFin = LocalDateTime.now();
        this.messageErreur = messageErreur;
        this.calculateDuration();
    }

    /**
     * Calcule la durée d'exécution avec précision.
     * Utilise les API Duration Java 8+ améliorées.
     */
    private void calculateDuration() {
        if (dateDebut != null && dateFin != null) {
            var duration = java.time.Duration.between(dateDebut, dateFin);
            // Précision en millisecondes convertie en secondes avec décimales
            double totalSeconds = duration.toMillis() / 1000.0;
            this.dureeSecondes = BigDecimal.valueOf(totalSeconds);
        }
    }

    /**
     * Retourne la durée formatée pour l'affichage.
     * Utilise les nouvelles fonctionnalités de formatage Java 21.
     */
    public String getFormattedDuration() {
        if (dureeSecondes == null) {
            return "N/A";
        }

        int totalSeconds = dureeSecondes.intValue();

        if (totalSeconds < 60) {
            return "%.1fs".formatted(dureeSecondes.doubleValue());
        } else if (totalSeconds < 3600) {
            return "%dm %ds".formatted(totalSeconds / 60, totalSeconds % 60);
        } else {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int remainingSeconds = totalSeconds % 60;
            return "%dh %dm %ds".formatted(hours, minutes, remainingSeconds);
        }
    }

    /**
     * Vérifie si l'export est terminé (succès ou échec).
     * Utilise les nouvelles méthodes de StatutExecution.
     */
    public boolean isCompleted() {
        return statut != null && statut.isCompleted();
    }

    /**
     * Vérifie si l'export a réussi.
     * Délègue au StatutExecution pour la logique métier.
     */
    public boolean isSuccessful() {
        return statut != null && statut.isSuccessful();
    }

    /**
     * Vérifie si l'export a échoué.
     * Utilise pattern matching via StatutExecution.
     */
    public boolean isFailed() {
        return statut != null && statut.isFailed();
    }

    /**
     * Retourne le niveau de priorité pour l'affichage.
     */
    public StatutExecution.Priority getPriority() {
        return statut != null ? statut.getPriority() : StatutExecution.Priority.INFO;
    }

    /**
     * Méthode utilitaire pour créer un nouveau tracking d'export.
     */
    public static ExportExecution create(String nomCsv, Long jobExecutionId) {
        return ExportExecution.builder()
                .nomCsv(nomCsv)
                .jobExecutionId(jobExecutionId)
                .statut(StatutExecution.EN_COURS)
                .dateDebut(LocalDateTime.now())
                .build();
    }
}