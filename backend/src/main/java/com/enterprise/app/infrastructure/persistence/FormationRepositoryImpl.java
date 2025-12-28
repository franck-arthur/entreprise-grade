package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.domain.repository.FormationRepository;
import com.enterprise.app.infrastructure.persistence.specification.FormationSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FormationRepositoryImpl implements FormationRepository {

    private final JpaFormationRepository jpaRepository;

    @Override
    public Optional<Formation> findById(Long id) {
        return jpaRepository.findByIdWithRelations(id);
    }

    @Override
    public Page<Formation> findByFilters(Long secteurId, Long regionId, ModaliteFormation modalite,
                                         FormationStatut statut, Pageable pageable) {
        var specification = FormationSpecifications.withDynamicFilters(secteurId, regionId, modalite, statut);
        return jpaRepository.findAll(specification, pageable);
    }

    @Override
    public Formation save(Formation formation) {
        return jpaRepository.save(formation);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public int countParticipantsInscrits(Long formationId) {
        return jpaRepository.countParticipantsInscrits(formationId);
    }

    @Override
    public boolean isFormationComplete(Long formationId) {
        Boolean result = jpaRepository.isFormationComplete(formationId);
        return result != null && result;
    }
}