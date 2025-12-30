package com.enterprise.app.domain.repository.csvexport;

import com.enterprise.app.domain.model.csvexport.ExcelDefinitionStaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour la table de staging des définitions Excel.
 */
@Repository
public interface ExcelDefinitionStagingRepository extends JpaRepository<ExcelDefinitionStaging, Long> {

    /**
     * Trouve tous les enregistrements pour un CSV donné, ordonnés par ligne Excel.
     */
    List<ExcelDefinitionStaging> findByNomCsvOrderByExcelRowNumberAsc(String nomCsv);

    /**
     * Trouve les codes variables distincts pour un CSV donné.
     */
    @Query("SELECT DISTINCT eds.codeVariable FROM ExcelDefinitionStaging eds WHERE eds.nomCsv = :nomCsv")
    List<String> findDistinctCodeVariablesByNomCsv(@Param("nomCsv") String nomCsv);

    /**
     * Trouve tous les enregistrements avec valeurs possibles pour un CSV donné.
     */
    List<ExcelDefinitionStaging> findByNomCsvAndValeurPossibleIsNotNull(String nomCsv);

    /**
     * Supprime tous les enregistrements pour un CSV donné.
     */
    @Modifying
    @Query("DELETE FROM ExcelDefinitionStaging eds WHERE eds.nomCsv = :nomCsv")
    int deleteByNomCsv(@Param("nomCsv") String nomCsv);

    /**
     * Supprime tous les enregistrements plus anciens qu'une date donnée.
     */
    @Modifying
    @Query("DELETE FROM ExcelDefinitionStaging eds WHERE eds.dateImport < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Compte les enregistrements pour un CSV donné.
     */
    long countByNomCsv(String nomCsv);

    /**
     * Trouve les noms CSV distincts présents dans le staging.
     */
    @Query("SELECT DISTINCT eds.nomCsv FROM ExcelDefinitionStaging eds")
    List<String> findDistinctNomCsv();

    /**
     * Trouve l'ordre max des lignes Excel pour un CSV donné.
     */
    @Query("SELECT MAX(eds.excelRowNumber) FROM ExcelDefinitionStaging eds WHERE eds.nomCsv = :nomCsv")
    Integer findMaxExcelRowNumberByNomCsv(@Param("nomCsv") String nomCsv);
}