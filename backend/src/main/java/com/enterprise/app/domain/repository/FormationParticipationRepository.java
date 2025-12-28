package com.enterprise.app.domain.repository;

import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.domain.model.StatutParticipation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface FormationParticipationRepository {

    Optional<FormationParticipation> findById(Long id);

    Optional<FormationParticipation> findByFormationIdAndUserId(Long formationId, Long userId);

    List<FormationParticipation> findByFormationId(Long formationId);

    List<FormationParticipation> findByFormationIdAndStatutParticipation(Long formationId, StatutParticipation statut);

    Page<FormationParticipation> findByUserIdAndStatutParticipation(Long userId, StatutParticipation statut, Pageable pageable);

    FormationParticipation save(FormationParticipation participation);

    void deleteById(Long id);

    void deleteByFormationIdAndUserId(Long formationId, Long userId);

    boolean existsByFormationIdAndUserId(Long formationId, Long userId);

    int countByFormationIdAndStatutParticipation(Long formationId, StatutParticipation statut);

    int countByFormationId(Long formationId);

    long count();
}