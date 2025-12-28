package com.enterprise.app.domain.repository;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;

public interface FormationRepository {

    Optional<Formation> findById(Long id);

    Page<Formation> findByFilters(Long secteurId, Long regionId, ModaliteFormation modalite,
                                  FormationStatut statut, Pageable pageable);

    Formation save(Formation formation);

    void deleteById(Long id);

    boolean existsById(Long id);

    long count();

    int countParticipantsInscrits(Long formationId);

    boolean isFormationComplete(Long formationId);
}