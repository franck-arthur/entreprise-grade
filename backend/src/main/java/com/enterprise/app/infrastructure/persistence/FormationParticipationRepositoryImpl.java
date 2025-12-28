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

@Component
@RequiredArgsConstructor
public class FormationParticipationRepositoryImpl implements FormationParticipationRepository {

    private final JpaFormationParticipationRepository jpaRepository;

    @Override
    public Optional<FormationParticipation> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<FormationParticipation> findByFormationIdAndUserId(Long formationId, Long userId) {
        return jpaRepository.findByFormationIdAndUserId(formationId, userId);
    }

    @Override
    public List<FormationParticipation> findByFormationId(Long formationId) {
        return jpaRepository.findByFormationId(formationId);
    }

    @Override
    public List<FormationParticipation> findByFormationIdAndStatutParticipation(Long formationId, StatutParticipation statut) {
        return jpaRepository.findByFormationIdAndStatutParticipation(formationId, statut);
    }

    @Override
    public Page<FormationParticipation> findByUserIdAndStatutParticipation(Long userId, StatutParticipation statut, Pageable pageable) {
        return jpaRepository.findByUserIdAndStatutParticipation(userId, statut, pageable);
    }

    @Override
    public FormationParticipation save(FormationParticipation participation) {
        return jpaRepository.save(participation);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByFormationIdAndUserId(Long formationId, Long userId) {
        jpaRepository.deleteByFormationIdAndUserId(formationId, userId);
    }

    @Override
    public boolean existsByFormationIdAndUserId(Long formationId, Long userId) {
        return jpaRepository.existsByFormationIdAndUserId(formationId, userId);
    }

    @Override
    public int countByFormationIdAndStatutParticipation(Long formationId, StatutParticipation statut) {
        return jpaRepository.countByFormationIdAndStatutParticipation(formationId, statut);
    }

    @Override
    public int countByFormationId(Long formationId) {
        return jpaRepository.countByFormationIdExcludingCancelled(formationId);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}