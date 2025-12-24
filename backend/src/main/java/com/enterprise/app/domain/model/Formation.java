package com.enterprise.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
    name = "formations",
    indexes = {
        @Index(name = "idx_formation_date", columnList = "date_formation"),
        @Index(name = "idx_formation_secteur", columnList = "secteur"),
        @Index(name = "idx_formation_region", columnList = "region"),
        @Index(name = "idx_formation_modalite", columnList = "modalite"),
        @Index(name = "idx_formation_date_secteur", columnList = "date_formation,secteur"),
        @Index(name = "idx_formation_date_region", columnList = "date_formation,region")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(nullable = false, length = 500)
    private String formateurs;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_formation", nullable = false)
    private LocalDate dateFormation;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(nullable = false, length = 100)
    private String secteur;

    @Column(nullable = false, length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModaliteFormation modalite;

    @Column(name = "nb_participants", nullable = false)
    private Integer nbParticipants;

    @Column(length = 200)
    private String lieu;

    @Column(length = 100)
    private String ville;

    @Column(name = "lien_participation", length = 500)
    private String lienParticipation;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public FormationStatut getStatut() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateDebutFormation = LocalDateTime.of(dateFormation, heureDebut);
        LocalDateTime dateFinFormation = LocalDateTime.of(dateFormation, heureFin);

        if (dateFinFormation.isBefore(now)) {
            return FormationStatut.TERMINEE;
        } else if (dateDebutFormation.isAfter(now)) {
            return FormationStatut.A_VENIR;
        } else {
            return FormationStatut.EN_COURS;
        }
    }

    public boolean isComplet() {
        return false; // Sera calculé via le repository
    }

    public boolean peutAccepterInscription() {
        return !isComplet() && getStatut() == FormationStatut.A_VENIR;
    }

    public LocalDateTime getDateDebutFormation() {
        return LocalDateTime.of(dateFormation, heureDebut);
    }

    public LocalDateTime getDateFinFormation() {
        return LocalDateTime.of(dateFormation, heureFin);
    }

    public void validerCoherenceDates() {
        if (heureFin.isBefore(heureDebut) || heureFin.equals(heureDebut)) {
            throw new IllegalArgumentException("L'heure de fin doit être postérieure à l'heure de début");
        }
    }

    public void validerNbParticipants() {
        if (nbParticipants == null || nbParticipants <= 0) {
            throw new IllegalArgumentException("Le nombre de participants doit être positif");
        }
    }

    public void validerModaliteEtChamps() {
        if (modalite == null) {
            throw new IllegalArgumentException("La modalité de formation est obligatoire");
        }

        switch (modalite) {
            case PRESENTIEL:
                if (ville == null || ville.trim().isEmpty()) {
                    throw new IllegalArgumentException("La ville est obligatoire pour une formation en présentiel");
                }
                if (lieu == null || lieu.trim().isEmpty()) {
                    throw new IllegalArgumentException("Le lieu est obligatoire pour une formation en présentiel");
                }
                break;
            case EN_LIGNE:
                if (lienParticipation == null || lienParticipation.trim().isEmpty()) {
                    throw new IllegalArgumentException("Le lien de participation est obligatoire pour une formation en ligne");
                }
                break;
        }
    }
}