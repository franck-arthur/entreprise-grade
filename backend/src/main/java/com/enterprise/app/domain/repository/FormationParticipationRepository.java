package com.enterprise.app.domain.repository;

import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.domain.model.StatutParticipation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormationParticipationRepository {

    Optional<FormationParticipation> findById(UUID id);

    Optional<FormationParticipation> findByFormationIdAndUserId(UUID formationId, UUID userId);

    List<FormationParticipation> findByFormationId(UUID formationId);

    List<FormationParticipation> findByFormationIdAndStatutParticipation(UUID formationId, StatutParticipation statut);

    Page<FormationParticipation> findByUserId(UUID userId, Pageable pageable);

    Page<FormationParticipation> findByUserIdAndStatutParticipation(UUID userId, StatutParticipation statut, Pageable pageable);

    FormationParticipation save(FormationParticipation participation);

    void deleteById(UUID id);

    void deleteByFormationIdAndUserId(UUID formationId, UUID userId);

    boolean existsByFormationIdAndUserId(UUID formationId, UUID userId);

    int countByFormationIdAndStatutParticipation(UUID formationId, StatutParticipation statut);

    int countByFormationId(UUID formationId);

    long count();
}