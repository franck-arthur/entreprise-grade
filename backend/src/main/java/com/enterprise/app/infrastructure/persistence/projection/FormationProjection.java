package com.enterprise.app.infrastructure.persistence.projection;

import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Projection pour Formation avec comptage des participants inscrits en une seule requête.
 * Permet d'éviter les requêtes N+1 lors du mapping vers DTO.
 */
public interface FormationProjection {

    UUID getId();
    String getLibelle();
    String getFormateurs();
    String getDescription();
    LocalDate getDateFormation();
    LocalTime getHeureDebut();
    LocalTime getHeureFin();
    String getSecteur();
    String getRegion();
    ModaliteFormation getModalite();
    Integer getNbParticipants();
    String getLieu();
    String getVille();
    String getLienParticipation();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    // Champ calculé pour les participants inscrits
    Integer getNbParticipantsInscrits();

    // Méthode par défaut pour calculer le statut dynamiquement
    default FormationStatut getStatut() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateDebutFormation = LocalDateTime.of(getDateFormation(), getHeureDebut());
        LocalDateTime dateFinFormation = LocalDateTime.of(getDateFormation(), getHeureFin());

        if (dateFinFormation.isBefore(now)) {
            return FormationStatut.TERMINEE;
        } else if (dateDebutFormation.isAfter(now)) {
            return FormationStatut.A_VENIR;
        } else {
            return FormationStatut.EN_COURS;
        }
    }

    // Méthode par défaut pour calculer si la formation est complète
    default Boolean isComplet() {
        Integer inscrits = getNbParticipantsInscrits();
        Integer max = getNbParticipants();
        return inscrits != null && max != null && inscrits >= max;
    }
}