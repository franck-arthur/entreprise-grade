package com.enterprise.app.domain.model.csvexport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests pour CsvColumnDefinition")
class CsvColumnDefinitionTest {

    private CsvColumnDefinition csvColumnDefinition;

    @BeforeEach
    void setUp() {
        csvColumnDefinition = CsvColumnDefinition.builder()
                .nomCsv("nom_test")
                .ordreColonne(1)
                .codeVariable("CODE_TEST")
                .libelle("Libellé test")
                .format("TEXT")
                .typeQuestion("UNIQUE")
                .regleGestion("OBLIGATOIRE")
                .actif(true)
                .build();
    }

    @Test
    @DisplayName("Doit créer une instance valide avec le builder")
    void builder_ShouldCreateValidInstance() {
        // Given & When
        CsvColumnDefinition definition = CsvColumnDefinition.builder()
                .nomCsv("test_csv")
                .ordreColonne(2)
                .codeVariable("VAR_TEST")
                .build();

        // Then
        assertThat(definition).isNotNull();
        assertThat(definition.getNomCsv()).isEqualTo("test_csv");
        assertThat(definition.getOrdreColonne()).isEqualTo(2);
        assertThat(definition.getCodeVariable()).isEqualTo("VAR_TEST");
        assertThat(definition.getActif()).isTrue(); // Valeur par défaut
        assertThat(definition.getValeursPossibles()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Doit ajouter une valeur possible avec addValeurPossible")
    void addValeurPossible_ShouldAddValueToList() {
        // Given
        Integer position = 1;
        String valeur = "Valeur test";

        // When
        csvColumnDefinition.addValeurPossible(position, valeur);

        // Then
        List<CsvColumnValues> valeurs = csvColumnDefinition.getValeursPossibles();
        assertThat(valeurs).hasSize(1);
        assertThat(valeurs.get(0).getPosition()).isEqualTo(position);
        assertThat(valeurs.get(0).getValeurPossible()).isEqualTo(valeur);
        assertThat(valeurs.get(0).getColumnDefinition()).isEqualTo(csvColumnDefinition);
    }

    @Test
    @DisplayName("Doit ajouter plusieurs valeurs possibles dans l'ordre")
    void addValeurPossible_ShouldAddMultipleValuesInOrder() {
        // Given & When
        csvColumnDefinition.addValeurPossible(3, "Valeur C");
        csvColumnDefinition.addValeurPossible(1, "Valeur A");
        csvColumnDefinition.addValeurPossible(2, "Valeur B");

        // Then
        List<CsvColumnValues> valeurs = csvColumnDefinition.getValeursPossibles();
        assertThat(valeurs).hasSize(3);
        // Les valeurs doivent être triées par position grâce à @OrderBy
        assertThat(valeurs.get(0).getValeurPossible()).isEqualTo("Valeur A");
        assertThat(valeurs.get(1).getValeurPossible()).isEqualTo("Valeur B");
        assertThat(valeurs.get(2).getValeurPossible()).isEqualTo("Valeur C");
    }

    @Test
    @DisplayName("Doit vider toutes les valeurs possibles avec clearValeursPossibles")
    void clearValeursPossibles_ShouldRemoveAllValues() {
        // Given
        csvColumnDefinition.addValeurPossible(1, "Valeur 1");
        csvColumnDefinition.addValeurPossible(2, "Valeur 2");
        assertThat(csvColumnDefinition.getValeursPossibles()).hasSize(2);

        // When
        csvColumnDefinition.clearValeursPossibles();

        // Then
        assertThat(csvColumnDefinition.getValeursPossibles()).isEmpty();
    }

    @Test
    @DisplayName("Doit retourner true pour hasMapping (implémentation simplifiée)")
    void hasMapping_ShouldReturnTrue() {
        // When
        boolean result = csvColumnDefinition.hasMapping();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Doit retourner la valeur possible à une position donnée")
    void getValeurPossibleAtPosition_ShouldReturnCorrectValue() {
        // Given
        csvColumnDefinition.addValeurPossible(1, "Première valeur");
        csvColumnDefinition.addValeurPossible(2, "Deuxième valeur");
        csvColumnDefinition.addValeurPossible(3, "Troisième valeur");

        // When
        String valeurPosition2 = csvColumnDefinition.getValeurPossibleAtPosition(2);
        String valeurPosition1 = csvColumnDefinition.getValeurPossibleAtPosition(1);

        // Then
        assertThat(valeurPosition2).isEqualTo("Deuxième valeur");
        assertThat(valeurPosition1).isEqualTo("Première valeur");
    }

    @Test
    @DisplayName("Doit retourner null pour une position inexistante")
    void getValeurPossibleAtPosition_ShouldReturnNullForNonExistentPosition() {
        // Given
        csvColumnDefinition.addValeurPossible(1, "Seule valeur");

        // When
        String valeur = csvColumnDefinition.getValeurPossibleAtPosition(999);

        // Then
        assertThat(valeur).isNull();
    }

    @Test
    @DisplayName("Doit avoir l'égalité basée sur nomCsv et codeVariable")
    void equals_ShouldBeBasedOnNomCsvAndCodeVariable() {
        // Given
        CsvColumnDefinition definition1 = CsvColumnDefinition.builder()
                .nomCsv("test")
                .codeVariable("VAR1")
                .ordreColonne(1)
                .build();

        CsvColumnDefinition definition2 = CsvColumnDefinition.builder()
                .nomCsv("test")
                .codeVariable("VAR1")
                .ordreColonne(2) // Ordre différent mais égalité basée sur nomCsv + codeVariable
                .build();

        CsvColumnDefinition definition3 = CsvColumnDefinition.builder()
                .nomCsv("test")
                .codeVariable("VAR2")
                .ordreColonne(1)
                .build();

        // When & Then
        assertThat(definition1).isEqualTo(definition2);
        assertThat(definition1).isNotEqualTo(definition3);
        assertThat(definition1.hashCode()).isEqualTo(definition2.hashCode());
    }

    @Test
    @DisplayName("Doit exclure valeursPossibles du toString pour éviter les références circulaires")
    void toString_ShouldExcludeValeursPossibles() {
        // Given
        csvColumnDefinition.addValeurPossible(1, "Test");

        // When
        String toStringResult = csvColumnDefinition.toString();

        // Then
        assertThat(toStringResult).doesNotContain("valeursPossibles");
        assertThat(toStringResult).contains("nomCsv");
        assertThat(toStringResult).contains("codeVariable");
    }

    @Test
    @DisplayName("Doit utiliser la valeur par défaut true pour actif")
    void builder_ShouldUseDefaultTrueForActif() {
        // Given & When
        CsvColumnDefinition definition = CsvColumnDefinition.builder()
                .nomCsv("test")
                .codeVariable("TEST")
                .build();

        // Then
        assertThat(definition.getActif()).isTrue();
    }

    @Test
    @DisplayName("Doit permettre de définir actif à false")
    void builder_ShouldAllowSettingActifToFalse() {
        // Given & When
        CsvColumnDefinition definition = CsvColumnDefinition.builder()
                .nomCsv("test")
                .codeVariable("TEST")
                .actif(false)
                .build();

        // Then
        assertThat(definition.getActif()).isFalse();
    }

    @Test
    @DisplayName("Doit utiliser AllArgsConstructor correctement")
    void allArgsConstructor_ShouldSetAllFields() {
        // Given
        Long id = 1L;
        String nomCsv = "test_csv";
        Integer ordreColonne = 5;
        String codeVariable = "CODE_TEST";
        String libelle = "Test libellé";
        String format = "DATE";
        String typeQuestion = "MULTIPLE";
        String regleGestion = "OPTIONNEL";
        Boolean actif = false;
        LocalDateTime dateCreation = LocalDateTime.now();
        LocalDateTime dateModification = LocalDateTime.now();

        // When
        CsvColumnDefinition definition = new CsvColumnDefinition(
                id, nomCsv, ordreColonne, codeVariable, libelle, format,
                typeQuestion, regleGestion, actif, dateCreation, dateModification,
                List.of()
        );

        // Then
        assertThat(definition.getId()).isEqualTo(id);
        assertThat(definition.getNomCsv()).isEqualTo(nomCsv);
        assertThat(definition.getOrdreColonne()).isEqualTo(ordreColonne);
        assertThat(definition.getCodeVariable()).isEqualTo(codeVariable);
        assertThat(definition.getLibelle()).isEqualTo(libelle);
        assertThat(definition.getFormat()).isEqualTo(format);
        assertThat(definition.getTypeQuestion()).isEqualTo(typeQuestion);
        assertThat(definition.getRegleGestion()).isEqualTo(regleGestion);
        assertThat(definition.getActif()).isEqualTo(actif);
        assertThat(definition.getDateCreation()).isEqualTo(dateCreation);
        assertThat(definition.getDateModification()).isEqualTo(dateModification);
    }

    @Test
    @DisplayName("Doit permettre la modification des propriétés via les setters")
    void setters_ShouldModifyProperties() {
        // Given
        CsvColumnDefinition definition = new CsvColumnDefinition();
        String nouveauNom = "nouveau_nom";
        Integer nouvelOrdre = 10;

        // When
        definition.setNomCsv(nouveauNom);
        definition.setOrdreColonne(nouvelOrdre);
        definition.setActif(false);

        // Then
        assertThat(definition.getNomCsv()).isEqualTo(nouveauNom);
        assertThat(definition.getOrdreColonne()).isEqualTo(nouvelOrdre);
        assertThat(definition.getActif()).isFalse();
    }
}