package com.enterprise.app.domain.repository.csvexport;

import com.enterprise.app.domain.model.csvexport.ExportExecution;
import com.enterprise.app.domain.model.csvexport.StatutExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour le tracking des exécutions d'export CSV.
 */
@Repository
public interface ExportExecutionRepository extends JpaRepository<ExportExecution, Long> {

    /**
     * Trouve l'exécution par job execution ID.
     */
    Optional<ExportExecution> findByJobExecutionId(Long jobExecutionId);

    /**
     * Trouve les dernières exécutions pour un CSV donné.
     */
    List<ExportExecution> findTop10ByNomCsvOrderByDateDebutDesc(String nomCsv);

    /**
     * Trouve les exécutions par statut.
     */
    List<ExportExecution> findByStatutOrderByDateDebutDesc(StatutExecution statut);

    /**
     * Trouve les exécutions en cours.
     */
    List<ExportExecution> findByStatutAndDateFinIsNull(StatutExecution statut);

    /**
     * Trouve les exécutions dans une période donnée.
     */
    List<ExportExecution> findByDateDebutBetweenOrderByDateDebutDesc(LocalDateTime debut, LocalDateTime fin);

    /**
     * Trouve la dernière exécution réussie pour un CSV donné.
     */
    Optional<ExportExecution> findFirstByNomCsvAndStatutOrderByDateDebutDesc(String nomCsv, StatutExecution statut);

    /**
     * Statistiques des exécutions pour un CSV donné.
     */
    @Query("""
        SELECT COUNT(*) as total,
               SUM(CASE WHEN ee.statut = 'TERMINE' THEN 1 ELSE 0 END) as succes,
               SUM(CASE WHEN ee.statut = 'ECHEC' THEN 1 ELSE 0 END) as echecs,
               AVG(ee.dureeSecondes) as duree_moyenne
        FROM ExportExecution ee
        WHERE ee.nomCsv = :nomCsv
        AND ee.dateDebut >= :depuis
        """)
    Object[] getStatistiquesExport(@Param("nomCsv") String nomCsv, @Param("depuis") LocalDateTime depuis);

    /**
     * Trouve les exécutions qui ont duré plus que la durée spécifiée (en secondes).
     */
    @Query("SELECT ee FROM ExportExecution ee WHERE ee.dureeSecondes > :seuilSecondes")
    List<ExportExecution> findLongRunningExecutions(@Param("seuilSecondes") Double seuilSecondes);

    /**
     * Supprime les exécutions plus anciennes qu'une date donnée.
     */
    void deleteByDateDebutBefore(LocalDateTime cutoffDate);

    /**
     * Compte les exécutions par statut pour un CSV donné.
     */
    @Query("SELECT ee.statut, COUNT(*) FROM ExportExecution ee WHERE ee.nomCsv = :nomCsv GROUP BY ee.statut")
    List<Object[]> countByStatutForCsv(@Param("nomCsv") String nomCsv);
}