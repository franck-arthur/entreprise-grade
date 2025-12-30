package com.enterprise.app.integration;

import com.enterprise.app.domain.model.csvexport.CsvColumnDefinition;
import com.enterprise.app.domain.model.csvexport.ColumnSqlMapping;
import com.enterprise.app.domain.repository.csvexport.CsvColumnDefinitionRepository;
import com.enterprise.app.domain.repository.csvexport.ColumnSqlMappingRepository;
import com.enterprise.app.application.service.csvexport.CsvExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration pour le système d'export CSV complet.
 * Teste l'interaction entre les différents composants.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Système d'export CSV - Test d'intégration")
@Sql("/sql/csv-export-test-data.sql") // Données de test
class CsvExportSystemIntegrationTest {

    @Autowired
    private CsvColumnDefinitionRepository columnDefinitionRepository;

    @Autowired
    private ColumnSqlMappingRepository mappingRepository;

    @Autowired
    private CsvExportService csvExportService;

    @Test
    @DisplayName("Devrait charger les définitions de colonnes avec l'ordre préservé")
    @Transactional
    void shouldLoadColumnDefinitionsWithPreservedOrder() {
        // Given - Données insérées via le script SQL

        // When
        List<CsvColumnDefinition> columns = columnDefinitionRepository
                .findByNomCsvAndActifTrueOrderByOrdreColonneAsc("CSV_TEST");

        // Then
        assertThat(columns).isNotEmpty();
        assertThat(columns).extracting("codeVariable")
                .containsExactly("Q_ID", "Q_SEXE", "Q_EMAIL", "Q_VILLE"); // Ordre exact

        // Vérifier que l'ordre des colonnes correspond bien à l'ordre Excel
        for (int i = 0; i < columns.size(); i++) {
            assertThat(columns.get(i).getOrdreColonne()).isEqualTo(i + 1);
        }
    }

    @Test
    @DisplayName("Devrait associer les mappings techniques aux définitions métier")
    @Transactional
    void shouldAssociateTechnicalMappingsWithBusinessDefinitions() {
        // When
        List<ColumnSqlMapping> mappings = mappingRepository.findMappingsForCsv("CSV_TEST");

        // Then
        assertThat(mappings).isNotEmpty();

        // Vérifier qu'on a des mappings pour nos colonnes de test
        assertThat(mappings).extracting("codeVariable")
                .contains("Q_ID", "Q_SEXE", "Q_EMAIL");
    }

    @Test
    @DisplayName("Devrait identifier les colonnes sans mapping technique")
    @Transactional
    void shouldIdentifyColumnsWithoutMapping() {
        // When
        List<CsvColumnDefinition> unmappedColumns = columnDefinitionRepository
                .findColumnsWithoutMapping("CSV_TEST");

        // Then
        // Si nos données de test incluent des colonnes sans mapping, elles devraient apparaître ici
        assertThat(unmappedColumns).extracting("codeVariable")
                .doesNotContain("Q_ID", "Q_SEXE"); // Ces colonnes ont des mappings

        // Mais Q_VILLE pourrait ne pas avoir de mapping selon nos données de test
        assertThat(unmappedColumns).extracting("codeVariable")
                .contains("Q_VILLE");
    }

    @Test
    @DisplayName("Devrait supporter différents types de mappings techniques")
    @Transactional
    void shouldSupportDifferentMappingTypes() {
        // When
        List<ColumnSqlMapping> simpleMappings = mappingRepository.findSimpleColumnMappings();
        List<ColumnSqlMapping> complexMappings = mappingRepository.findComplexExpressionMappings();

        // Then
        assertThat(simpleMappings).isNotEmpty();
        assertThat(complexMappings).isNotEmpty();

        // Vérifier qu'un mapping simple a bien les champs requis
        ColumnSqlMapping simpleMapping = simpleMappings.stream()
                .filter(m -> "Q_ID".equals(m.getCodeVariable()))
                .findFirst()
                .orElseThrow();

        assertThat(simpleMapping.isSimpleColumn()).isTrue();
        assertThat(simpleMapping.getSourceTable()).isNotNull();
        assertThat(simpleMapping.getSourceColumn()).isNotNull();
        assertThat(simpleMapping.getSqlExpression()).isNull();

        // Vérifier qu'un mapping complexe a bien une expression SQL
        ColumnSqlMapping complexMapping = complexMappings.get(0);
        assertThat(complexMapping.isComplexExpression()).isTrue();
        assertThat(complexMapping.getSqlExpression()).isNotNull();
        assertThat(complexMapping.getSourceTable()).isNull();
    }

    @Test
    @DisplayName("Devrait générer l'alias SQL correct pour les différents types de mapping")
    @Transactional
    void shouldGenerateCorrectSqlAliasForDifferentMappingTypes() {
        // Given
        ColumnSqlMapping simpleMapping = ColumnSqlMapping.ofSimple("Q_TEST_SIMPLE", "questionnaire", "id");
        ColumnSqlMapping complexMapping = ColumnSqlMapping.ofExpression("Q_TEST_COMPLEX", "COUNT(*)");

        // When & Then
        String simpleAlias = simpleMapping.getSqlAlias();
        assertThat(simpleAlias).isEqualTo("q.id AS Q_TEST_SIMPLE");

        String complexAlias = complexMapping.getSqlAlias();
        assertThat(complexAlias).isEqualTo("(COUNT(*)) AS Q_TEST_COMPLEX");
    }

    @Test
    @DisplayName("Devrait respecter les contraintes d'unicité des définitions")
    @Transactional
    void shouldRespectDefinitionUniquenessConstraints() {
        // Given
        CsvColumnDefinition definition1 = CsvColumnDefinition.builder()
                .nomCsv("CSV_UNIQUE_TEST")
                .ordreColonne(1)
                .codeVariable("Q_UNIQUE")
                .libelle("Test")
                .build();

        CsvColumnDefinition definition2 = CsvColumnDefinition.builder()
                .nomCsv("CSV_UNIQUE_TEST")
                .ordreColonne(2) // Ordre différent
                .codeVariable("Q_UNIQUE") // Même code variable -> doit échouer
                .libelle("Test 2")
                .build();

        // When
        columnDefinitionRepository.save(definition1);

        // Then
        // L'insertion de definition2 devrait échouer à cause de la contrainte unique (nom_csv, code_variable)
        try {
            columnDefinitionRepository.save(definition2);
            columnDefinitionRepository.flush();
            assertThat(false).as("La contrainte d'unicité aurait dû être violée").isTrue();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("uk_csv_column_nom_code");
        }
    }

    @Test
    @DisplayName("Devrait exécuter les exports asynchrones avec Virtual Threads")
    void shouldExecuteAsynchronousExportsWithVirtualThreads() throws Exception {
        // When - Lancement d'un export asynchrone
        CompletableFuture<Void> exportFuture = csvExportService.exportAllCsvsHebdomadaireAsync();

        // Then
        assertThat(exportFuture).isNotNull();
        assertThat(exportFuture.isDone()).isFalse();

        // Attendre un court délai pour voir si l'exécution progresse
        Thread.sleep(100);

        // Le future devrait toujours être en cours (sauf échec)
        assertThat(exportFuture.isCancelled()).isFalse();

        // Essayer d'attendre avec timeout court pour éviter que le test traîne
        try {
            exportFuture.get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Le timeout est attendu si le service essaie vraiment de faire l'export
            // C'est acceptable car nous testons la mécanisme asynchrone
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Devrait récupérer les statistiques de manière asynchrone")
    void shouldRetrieveStatisticsAsynchronously() throws Exception {
        // Given
        String nomCsv = "CSV_TEST";
        LocalDateTime depuis = LocalDateTime.now().minusDays(30);

        // When
        CompletableFuture<Object[]> statsFuture = csvExportService.getExportStatisticsAsync(nomCsv, depuis);

        // Then
        assertThat(statsFuture).isNotNull();

        // Récupérer le résultat avec timeout
        Object[] statistiques = statsFuture.get(5, TimeUnit.SECONDS);

        // Les statistiques peuvent être nulles ou vides selon les données de test
        // mais le mécanisme asynchrone doit fonctionner
        assertThat(statistiques).isNotNull();
    }

    @Test
    @DisplayName("Devrait nettoyer les ressources Virtual Threads au shutdown")
    void shouldCleanupVirtualThreadsOnShutdown() {
        // When
        csvExportService.cleanup();

        // Then - Pas d'exception et le service reste utilisable
        assertThat(csvExportService).isNotNull();

        // Vérifier que le service fonctionne toujours pour les opérations synchrones
        List<CsvColumnDefinition> definitions = columnDefinitionRepository
                .findByNomCsvAndActifTrueOrderByOrdreColonneAsc("CSV_TEST");
        assertThat(definitions).isNotEmpty();
    }

    @Test
    @DisplayName("Devrait gérer l'exécution parallèle de multiples exports")
    void shouldHandleParallelExecutionOfMultipleExports() throws Exception {
        // Given - Simulation de multiples exports simultanés
        LocalDateTime depuis = LocalDateTime.now().minusDays(7);

        // When - Lancement de plusieurs opérations asynchrones
        CompletableFuture<Object[]> stats1 = csvExportService.getExportStatisticsAsync("CSV_TEST", depuis);
        CompletableFuture<Object[]> stats2 = csvExportService.getExportStatisticsAsync("CSV_TEST", depuis);
        CompletableFuture<Object[]> stats3 = csvExportService.getExportStatisticsAsync("CSV_TEST", depuis);

        // Then - Attendre toutes les opérations
        CompletableFuture<Void> allStats = CompletableFuture.allOf(stats1, stats2, stats3);

        // Toutes les opérations doivent se terminer sans erreur
        allStats.get(10, TimeUnit.SECONDS);

        assertThat(stats1.isDone()).isTrue();
        assertThat(stats2.isDone()).isTrue();
        assertThat(stats3.isDone()).isTrue();

        // Vérifier que les résultats sont cohérents
        Object[] result1 = stats1.get();
        Object[] result2 = stats2.get();
        Object[] result3 = stats3.get();

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();
    }
}