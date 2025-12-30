package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.application.dto.csvexport.ExcelRowDTO;
import com.enterprise.app.domain.model.csvexport.ExcelDefinitionStaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour ExcelRowProcessor.
 */
@DisplayName("ExcelRowProcessor - Traitement des lignes Excel")
class ExcelRowProcessorTest {

    private final ExcelRowProcessor processor = new ExcelRowProcessor();

    @Test
    @DisplayName("Devrait traiter une ligne sans valeurs possibles")
    void shouldProcessRowWithoutValeursPossibles() throws Exception {
        // Given
        ExcelRowDTO input = ExcelRowDTO.create(
                "CSV_1",
                2,
                "Q_ID",
                "Identifiant",
                "NUM",
                "NUMERIQUE",
                "NUMERIQUE_DIRECT",
                Map.of());

        // When
        List<ExcelDefinitionStaging> result = processor.process(input);

        // Then
        assertThat(result).hasSize(1);

        ExcelDefinitionStaging staging = result.get(0);
        assertThat(staging.getNomCsv()).isEqualTo("CSV_1");
        assertThat(staging.getExcelRowNumber()).isEqualTo(2);
        assertThat(staging.getCodeVariable()).isEqualTo("Q_ID");
        assertThat(staging.getLibelle()).isEqualTo("Identifiant");
        assertThat(staging.getFormat()).isEqualTo("NUM");
        assertThat(staging.getTypeQuestion()).isEqualTo("NUMERIQUE");
        assertThat(staging.getRegleGestion()).isEqualTo("NUMERIQUE_DIRECT");
        assertThat(staging.getIndexValeur()).isNull();
        assertThat(staging.getValeurPossible()).isNull();
    }

    @Test
    @DisplayName("Devrait traiter une ligne avec valeurs possibles")
    void shouldProcessRowWithValeursPossibles() throws Exception {
        // Given
        ExcelRowDTO input = ExcelRowDTO.create(
                "CSV_1",
                3,
                "Q_SEXE",
                "Sexe",
                "NUM",
                "LISTE",
                "POSITION_VALEUR_POSSIBLE",
                Map.of(
                    1, "Homme",
                    2, "Femme",
                    3, "Autre"
                ));

        // When
        List<ExcelDefinitionStaging> result = processor.process(input);

        // Then
        assertThat(result).hasSize(3);

        // Vérifier le premier enregistrement
        ExcelDefinitionStaging staging1 = result.get(0);
        assertThat(staging1.getNomCsv()).isEqualTo("CSV_1");
        assertThat(staging1.getExcelRowNumber()).isEqualTo(3);
        assertThat(staging1.getCodeVariable()).isEqualTo("Q_SEXE");
        assertThat(staging1.getIndexValeur()).isIn(1, 2, 3); // L'ordre peut varier selon l'itération du Map
        assertThat(staging1.getValeurPossible()).isIn("Homme", "Femme", "Autre");

        // Vérifier que tous les enregistrements ont les mêmes informations de base
        result.forEach(staging -> {
            assertThat(staging.getNomCsv()).isEqualTo("CSV_1");
            assertThat(staging.getExcelRowNumber()).isEqualTo(3);
            assertThat(staging.getCodeVariable()).isEqualTo("Q_SEXE");
            assertThat(staging.getLibelle()).isEqualTo("Sexe");
        });
    }

    @Test
    @DisplayName("Devrait normaliser la règle de gestion vide vers la valeur par défaut")
    void shouldNormalizeEmptyRegleGestionToDefault() throws Exception {
        // Given
        ExcelRowDTO input = ExcelRowDTO.create(
                "CSV_1",
                2,
                "Q_TEST",
                null,
                null,
                null,
                "", // Vide
                Map.of());

        // When
        List<ExcelDefinitionStaging> result = processor.process(input);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRegleGestion()).isEqualTo("POSITION_VALEUR_POSSIBLE");
    }

    @Test
    @DisplayName("Devrait normaliser une règle de gestion inconnue vers la valeur par défaut")
    void shouldNormalizeUnknownRegleGestionToDefault() throws Exception {
        // Given
        ExcelRowDTO input = ExcelRowDTO.create(
                "CSV_1",
                2,
                "Q_TEST",
                null,
                null,
                null,
                "REGLE_INCONNUE",
                Map.of());

        // When
        List<ExcelDefinitionStaging> result = processor.process(input);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRegleGestion()).isEqualTo("POSITION_VALEUR_POSSIBLE");
    }

    @Test
    @DisplayName("Devrait préserver les règles de gestion valides")
    void shouldPreserveValidRegleGestion() throws Exception {
        // Given
        String[] validRules = {"TEXTE_DIRECT", "NUMERIQUE_DIRECT", "DATE_ISO", "POSITION_VALEUR_POSSIBLE"};

        for (String rule : validRules) {
            ExcelRowDTO input = ExcelRowDTO.create(
                    "CSV_1",
                    2,
                    "Q_TEST",
                    null,
                    null,
                    null,
                    rule.toLowerCase(), // Tester la normalisation en majuscules
                    Map.of());

            // When
            List<ExcelDefinitionStaging> result = processor.process(input);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRegleGestion()).isEqualTo(rule);
        }
    }

    @Test
    @DisplayName("Devrait préserver l'ordre Excel dans les enregistrements de staging")
    void shouldPreserveExcelRowOrderInStagingRecords() throws Exception {
        // Given
        ExcelRowDTO input = ExcelRowDTO.create(
                "CSV_1",
                42, // Numéro spécifique
                "Q_TEST",
                null,
                null,
                null,
                null,
                Map.of(1, "Valeur1", 2, "Valeur2"));

        // When
        List<ExcelDefinitionStaging> result = processor.process(input);

        // Then
        assertThat(result).hasSize(2);
        result.forEach(staging -> {
            assertThat(staging.getExcelRowNumber()).isEqualTo(42);
        });
    }
}