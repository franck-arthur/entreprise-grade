package com.enterprise.app.infrastructure.persistence.specification;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FormationSpecifications {

    public static final String ATTRIBUT_DATE_FORMATION = "dateFormation";
    public static final String ATTRIBUT_HEURE_DEBUT = "heureDebut";
    public static final String ATTRIBUT_HEURE_FIN = "heureFin";
    public static final String ATTRIBUT_SECTEUR = "secteur";
    public static final String ATTRIBUT_REGION = "region";
    public static final String ATTRIBUT_ID = "id";
    public static final String ATTRIBUT_MODALITE = "modalite";

    public FormationSpecifications() {
    }

    public static Specification<Formation> hasSecteur(Long secteurId) {
        return (root, query, criteriaBuilder) -> {
            if (secteurId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(ATTRIBUT_SECTEUR).get(ATTRIBUT_ID), secteurId);
        };
    }

    public static Specification<Formation> hasRegion(Long regionId) {
        return (root, query, criteriaBuilder) -> {
            if (regionId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(ATTRIBUT_REGION).get(ATTRIBUT_ID), regionId);
        };
    }

    public static Specification<Formation> hasModalite(ModaliteFormation modalite) {
        return (root, query, criteriaBuilder) -> {
            if (modalite == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(ATTRIBUT_MODALITE), modalite);
        };
    }

    public static Specification<Formation> hasStatut(FormationStatut statut) {
        return (root, query, criteriaBuilder) -> {
            if (statut == null) {
                return criteriaBuilder.conjunction();
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDate currentDate = now.toLocalDate();

            return switch (statut) {
                case A_VENIR ->
                    // Formation à venir : soit date future, soit même date avec heure future
                    criteriaBuilder.or(
                        criteriaBuilder.greaterThan(root.get(ATTRIBUT_DATE_FORMATION), currentDate),
                        criteriaBuilder.and(
                            criteriaBuilder.equal(root.get(ATTRIBUT_DATE_FORMATION), currentDate),
                            criteriaBuilder.greaterThan(root.get(ATTRIBUT_HEURE_DEBUT), now.toLocalTime())
                        )
                    );
                case EN_COURS ->
                    // Formation en cours : même date, heure début <= maintenant <= heure fin
                    criteriaBuilder.and(
                        criteriaBuilder.equal(root.get(ATTRIBUT_DATE_FORMATION), currentDate),
                        criteriaBuilder.lessThanOrEqualTo(root.get(ATTRIBUT_HEURE_DEBUT), now.toLocalTime()),
                        criteriaBuilder.greaterThanOrEqualTo(root.get(ATTRIBUT_HEURE_FIN), now.toLocalTime())
                    );
                case TERMINEE ->
                    // Formation terminée : soit date passée, soit même date avec heure passée
                    criteriaBuilder.or(
                        criteriaBuilder.lessThan(root.get(ATTRIBUT_DATE_FORMATION), currentDate),
                        criteriaBuilder.and(
                            criteriaBuilder.equal(root.get(ATTRIBUT_DATE_FORMATION), currentDate),
                            criteriaBuilder.lessThan(root.get(ATTRIBUT_HEURE_FIN), now.toLocalTime())
                        )
                    );
            };

        };
    }

    public static Specification<Formation> withFetchJoins() {
        return (root, query, criteriaBuilder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch(ATTRIBUT_SECTEUR, JoinType.LEFT);
                root.fetch(ATTRIBUT_REGION, JoinType.LEFT);
                query.distinct(true);
            }
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<Formation> withDynamicFilters(
            Long secteurId,
            Long regionId,
            ModaliteFormation modalite,
            FormationStatut statut) {

        return Specification
            .where(withFetchJoins())
            .and(hasSecteur(secteurId))
            .and(hasRegion(regionId))
            .and(hasModalite(modalite))
            .and(hasStatut(statut));
    }
}