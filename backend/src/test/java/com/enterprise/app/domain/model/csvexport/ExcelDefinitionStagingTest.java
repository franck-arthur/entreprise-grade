package com.enterprise.app.domain.model.csvexport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests pour ExcelDefinitionStaging")
class ExcelDefinitionStagingTest {

    @Test
    @DisplayName("Doit créer une instance valide avec le builder")
    void builder_ShouldCreateValidInstance() {
        // Given & When
        ExcelDefinitionStaging staging = ExcelDefinitionStaging.builder()
                .nomCsv("test_csv")
                .excelRowNumber(2)
                .codeVariable("TEST_VAR")
                .libelle("Test libellé")
                .format("TEXT")
                .typeQuestion("UNIQUE")
                .regleGestion("OBLIGATOIRE")
                .build();

        // Then
        assertThat(staging).isNotNull();
        assertThat(staging.getNomCsv()).isEqualTo("test_csv");
        assertThat(staging.getExcelRowNumber()).isEqualTo(2);
        assertThat(staging.getCodeVariable()).isEqualTo("TEST_VAR");
        assertThat(staging.getLibelle()).isEqualTo("Test libellé");
        assertThat(staging.getFormat()).isEqualTo("TEXT");
        assertThat(staging.getTypeQuestion()).isEqualTo("UNIQUE");
        assertThat(staging.getRegleGestion()).isEqualTo("OBLIGATOIRE");
    }

    @Test
    @DisplayName("Doit créer un enregistrement de base avec la méthode factory ofBase")
    void ofBase_ShouldCreateBasicRecord() {
        // Given & When
        ExcelDefinitionStaging staging = ExcelDefinitionStaging.ofBase("formation_csv", 3, "NOM_FORMATION");

        // Then
        assertThat(staging).isNotNull();
        assertThat(staging.getNomCsv()).isEqualTo("formation_csv");
        assertThat(staging.getExcelRowNumber()).isEqualTo(3);
        assertThat(staging.getCodeVariable()).isEqualTo("NOM_FORMATION");
        assertThat(staging.getDateImport()).isNotNull();
        assertThat(staging.getDateImport()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(staging.getIndexValeur()).isNull();
        assertThat(staging.getValeurPossible()).isNull();
    }

    @Test
    @DisplayName("Doit créer un enregistrement avec valeur possible avec la méthode factory ofWithValue")
    void ofWithValue_ShouldCreateRecordWithValue() {
        // Given & When
        ExcelDefinitionStaging staging = ExcelDefinitionStaging.ofWithValue(
                "secteur_csv", 4, "TYPE_SECTEUR", 1, "Public"
        );

        // Then
        assertThat(staging).isNotNull();
        assertThat(staging.getNomCsv()).isEqualTo("secteur_csv");
        assertThat(staging.getExcelRowNumber()).isEqualTo(4);
        assertThat(staging.getCodeVariable()).isEqualTo("TYPE_SECTEUR");
        assertThat(staging.getIndexValeur()).isEqualTo(1);
        assertThat(staging.getValeurPossible()).isEqualTo("Public");
        assertThat(staging.getDateImport()).isNotNull();
        assertThat(staging.getDateImport()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Doit détecter la présence d'une valeur possible avec hasValeurPossible")
    void hasValeurPossible_ShouldDetectValuePresence() {
        // Given
        ExcelDefinitionStaging withValue = ExcelDefinitionStaging.ofWithValue(
                "test", 2, "VAR", 1, "Valeur test"
        );
        ExcelDefinitionStaging withEmptyValue = ExcelDefinitionStaging.builder()
                .nomCsv("test")
                .excelRowNumber(2)
                .codeVariable("VAR")
                .valeurPossible("")
                .build();
        ExcelDefinitionStaging withWhitespaceValue = ExcelDefinitionStaging.builder()
                .nomCsv("test")
                .excelRowNumber(2)
                .codeVariable("VAR")
                .valeurPossible("   ")
                .build();
        ExcelDefinitionStaging withoutValue = ExcelDefinitionStaging.ofBase("test", 2, "VAR");

        // When & Then
        assertThat(withValue.hasValeurPossible()).isTrue();
        assertThat(withEmptyValue.hasValeurPossible()).isFalse();
        assertThat(withWhitespaceValue.hasValeurPossible()).isFalse();
        assertThat(withoutValue.hasValeurPossible()).isFalse();
    }

    @Test
    @DisplayName("Doit avoir l'égalité basée sur nomCsv, excelRowNumber, codeVariable et indexValeur")
    void equals_ShouldBeBasedOnCompositeKey() {
        // Given
        ExcelDefinitionStaging staging1 = ExcelDefinitionStaging.builder()
                .nomCsv("test")
                .excelRowNumber(2)
                .codeVariable("VAR1")
                .indexValeur(1)
                .libelle("Libellé différent")
                .build();

        ExcelDefinitionStaging staging2 = ExcelDefinitionStaging.builder()
                .nomCsv("test")
                .excelRowNumber(2)
                .codeVariable("VAR1")
                .indexValeur(1)
                .libelle("Autre libellé")
                .build();

        ExcelDefinitionStaging staging3 = ExcelDefinitionStaging.builder()
                .nomCsv("test")
                .excelRowNumber(2)
                .codeVariable("VAR1")
                .indexValeur(2) // indexValeur différent
                .build();

        ExcelDefinitionStaging staging4 = ExcelDefinitionStaging.builder()
                .nomCsv("test")
                .excelRowNumber(3) // excelRowNumber différent
                .codeVariable("VAR1")
                .indexValeur(1)
                .build();

        // When & Then
        assertThat(staging1).isEqualTo(staging2); // Même clé composite
        assertThat(staging1).isNotEqualTo(staging3); // indexValeur différent
        assertThat(staging1).isNotEqualTo(staging4); // excelRowNumber différent
        assertThat(staging1.hashCode()).isEqualTo(staging2.hashCode());
    }

    @Test
    @DisplayName("Doit utiliser AllArgsConstructor correctement")
    void allArgsConstructor_ShouldSetAllFields() {
        // Given
        Long id = 1L;
        String nomCsv = "test_csv";
        Integer excelRowNumber = 5;
        String codeVariable = "TEST_CODE";
        String libelle = "Test libellé";
        String format = "DATE";
        String typeQuestion = "MULTIPLE";
        String regleGestion = "OPTIONNEL";
        Integer indexValeur = 2;
        String valeurPossible = "Valeur test";
        LocalDateTime dateImport = LocalDateTime.now();
        String fichierSource = "test.xlsx";

        // When
        ExcelDefinitionStaging staging = new ExcelDefinitionStaging(
                id, nomCsv, excelRowNumber, codeVariable, libelle, format,
                typeQuestion, regleGestion, indexValeur, valeurPossible,
                dateImport, fichierSource
        );

        // Then
        assertThat(staging.getId()).isEqualTo(id);
        assertThat(staging.getNomCsv()).isEqualTo(nomCsv);
        assertThat(staging.getExcelRowNumber()).isEqualTo(excelRowNumber);
        assertThat(staging.getCodeVariable()).isEqualTo(codeVariable);
        assertThat(staging.getLibelle()).isEqualTo(libelle);
        assertThat(staging.getFormat()).isEqualTo(format);
        assertThat(staging.getTypeQuestion()).isEqualTo(typeQuestion);
        assertThat(staging.getRegleGestion()).isEqualTo(regleGestion);
        assertThat(staging.getIndexValeur()).isEqualTo(indexValeur);
        assertThat(staging.getValeurPossible()).isEqualTo(valeurPossible);
        assertThat(staging.getDateImport()).isEqualTo(dateImport);
        assertThat(staging.getFichierSource()).isEqualTo(fichierSource);
    }

    @Test
    @DisplayName("Doit permettre la modification des propriétés via les setters")
    void setters_ShouldModifyProperties() {
        // Given
        ExcelDefinitionStaging staging = new ExcelDefinitionStaging();
        String nouveauNom = "nouveau_csv";
        Integer nouvelIndex = 3;
        String nouvelleFichier = "nouveau.xlsx";

        // When
        staging.setNomCsv(nouveauNom);
        staging.setIndexValeur(nouvelIndex);
        staging.setFichierSource(nouvelleFichier);

        // Then
        assertThat(staging.getNomCsv()).isEqualTo(nouveauNom);
        assertThat(staging.getIndexValeur()).isEqualTo(nouvelIndex);
        assertThat(staging.getFichierSource()).isEqualTo(nouvelleFichier);
    }

    @Test
    @DisplayName("Doit gérer les cas limites pour hasValeurPossible")
    void hasValeurPossible_ShouldHandleEdgeCases() {
        // Given
        ExcelDefinitionStaging withNullValue = ExcelDefinitionStaging.builder()
                .nomCsv("test")
                .excelRowNumber(2)
                .codeVariable("VAR")
                .valeurPossible(null)
                .build();

        ExcelDefinitionStaging withMixedWhitespace = ExcelDefinitionStaging.builder()
                .nomCsv("test")
                .excelRowNumber(2)
                .codeVariable("VAR")
                .valeurPossible(" \t \n ")
                .build();

        ExcelDefinitionStaging withValidValue = ExcelDefinitionStaging.builder()
                .nomCsv("test")
                .excelRowNumber(2)
                .codeVariable("VAR")
                .valeurPossible("  valeur valide  ")
                .build();

        // When & Then
        assertThat(withNullValue.hasValeurPossible()).isFalse();
        assertThat(withMixedWhitespace.hasValeurPossible()).isFalse();
        assertThat(withValidValue.hasValeurPossible()).isTrue();
    }

    @Test
    @DisplayName("Doit créer des instances avec les factory methods et préserver l'ordre temporel")
    void factoryMethods_ShouldPreserveTemporalOrder() {
        // Given
        LocalDateTime before = LocalDateTime.now();

        // When
        ExcelDefinitionStaging staging1 = ExcelDefinitionStaging.ofBase("test", 2, "VAR1");
        ExcelDefinitionStaging staging2 = ExcelDefinitionStaging.ofWithValue("test", 3, "VAR2", 1, "Valeur");

        // Then
        assertThat(staging1.getDateImport()).isAfterOrEqualTo(before);
        assertThat(staging2.getDateImport()).isAfterOrEqualTo(before);
        assertThat(staging2.getDateImport()).isAfterOrEqualTo(staging1.getDateImport());
    }

    @Test
    @DisplayName("Doit inclure tous les champs dans toString")
    void toString_ShouldIncludeAllFields() {
        // Given
        ExcelDefinitionStaging staging = ExcelDefinitionStaging.builder()
                .nomCsv("test_csv")
                .excelRowNumber(5)
                .codeVariable("TEST_VAR")
                .valeurPossible("Test valeur")
                .build();

        // When
        String toStringResult = staging.toString();

        // Then
        assertThat(toStringResult).contains("nomCsv");
        assertThat(toStringResult).contains("excelRowNumber");
        assertThat(toStringResult).contains("codeVariable");
        assertThat(toStringResult).contains("valeurPossible");
    }
}