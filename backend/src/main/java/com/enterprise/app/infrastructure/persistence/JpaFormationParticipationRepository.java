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

@Repository
public interface JpaFormationParticipationRepository extends JpaRepository<FormationParticipation, Long> {

    Optional<FormationParticipation> findByFormationIdAndUserId(Long formationId, Long userId);

    List<FormationParticipation> findByFormationId(Long formationId);

    List<FormationParticipation> findByFormationIdAndStatutParticipation(Long formationId, StatutParticipation statut);

    Page<FormationParticipation> findByUserIdAndStatutParticipation(Long userId, StatutParticipation statut, Pageable pageable);

    void deleteByFormationIdAndUserId(Long formationId, Long userId);

    boolean existsByFormationIdAndUserId(Long formationId, Long userId);

    int countByFormationIdAndStatutParticipation(Long formationId, StatutParticipation statut);

    @Query("SELECT COUNT(fp) FROM FormationParticipation fp WHERE fp.formation.id = :formationId")
    int countByFormationIdExcludingCancelled(@Param("formationId") Long formationId);
}