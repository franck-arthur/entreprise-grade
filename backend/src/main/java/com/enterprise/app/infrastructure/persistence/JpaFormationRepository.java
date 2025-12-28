package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Formation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaFormationRepository extends JpaRepository<Formation, Long>, JpaSpecificationExecutor<Formation> {

    @Query("SELECT COUNT(fp) FROM FormationParticipation fp WHERE fp.formation.id = :formationId")
    int countParticipantsInscrits(@Param("formationId") Long formationId);

    @Query("SELECT CASE WHEN COUNT(fp) >= f.nbParticipants THEN true ELSE false END " +
           "FROM Formation f LEFT JOIN FormationParticipation fp ON fp.formation.id = f.id " +
           "WHERE f.id = :formationId GROUP BY f.id, f.nbParticipants")
    Boolean isFormationComplete(@Param("formationId") Long formationId);

    @Query("SELECT f FROM Formation f " +
           "LEFT JOIN FETCH f.secteur " +
           "LEFT JOIN FETCH f.region " +
           "WHERE f.id = :id")
    Optional<Formation> findByIdWithRelations(@Param("id") Long id);
}