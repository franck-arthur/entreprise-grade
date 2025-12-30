package com.enterprise.app.infrastructure.batch.csvexport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;

/**
 * Tasklet pour normaliser les définitions Excel en appelant la procédure PostgreSQL.
 */
@Slf4j
@Component
@StepScope
public class NormalizeDefinitionsTasklet implements Tasklet {

    private final DataSource dataSource;

    public NormalizeDefinitionsTasklet(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Normalisation des définitions Excel...");

        try (Connection connection = dataSource.getConnection()) {
            // Appeler la procédure de normalisation PostgreSQL
            try (CallableStatement cs = connection.prepareCall("CALL export_csv.normalize_definitions()")) {
                cs.execute();
                log.info("Procédure de normalisation exécutée avec succès");
            }

            // Optionnel: récupérer des statistiques
            try (var stmt = connection.createStatement();
                 var rs = stmt.executeQuery("""
                     SELECT
                         COUNT(*) as total_definitions,
                         COUNT(DISTINCT nom_csv) as csv_count,
                         COUNT(CASE WHEN actif = true THEN 1 END) as active_definitions
                     FROM export_csv.csv_column_definition
                     """)) {

                if (rs.next()) {
                    long totalDef = rs.getLong("total_definitions");
                    long csvCount = rs.getLong("csv_count");
                    long activeDef = rs.getLong("active_definitions");

                    log.info("Statistiques après normalisation: {} définitions totales, {} CSV, {} définitions actives",
                            totalDef, csvCount, activeDef);

                    // Stocker les métriques dans le contexte
                    contribution.getStepExecution().getExecutionContext()
                            .putLong("normalize.totalDefinitions", totalDef);
                    contribution.getStepExecution().getExecutionContext()
                            .putLong("normalize.csvCount", csvCount);
                    contribution.getStepExecution().getExecutionContext()
                            .putLong("normalize.activeDefinitions", activeDef);
                }
            }

            // Vérifier les colonnes sans mapping technique
            try (var stmt = connection.createStatement();
                 var rs = stmt.executeQuery("""
                     SELECT COUNT(*) as unmapped_count
                     FROM export_csv.csv_column_definition cd
                     LEFT JOIN export_csv.column_sql_mapping csm ON csm.code_variable = cd.code_variable
                     WHERE cd.actif = true AND csm.id IS NULL
                     """)) {

                if (rs.next()) {
                    long unmappedCount = rs.getLong("unmapped_count");
                    log.info("Colonnes sans mapping technique: {}", unmappedCount);

                    contribution.getStepExecution().getExecutionContext()
                            .putLong("normalize.unmappedColumns", unmappedCount);

                    if (unmappedCount > 0) {
                        log.warn("ATTENTION: {} colonnes n'ont pas de mapping technique défini", unmappedCount);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Erreur lors de la normalisation des définitions", e);
            throw e;
        }

        log.info("Normalisation des définitions terminée");
        return RepeatStatus.FINISHED;
    }
}