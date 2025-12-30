package com.enterprise.app.domain.repository.csvexport;

import com.enterprise.app.domain.model.csvexport.CsvColumnDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les définitions de colonnes CSV.
 */
@Repository
public interface CsvColumnDefinitionRepository extends JpaRepository<CsvColumnDefinition, Long> {

    /**
     * Trouve toutes les colonnes actives pour un CSV donné, ordonnées par ordre_colonne.
     */
    List<CsvColumnDefinition> findByNomCsvAndActifTrueOrderByOrdreColonneAsc(String nomCsv);

    /**
     * Trouve une colonne par nom CSV et code variable.
     */
    Optional<CsvColumnDefinition> findByNomCsvAndCodeVariable(String nomCsv, String codeVariable);

    /**
     * Trouve toutes les colonnes pour un CSV donné (actives et inactives).
     */
    List<CsvColumnDefinition> findByNomCsvOrderByOrdreColonneAsc(String nomCsv);

    /**
     * Vérifie si une colonne existe pour un CSV et code variable donnés.
     */
    boolean existsByNomCsvAndCodeVariable(String nomCsv, String codeVariable);

    /**
     * Compte le nombre de colonnes actives pour un CSV.
     */
    long countByNomCsvAndActifTrue(String nomCsv);

    /**
     * Trouve les codes variables distincts pour un CSV donné.
     */
    @Query("SELECT DISTINCT cd.codeVariable FROM CsvColumnDefinition cd WHERE cd.nomCsv = :nomCsv AND cd.actif = true")
    List<String> findDistinctCodeVariablesByNomCsv(@Param("nomCsv") String nomCsv);

    /**
     * Trouve les colonnes sans mapping technique associé.
     */
    @Query("""
        SELECT cd FROM CsvColumnDefinition cd
        WHERE cd.nomCsv = :nomCsv
        AND cd.actif = true
        AND NOT EXISTS (
            SELECT 1 FROM ColumnSqlMapping csm
            WHERE csm.codeVariable = cd.codeVariable
        )
        ORDER BY cd.ordreColonne
        """)
    List<CsvColumnDefinition> findColumnsWithoutMapping(@Param("nomCsv") String nomCsv);

    /**
     * Trouve la prochaine position disponible pour un CSV donné.
     */
    @Query("SELECT COALESCE(MAX(cd.ordreColonne), 0) + 1 FROM CsvColumnDefinition cd WHERE cd.nomCsv = :nomCsv")
    Integer findNextOrdreColonne(@Param("nomCsv") String nomCsv);

    /**
     * Désactive toutes les colonnes pour un CSV donné.
     */
    @Query("UPDATE CsvColumnDefinition cd SET cd.actif = false WHERE cd.nomCsv = :nomCsv")
    int deactivateAllByNomCsv(@Param("nomCsv") String nomCsv);
}