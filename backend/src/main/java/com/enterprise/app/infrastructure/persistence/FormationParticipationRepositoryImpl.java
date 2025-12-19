package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.domain.model.StatutParticipation;
import com.enterprise.app.domain.repository.FormationParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FormationParticipationRepositoryImpl implements FormationParticipationRepository {

    private final JpaFormationParticipationRepository jpaRepository;

    @Override
    public Optional<FormationParticipation> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<FormationParticipation> findByFormationIdAndUserId(UUID formationId, UUID userId) {
        return jpaRepository.findByFormationIdAndUserId(formationId, userId);
    }

    @Override
    public List<FormationParticipation> findByFormationId(UUID formationId) {
        return jpaRepository.findByFormationId(formationId);
    }

    @Override
    public List<FormationParticipation> findByFormationIdAndStatutParticipation(UUID formationId, StatutParticipation statut) {
        return jpaRepository.findByFormationIdAndStatutParticipation(formationId, statut);
    }

    @Override
    public Page<FormationParticipation> findByUserId(UUID userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<FormationParticipation> findByUserIdAndStatutParticipation(UUID userId, StatutParticipation statut, Pageable pageable) {
        return jpaRepository.findByUserIdAndStatutParticipation(userId, statut, pageable);
    }

    @Override
    public FormationParticipation save(FormationParticipation participation) {
        return jpaRepository.save(participation);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByFormationIdAndUserId(UUID formationId, UUID userId) {
        jpaRepository.deleteByFormationIdAndUserId(formationId, userId);
    }

    @Override
    public boolean existsByFormationIdAndUserId(UUID formationId, UUID userId) {
        return jpaRepository.existsByFormationIdAndUserId(formationId, userId);
    }

    @Override
    public int countByFormationIdAndStatutParticipation(UUID formationId, StatutParticipation statut) {
        return jpaRepository.countByFormationIdAndStatutParticipation(formationId, statut);
    }

    @Override
    public int countByFormationId(UUID formationId) {
        return jpaRepository.countByFormationIdExcludingCancelled(formationId);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}