package com.enterprise.app.domain.repository.csvexport;

import com.enterprise.app.domain.model.csvexport.ColumnSqlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les mappings techniques SQL.
 */
@Repository
public interface ColumnSqlMappingRepository extends JpaRepository<ColumnSqlMapping, Long> {

    /**
     * Trouve un mapping par code variable.
     */
    Optional<ColumnSqlMapping> findByCodeVariable(String codeVariable);

    /**
     * Vérifie si un mapping existe pour un code variable donné.
     */
    boolean existsByCodeVariable(String codeVariable);

    /**
     * Trouve tous les mappings pour les codes variables d'un CSV donné.
     */
    @Query("""
        SELECT csm FROM ColumnSqlMapping csm
        WHERE csm.codeVariable IN (
            SELECT cd.codeVariable FROM CsvColumnDefinition cd
            WHERE cd.nomCsv = :nomCsv AND cd.actif = true
        )
        ORDER BY csm.codeVariable
        """)
    List<ColumnSqlMapping> findMappingsForCsv(@Param("nomCsv") String nomCsv);

    /**
     * Trouve tous les mappings de type colonne simple.
     */
    @Query("SELECT csm FROM ColumnSqlMapping csm WHERE csm.sourceTable IS NOT NULL AND csm.sourceColumn IS NOT NULL")
    List<ColumnSqlMapping> findSimpleColumnMappings();

    /**
     * Trouve tous les mappings de type expression complexe.
     */
    @Query("SELECT csm FROM ColumnSqlMapping csm WHERE csm.sqlExpression IS NOT NULL")
    List<ColumnSqlMapping> findComplexExpressionMappings();

    /**
     * Trouve les codes variables qui n'ont pas de mapping technique.
     */
    @Query("""
        SELECT cd.codeVariable FROM CsvColumnDefinition cd
        WHERE cd.nomCsv = :nomCsv
        AND cd.actif = true
        AND NOT EXISTS (
            SELECT 1 FROM ColumnSqlMapping csm
            WHERE csm.codeVariable = cd.codeVariable
        )
        ORDER BY cd.ordreColonne
        """)
    List<String> findUnmappedCodeVariables(@Param("nomCsv") String nomCsv);

    /**
     * Trouve tous les mappings qui utilisent une table donnée.
     */
    @Query("""
        SELECT csm FROM ColumnSqlMapping csm
        WHERE csm.sourceTable = :tableName
        OR :tableName = ANY(csm.joinTables)
        """)
    List<ColumnSqlMapping> findMappingsUsingTable(@Param("tableName") String tableName);

    /**
     * Trouve les mappings avec sous-requêtes.
     */
    List<ColumnSqlMapping> findByRequiresSubqueryTrue();

    /**
     * Trouve les mappings avec agrégations.
     */
    List<ColumnSqlMapping> findByIsAggregationTrue();
}