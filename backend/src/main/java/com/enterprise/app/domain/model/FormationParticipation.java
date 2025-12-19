package com.enterprise.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "formation_participations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_formation_user", columnNames = {"formation_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_formation_participation_formation", columnList = "formation_id"),
        @Index(name = "idx_formation_participation_user", columnList = "user_id"),
        @Index(name = "idx_formation_participation_statut", columnList = "statut_participation"),
        @Index(name = "idx_formation_participation_formation_statut", columnList = "formation_id,statut_participation")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "formation_id", nullable = false)
    private Formation formation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_participation", nullable = false)
    @Builder.Default
    private StatutParticipation statutParticipation = StatutParticipation.INSCRIT;

    @Column(name = "date_inscription", nullable = false)
    private LocalDateTime dateInscription;

    @Column(name = "date_presence")
    private LocalDateTime datePresence;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    private void prePersist() {
        if (dateInscription == null) {
            dateInscription = LocalDateTime.now();
        }
    }

    public void marquerPresent() {
        this.statutParticipation = StatutParticipation.PRESENT;
        this.datePresence = LocalDateTime.now();
    }

    public void marquerAbsent() {
        this.statutParticipation = StatutParticipation.ABSENT;
        this.datePresence = null;
    }

    public boolean isPresent() {
        return statutParticipation == StatutParticipation.PRESENT;
    }

    public boolean isAbsent() {
        return statutParticipation == StatutParticipation.ABSENT;
    }

    public boolean isInscrit() {
        return statutParticipation == StatutParticipation.INSCRIT;
    }

    public void annulerInscription() {
        this.statutParticipation = StatutParticipation.ANNULE;
    }

    public boolean peutEtreModifiee() {
        return formation.getStatut() == FormationStatut.A_VENIR;
    }
}