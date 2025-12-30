package com.enterprise.app.application.dto.csvexport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests pour ExcelRowDTO")
class ExcelRowDTOTest {

    @Test
    @DisplayName("Doit créer une instance valide avec tous les paramètres")
    void constructor_ShouldCreateValidInstanceWithAllParameters() {
        // Given
        String nomCsv = "formation_csv";
        Integer excelRowNumber = 3;
        String codeVariable = "NOM_FORMATION";
        String libelle = "Nom de la formation";
        String format = "TEXT";
        String typeQuestion = "UNIQUE";
        String regleGestion = "OBLIGATOIRE";
        Map<Integer, String> valeursPossibles = Map.of(1, "Valeur 1", 2, "Valeur 2");

        // When
        ExcelRowDTO dto = new ExcelRowDTO(
                nomCsv, excelRowNumber, codeVariable, libelle,
                format, typeQuestion, regleGestion, valeursPossibles
        );

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.nomCsv()).isEqualTo(nomCsv);
        assertThat(dto.excelRowNumber()).isEqualTo(excelRowNumber);
        assertThat(dto.codeVariable()).isEqualTo(codeVariable);
        assertThat(dto.libelle()).isEqualTo(libelle);
        assertThat(dto.format()).isEqualTo(format);
        assertThat(dto.typeQuestion()).isEqualTo(typeQuestion);
        assertThat(dto.regleGestion()).isEqualTo(regleGestion);
        assertThat(dto.valeursPossibles()).isEqualTo(valeursPossibles);
    }

    @Test
    @DisplayName("Doit utiliser le constructeur compact avec validation et initialisation par défaut")
    void compactConstructor_ShouldValidateAndInitializeDefaults() {
        // Given
        Map<Integer, String> valeursPossibles = Map.of(1, "Test");

        // When
        ExcelRowDTO dto = new ExcelRowDTO(
                "test_csv", 2, "CODE_TEST", "Libellé",
                "TEXT", "UNIQUE", "OBLIGATOIRE", valeursPossibles
        );

        // Then
        assertThat(dto.valeursPossibles()).isNotNull();
        assertThat(dto.valeursPossibles()).isUnmodifiable(); // Map.copyOf() rend immutable
        assertThat(dto.valeursPossibles()).containsEntry(1, "Test");
    }

    @Test
    @DisplayName("Doit initialiser une Map vide si valeursPossibles est null")
    void compactConstructor_ShouldInitializeEmptyMapWhenNull() {
        // Given & When
        ExcelRowDTO dto = new ExcelRowDTO(
                "test_csv", 2, "CODE_TEST", "Libellé",
                "TEXT", "UNIQUE", "OBLIGATOIRE", null
        );

        // Then
        assertThat(dto.valeursPossibles()).isNotNull();
        assertThat(dto.valeursPossibles()).isEmpty();
    }

    @Test
    @DisplayName("Doit créer un ExcelRowDTO avec valeurs par défaut via la méthode factory of")
    void of_ShouldCreateWithDefaultValues() {
        // Given
        String nomCsv = "secteur_csv";
        Integer excelRowNumber = 5;
        String codeVariable = "TYPE_SECTEUR";

        // When
        ExcelRowDTO dto = ExcelRowDTO.of(nomCsv, excelRowNumber, codeVariable);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.nomCsv()).isEqualTo(nomCsv);
        assertThat(dto.excelRowNumber()).isEqualTo(excelRowNumber);
        assertThat(dto.codeVariable()).isEqualTo(codeVariable);
        assertThat(dto.libelle()).isNull();
        assertThat(dto.format()).isNull();
        assertThat(dto.typeQuestion()).isNull();
        assertThat(dto.regleGestion()).isNull();
        assertThat(dto.valeursPossibles()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Doit créer un ExcelRowDTO complet via la méthode factory create")
    void create_ShouldCreateCompleteInstance() {
        // Given
        String nomCsv = "personne_csv";
        Integer excelRowNumber = 4;
        String codeVariable = "AGE_PERSONNE";
        String libelle = "Âge de la personne";
        String format = "NUMERIC";
        String typeQuestion = "RANGE";
        String regleGestion = "OPTIONNEL";
        Map<Integer, String> valeursPossibles = Map.of(1, "18-25", 2, "26-35", 3, "36+");

        // When
        ExcelRowDTO dto = ExcelRowDTO.create(
                nomCsv, excelRowNumber, codeVariable, libelle,
                format, typeQuestion, regleGestion, valeursPossibles
        );

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.nomCsv()).isEqualTo(nomCsv);
        assertThat(dto.excelRowNumber()).isEqualTo(excelRowNumber);
        assertThat(dto.codeVariable()).isEqualTo(codeVariable);
        assertThat(dto.libelle()).isEqualTo(libelle);
        assertThat(dto.format()).isEqualTo(format);
        assertThat(dto.typeQuestion()).isEqualTo(typeQuestion);
        assertThat(dto.regleGestion()).isEqualTo(regleGestion);
        assertThat(dto.valeursPossibles()).isEqualTo(valeursPossibles);
    }

    @Test
    @DisplayName("Doit créer une nouvelle instance avec valeur possible ajoutée via withValeurPossible")
    void withValeurPossible_ShouldCreateNewInstanceWithAddedValue() {
        // Given
        ExcelRowDTO originalDto = ExcelRowDTO.of("test", 2, "VAR");
        Integer position = 1;
        String valeur = "Nouvelle valeur";

        // When
        ExcelRowDTO newDto = originalDto.withValeurPossible(position, valeur);

        // Then
        assertThat(newDto).isNotSameAs(originalDto); // Immutabilité
        assertThat(originalDto.valeursPossibles()).isEmpty(); // L'original n'a pas changé
        assertThat(newDto.valeursPossibles()).hasSize(1);
        assertThat(newDto.valeursPossibles()).containsEntry(position, valeur);

        // Autres champs inchangés
        assertThat(newDto.nomCsv()).isEqualTo(originalDto.nomCsv());
        assertThat(newDto.excelRowNumber()).isEqualTo(originalDto.excelRowNumber());
        assertThat(newDto.codeVariable()).isEqualTo(originalDto.codeVariable());
    }

    @Test
    @DisplayName("Doit ajouter plusieurs valeurs possibles en préservant l'immutabilité")
    void withValeurPossible_ShouldAddMultipleValuesImmutably() {
        // Given
        ExcelRowDTO originalDto = ExcelRowDTO.of("test", 2, "VAR");

        // When
        ExcelRowDTO dto1 = originalDto.withValeurPossible(1, "Valeur 1");
        ExcelRowDTO dto2 = dto1.withValeurPossible(2, "Valeur 2");
        ExcelRowDTO dto3 = dto2.withValeurPossible(3, "Valeur 3");

        // Then
        assertThat(originalDto.valeursPossibles()).isEmpty();
        assertThat(dto1.valeursPossibles()).hasSize(1);
        assertThat(dto2.valeursPossibles()).hasSize(2);
        assertThat(dto3.valeursPossibles()).hasSize(3);

        assertThat(dto3.valeursPossibles()).containsAllEntriesOf(
                Map.of(1, "Valeur 1", 2, "Valeur 2", 3, "Valeur 3")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("Doit ignorer les valeurs possibles nulles ou vides")
    void withValeurPossible_ShouldIgnoreNullOrEmptyValues(String valeur) {
        // Given
        ExcelRowDTO originalDto = ExcelRowDTO.of("test", 2, "VAR");

        // When
        ExcelRowDTO newDto = originalDto.withValeurPossible(1, valeur);

        // Then
        assertThat(newDto).isSameAs(originalDto); // Retourne la même instance
        assertThat(newDto.valeursPossibles()).isEmpty();
    }

    @Test
    @DisplayName("Doit trimmer les valeurs possibles avant de les ajouter")
    void withValeurPossible_ShouldTrimValues() {
        // Given
        ExcelRowDTO originalDto = ExcelRowDTO.of("test", 2, "VAR");
        String valeurAvecEspaces = "  valeur avec espaces  ";

        // When
        ExcelRowDTO newDto = originalDto.withValeurPossible(1, valeurAvecEspaces);

        // Then
        assertThat(newDto.valeursPossibles()).containsEntry(1, "valeur avec espaces");
    }

    @Test
    @DisplayName("Doit détecter correctement la présence de valeurs possibles")
    void hasValeursPossibles_ShouldDetectCorrectly() {
        // Given
        ExcelRowDTO dtoSansValeurs = ExcelRowDTO.of("test", 2, "VAR");
        ExcelRowDTO dtoAvecValeurs = dtoSansValeurs.withValeurPossible(1, "Valeur");

        // When & Then
        assertThat(dtoSansValeurs.hasValeursPossibles()).isFalse();
        assertThat(dtoAvecValeurs.hasValeursPossibles()).isTrue();
    }

    @Test
    @DisplayName("Doit retourner le bon nombre de valeurs possibles")
    void getValeursPossiblesCount_ShouldReturnCorrectCount() {
        // Given
        ExcelRowDTO dto = ExcelRowDTO.of("test", 2, "VAR")
                .withValeurPossible(1, "Valeur 1")
                .withValeurPossible(2, "Valeur 2")
                .withValeurPossible(3, "Valeur 3");

        // When
        int count = dto.getValeursPossiblesCount();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Doit valider correctement avec pattern matching Java 21")
    void isValid_ShouldUsePatternMatchingCorrectly() {
        // Given
        ExcelRowDTO dtoValide = ExcelRowDTO.of("test", 2, "CODE_VALIDE");
        ExcelRowDTO dtoCodeNull = new ExcelRowDTO("test", 2, null, null, null, null, null, Map.of());
        ExcelRowDTO dtoCodeVide = new ExcelRowDTO("test", 2, "", null, null, null, null, Map.of());
        ExcelRowDTO dtoCodeEspaces = new ExcelRowDTO("test", 2, "   ", null, null, null, null, Map.of());
        ExcelRowDTO dtoRowNull = new ExcelRowDTO("test", null, "CODE", null, null, null, null, Map.of());
        ExcelRowDTO dtoRowHeader = new ExcelRowDTO("test", 1, "CODE", null, null, null, null, Map.of()); // Row 1 = header

        // When & Then
        assertThat(dtoValide.isValid()).isTrue();
        assertThat(dtoCodeNull.isValid()).isFalse();
        assertThat(dtoCodeVide.isValid()).isFalse();
        assertThat(dtoCodeEspaces.isValid()).isFalse();
        assertThat(dtoRowNull.isValid()).isFalse();
        assertThat(dtoRowHeader.isValid()).isFalse(); // Row 1 est le header
    }

    @Test
    @DisplayName("Doit générer une représentation condensée pour les logs avec formatage Java 21")
    void toLogString_ShouldUseJava21Formatting() {
        // Given
        ExcelRowDTO dto = ExcelRowDTO.of("formation_csv", 5, "NOM_FORMATION")
                .withValeurPossible(1, "Formation A")
                .withValeurPossible(2, "Formation B");

        // When
        String logString = dto.toLogString();

        // Then
        assertThat(logString).isNotNull();
        assertThat(logString).contains("[formation_csv]");
        assertThat(logString).contains("ligne 5");
        assertThat(logString).contains("NOM_FORMATION");
        assertThat(logString).contains("2 valeurs possibles");
        assertThat(logString).matches("\\[.+\\] ligne \\d+: .+ \\(\\d+ valeurs possibles\\)");
    }

    @Test
    @DisplayName("Doit avoir l'égalité basée sur tous les champs du record")
    void equals_ShouldBeBasedOnAllFields() {
        // Given
        Map<Integer, String> valeurs = Map.of(1, "Test");
        ExcelRowDTO dto1 = new ExcelRowDTO("test", 2, "CODE", "Libellé", "TEXT", "UNIQUE", "OBLIGATOIRE", valeurs);
        ExcelRowDTO dto2 = new ExcelRowDTO("test", 2, "CODE", "Libellé", "TEXT", "UNIQUE", "OBLIGATOIRE", valeurs);
        ExcelRowDTO dto3 = new ExcelRowDTO("test", 2, "CODE2", "Libellé", "TEXT", "UNIQUE", "OBLIGATOIRE", valeurs);

        // When & Then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    @DisplayName("Doit avoir une représentation toString complète")
    void toString_ShouldIncludeAllFields() {
        // Given
        ExcelRowDTO dto = ExcelRowDTO.create(
                "test_csv", 3, "CODE_TEST", "Libellé test",
                "TEXT", "UNIQUE", "OBLIGATOIRE", Map.of(1, "Valeur")
        );

        // When
        String toStringResult = dto.toString();

        // Then
        assertThat(toStringResult).contains("nomCsv");
        assertThat(toStringResult).contains("excelRowNumber");
        assertThat(toStringResult).contains("codeVariable");
        assertThat(toStringResult).contains("libelle");
        assertThat(toStringResult).contains("format");
        assertThat(toStringResult).contains("typeQuestion");
        assertThat(toStringResult).contains("regleGestion");
        assertThat(toStringResult).contains("valeursPossibles");
    }

    @Test
    @DisplayName("Doit gérer les factory methods avec valeursPossibles null")
    void factoryMethods_ShouldHandleNullValeursPossibles() {
        // Given & When
        ExcelRowDTO dtoFromCreate = ExcelRowDTO.create(
                "test", 2, "CODE", "Libellé", "TEXT", "UNIQUE", "OBLIGATOIRE", null
        );

        // Then
        assertThat(dtoFromCreate.valeursPossibles()).isNotNull();
        assertThat(dtoFromCreate.valeursPossibles()).isEmpty();
    }

    @Test
    @DisplayName("Doit préserver l'immutabilité avec des Maps complexes")
    void immutability_ShouldBePreservedWithComplexMaps() {
        // Given
        Map<Integer, String> originalMap = new HashMap<>();
        originalMap.put(1, "Valeur 1");
        originalMap.put(2, "Valeur 2");

        ExcelRowDTO dto = new ExcelRowDTO("test", 2, "CODE", null, null, null, null, originalMap);

        // When - Tentative de modification de la map originale
        originalMap.put(3, "Valeur 3");

        // Then - Le DTO ne doit pas être affecté
        assertThat(dto.valeursPossibles()).hasSize(2);
        assertThat(dto.valeursPossibles()).doesNotContainKey(3);
    }

    @Test
    @DisplayName("Doit utiliser les fonctionnalités Java 21 dans la validation")
    void validation_ShouldUseJava21Features() {
        // Given - Test du pattern matching avec conditions
        ExcelRowDTO dto = new ExcelRowDTO("test", 2, "CODE_VALIDE", null, null, null, null, Map.of());

        // When
        boolean isValid = switch (dto.codeVariable()) {
            case null -> false;
            case String code when code.trim().isEmpty() -> false;
            default -> dto.excelRowNumber() != null && dto.excelRowNumber() > 1;
        };

        // Then
        assertThat(isValid).isTrue();
        assertThat(dto.isValid()).isTrue(); // Cohérence avec l'implémentation
    }
}