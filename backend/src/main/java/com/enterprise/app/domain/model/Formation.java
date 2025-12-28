package com.enterprise.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.enterprise.app.domain.model.ModaliteFormation.PRESENTIEL;
import static org.springframework.util.StringUtils.hasText;

@Entity
@Table(
    name = "formations",
    indexes = {
        @Index(name = "idx_formation_secteur", columnList = "secteur_id"),
        @Index(name = "idx_formation_region", columnList = "region_id"),
        @Index(name = "idx_formation_modalite", columnList = "modalite"),
        @Index(name = "idx_formation_date_heure", columnList = "date_formation, date_debut, date_fin"),
        // Note: Les index calculés (date_formation + heure_debut/fin) sont créés en SQL
        // car JPA ne supporte pas nativement les index sur expressions calculées
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secteur_id", nullable = false)
    private Secteur secteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

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
        // Le calcul sera fait dans le mapper avec les données de participations
        return false;
    }

    public Boolean getComplet() {
        return isComplet();
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

}