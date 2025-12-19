package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.domain.model.StatutParticipation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFormationParticipationRepository extends JpaRepository<FormationParticipation, UUID> {

    Optional<FormationParticipation> findByFormationIdAndUserId(UUID formationId, UUID userId);

    List<FormationParticipation> findByFormationId(UUID formationId);

    List<FormationParticipation> findByFormationIdAndStatutParticipation(UUID formationId, StatutParticipation statut);

    Page<FormationParticipation> findByUserId(UUID userId, Pageable pageable);

    Page<FormationParticipation> findByUserIdAndStatutParticipation(UUID userId, StatutParticipation statut, Pageable pageable);

    void deleteByFormationIdAndUserId(UUID formationId, UUID userId);

    boolean existsByFormationIdAndUserId(UUID formationId, UUID userId);

    int countByFormationIdAndStatutParticipation(UUID formationId, StatutParticipation statut);

    @Query("SELECT COUNT(fp) FROM FormationParticipation fp WHERE fp.formation.id = :formationId AND fp.statutParticipation != 'ANNULE'")
    int countByFormationIdExcludingCancelled(@Param("formationId") UUID formationId);

    int countByFormationId(UUID formationId);
}