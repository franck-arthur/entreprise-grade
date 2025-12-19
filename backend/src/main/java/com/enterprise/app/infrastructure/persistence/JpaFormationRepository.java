package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFormationRepository extends JpaRepository<Formation, UUID> {

    Page<Formation> findBySecteur(String secteur, Pageable pageable);

    Page<Formation> findByRegion(String region, Pageable pageable);

    Page<Formation> findByModalite(ModaliteFormation modalite, Pageable pageable);

    Page<Formation> findByDateFormationBetween(LocalDate dateDebut, LocalDate dateFin, Pageable pageable);

    @Query("SELECT f FROM Formation f WHERE " +
           "(:secteur IS NULL OR f.secteur = :secteur) AND " +
           "(:region IS NULL OR f.region = :region) AND " +
           "(:modalite IS NULL OR f.modalite = :modalite)")
    Page<Formation> findByFilters(
        @Param("secteur") String secteur,
        @Param("region") String region,
        @Param("modalite") ModaliteFormation modalite,
        Pageable pageable
    );

    @Query("SELECT COUNT(fp) FROM FormationParticipation fp WHERE fp.formation.id = :formationId AND fp.statutParticipation != 'ANNULE'")
    int countParticipantsInscrits(@Param("formationId") UUID formationId);

    @Query("SELECT CASE WHEN COUNT(fp) >= f.nbParticipants THEN true ELSE false END " +
           "FROM Formation f LEFT JOIN FormationParticipation fp ON fp.formation.id = f.id AND fp.statutParticipation != 'ANNULE' " +
           "WHERE f.id = :formationId GROUP BY f.id, f.nbParticipants")
    Boolean isFormationComplete(@Param("formationId") UUID formationId);

    // Projection pour optimiser les requêtes avec comptage des participants
    @Query("SELECT f.id as id, f.libelle as libelle, f.formateurs as formateurs, f.description as description, " +
           "f.dateFormation as dateFormation, f.heureDebut as heureDebut, f.heureFin as heureFin, " +
           "f.secteur as secteur, f.region as region, f.modalite as modalite, f.nbParticipants as nbParticipants, " +
           "f.lieu as lieu, f.ville as ville, f.lienParticipation as lienParticipation, " +
           "f.createdAt as createdAt, f.updatedAt as updatedAt, " +
           "CAST(COUNT(fp) AS int) as nbParticipantsInscrits " +
           "FROM Formation f LEFT JOIN FormationParticipation fp ON fp.formation.id = f.id AND fp.statutParticipation != 'ANNULE' " +
           "WHERE f.id = :id GROUP BY f.id")
    Optional<FormationProjection> findProjectionById(@Param("id") UUID id);

    @Query("SELECT f.id as id, f.libelle as libelle, f.formateurs as formateurs, f.description as description, " +
           "f.dateFormation as dateFormation, f.heureDebut as heureDebut, f.heureFin as heureFin, " +
           "f.secteur as secteur, f.region as region, f.modalite as modalite, f.nbParticipants as nbParticipants, " +
           "f.lieu as lieu, f.ville as ville, f.lienParticipation as lienParticipation, " +
           "f.createdAt as createdAt, f.updatedAt as updatedAt, " +
           "CAST(COUNT(fp) AS int) as nbParticipantsInscrits " +
           "FROM Formation f LEFT JOIN FormationParticipation fp ON fp.formation.id = f.id AND fp.statutParticipation != 'ANNULE' " +
           "GROUP BY f.id")
    List<FormationProjection> findAllProjections();

    @Query("SELECT f.id as id, f.libelle as libelle, f.formateurs as formateurs, f.description as description, " +
           "f.dateFormation as dateFormation, f.heureDebut as heureDebut, f.heureFin as heureFin, " +
           "f.secteur as secteur, f.region as region, f.modalite as modalite, f.nbParticipants as nbParticipants, " +
           "f.lieu as lieu, f.ville as ville, f.lienParticipation as lienParticipation, " +
           "f.createdAt as createdAt, f.updatedAt as updatedAt, " +
           "CAST(COUNT(fp) AS int) as nbParticipantsInscrits " +
           "FROM Formation f LEFT JOIN FormationParticipation fp ON fp.formation.id = f.id AND fp.statutParticipation != 'ANNULE' " +
           "WHERE (:secteur IS NULL OR f.secteur = :secteur) AND " +
           "(:region IS NULL OR f.region = :region) AND " +
           "(:modalite IS NULL OR f.modalite = :modalite) " +
           "GROUP BY f.id")
    Page<FormationProjection> findProjectionsByFilters(
        @Param("secteur") String secteur,
        @Param("region") String region,
        @Param("modalite") ModaliteFormation modalite,
        Pageable pageable
    );
}