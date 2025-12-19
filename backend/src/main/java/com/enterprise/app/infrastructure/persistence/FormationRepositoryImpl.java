package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.domain.repository.FormationRepository;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FormationRepositoryImpl implements FormationRepository {

    private final JpaFormationRepository jpaRepository;

    @Override
    public Optional<Formation> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Formation> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Page<Formation> findByFilters(String secteur, String region, ModaliteFormation modalite,
                                       FormationStatut statut, Pageable pageable) {
        Page<Formation> formations = jpaRepository.findByFilters(secteur, region, modalite, pageable);

        if (statut != null) {
            // Le filtrage par statut est fait côté application car le statut est calculé
            List<Formation> filteredFormations = formations.getContent()
                    .stream()
                    .filter(formation -> formation.getStatut() == statut)
                    .collect(Collectors.toList());

            return new PageImpl<>(filteredFormations, pageable, filteredFormations.size());
        }

        return formations;
    }

    @Override
    public Page<Formation> findBySecteur(String secteur, Pageable pageable) {
        return jpaRepository.findBySecteur(secteur, pageable);
    }

    @Override
    public Page<Formation> findByRegion(String region, Pageable pageable) {
        return jpaRepository.findByRegion(region, pageable);
    }

    @Override
    public Page<Formation> findByModalite(ModaliteFormation modalite, Pageable pageable) {
        return jpaRepository.findByModalite(modalite, pageable);
    }

    @Override
    public Page<Formation> findByDateFormationBetween(LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        return jpaRepository.findByDateFormationBetween(dateDebut, dateFin, pageable);
    }

    @Override
    public Formation save(Formation formation) {
        formation.validerCoherenceDates();
        formation.validerNbParticipants();
        return jpaRepository.save(formation);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public int countParticipantsInscrits(UUID formationId) {
        return jpaRepository.countParticipantsInscrits(formationId);
    }

    @Override
    public boolean isFormationComplete(UUID formationId) {
        Boolean result = jpaRepository.isFormationComplete(formationId);
        return result != null && result;
    }

    @Override
    public Optional<FormationProjection> findProjectionById(UUID id) {
        return jpaRepository.findProjectionById(id);
    }

    @Override
    public List<FormationProjection> findAllProjections() {
        return jpaRepository.findAllProjections();
    }

    @Override
    public Page<FormationProjection> findProjectionsByFilters(String secteur, String region, ModaliteFormation modalite, Pageable pageable) {
        return jpaRepository.findProjectionsByFilters(secteur, region, modalite, pageable);
    }
}