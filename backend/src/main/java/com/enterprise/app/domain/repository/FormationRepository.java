package com.enterprise.app.domain.repository;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormationRepository {

    Optional<Formation> findById(UUID id);

    Page<Formation> findAll(Pageable pageable);

    Page<Formation> findByFilters(
        String secteur,
        String region,
        ModaliteFormation modalite,
        FormationStatut statut,
        Pageable pageable
    );

    Page<Formation> findBySecteur(String secteur, Pageable pageable);

    Page<Formation> findByRegion(String region, Pageable pageable);

    Page<Formation> findByModalite(ModaliteFormation modalite, Pageable pageable);

    Page<Formation> findByDateFormationBetween(LocalDate dateDebut, LocalDate dateFin, Pageable pageable);

    Formation save(Formation formation);

    void deleteById(UUID id);

    boolean existsById(UUID id);

    long count();

    int countParticipantsInscrits(UUID formationId);

    boolean isFormationComplete(UUID formationId);

    // Méthodes optimisées avec projections
    Optional<FormationProjection> findProjectionById(UUID id);

    List<FormationProjection> findAllProjections();

    Page<FormationProjection> findProjectionsByFilters(
        String secteur,
        String region,
        ModaliteFormation modalite,
        Pageable pageable
    );
}