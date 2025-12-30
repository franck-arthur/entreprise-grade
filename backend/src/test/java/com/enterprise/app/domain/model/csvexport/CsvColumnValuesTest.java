package com.enterprise.app.domain.model.csvexport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests pour CsvColumnValues")
class CsvColumnValuesTest {

    @Mock
    private CsvColumnDefinition mockColumnDefinition;

    @Test
    @DisplayName("Doit créer une instance valide avec le builder")
    void builder_ShouldCreateValidInstance() {
        // Given
        String valeurPossible = "Valeur test";
        Integer position = 1;

        // When
        CsvColumnValues columnValues = CsvColumnValues.builder()
                .columnDefinition(mockColumnDefinition)
                .position(position)
                .valeurPossible(valeurPossible)
                .build();

        // Then
        assertThat(columnValues).isNotNull();
        assertThat(columnValues.getColumnDefinition()).isEqualTo(mockColumnDefinition);
        assertThat(columnValues.getPosition()).isEqualTo(position);
        assertThat(columnValues.getValeurPossible()).isEqualTo(valeurPossible);
    }

    @Test
    @DisplayName("Doit créer une instance avec la méthode factory 'of'")
    void of_ShouldCreateValidInstance() {
        // Given
        String valeurPossible = "Valeur test";
        Integer position = 2;

        // When
        CsvColumnValues columnValues = CsvColumnValues.of(mockColumnDefinition, position, valeurPossible);

        // Then
        assertThat(columnValues).isNotNull();
        assertThat(columnValues.getColumnDefinition()).isEqualTo(mockColumnDefinition);
        assertThat(columnValues.getPosition()).isEqualTo(position);
        assertThat(columnValues.getValeurPossible()).isEqualTo(valeurPossible);
    }

    @Test
    @DisplayName("Doit avoir l'égalité basée sur columnDefinition et position")
    void equals_ShouldBeBasedOnColumnDefinitionAndPosition() {
        // Given
        CsvColumnValues columnValues1 = CsvColumnValues.builder()
                .columnDefinition(mockColumnDefinition)
                .position(1)
                .valeurPossible("Valeur 1")
                .build();

        CsvColumnValues columnValues2 = CsvColumnValues.builder()
                .columnDefinition(mockColumnDefinition)
                .position(1)
                .valeurPossible("Valeur 2")
                .build();

        CsvColumnValues columnValues3 = CsvColumnValues.builder()
                .columnDefinition(mockColumnDefinition)
                .position(2)
                .valeurPossible("Valeur 1")
                .build();

        // When & Then
        assertThat(columnValues1).isEqualTo(columnValues2); // Même definition et position
        assertThat(columnValues1).isNotEqualTo(columnValues3); // Position différente
        assertThat(columnValues1.hashCode()).isEqualTo(columnValues2.hashCode());
    }

    @Test
    @DisplayName("Doit exclure columnDefinition du toString pour éviter les références circulaires")
    void toString_ShouldExcludeColumnDefinition() {
        // Given
        CsvColumnValues columnValues = CsvColumnValues.builder()
                .columnDefinition(mockColumnDefinition)
                .position(1)
                .valeurPossible("Test")
                .build();

        // When
        String toStringResult = columnValues.toString();

        // Then
        assertThat(toStringResult).doesNotContain("columnDefinition");
        assertThat(toStringResult).contains("position");
        assertThat(toStringResult).contains("valeurPossible");
    }

    @Test
    @DisplayName("Doit accepter des valeurs nulles pour les champs optionnels")
    void builder_ShouldHandleNullValues() {
        // Given & When
        CsvColumnValues columnValues = CsvColumnValues.builder()
                .columnDefinition(mockColumnDefinition)
                .position(1)
                .valeurPossible(null)
                .build();

        // Then
        assertThat(columnValues).isNotNull();
        assertThat(columnValues.getValeurPossible()).isNull();
    }

    @Test
    @DisplayName("Doit gérer les valeurs par défaut du constructeur NoArgsConstructor")
    void noArgsConstructor_ShouldCreateInstanceWithNullValues() {
        // When
        CsvColumnValues columnValues = new CsvColumnValues();

        // Then
        assertThat(columnValues).isNotNull();
        assertThat(columnValues.getId()).isNull();
        assertThat(columnValues.getColumnDefinition()).isNull();
        assertThat(columnValues.getPosition()).isNull();
        assertThat(columnValues.getValeurPossible()).isNull();
        assertThat(columnValues.getDateCreation()).isNull();
    }

    @Test
    @DisplayName("Doit utiliser AllArgsConstructor correctement")
    void allArgsConstructor_ShouldSetAllFields() {
        // Given
        Long id = 1L;
        Integer position = 3;
        String valeur = "Test valeur";
        LocalDateTime dateCreation = LocalDateTime.now();

        // When
        CsvColumnValues columnValues = new CsvColumnValues(id, mockColumnDefinition, position, valeur, dateCreation);

        // Then
        assertThat(columnValues.getId()).isEqualTo(id);
        assertThat(columnValues.getColumnDefinition()).isEqualTo(mockColumnDefinition);
        assertThat(columnValues.getPosition()).isEqualTo(position);
        assertThat(columnValues.getValeurPossible()).isEqualTo(valeur);
        assertThat(columnValues.getDateCreation()).isEqualTo(dateCreation);
    }

    @Test
    @DisplayName("Doit permettre la modification des propriétés via les setters")
    void setters_ShouldModifyProperties() {
        // Given
        CsvColumnValues columnValues = new CsvColumnValues();
        String nouvelleValeur = "Nouvelle valeur";
        Integer nouvellePosition = 5;

        // When
        columnValues.setPosition(nouvellePosition);
        columnValues.setValeurPossible(nouvelleValeur);
        columnValues.setColumnDefinition(mockColumnDefinition);

        // Then
        assertThat(columnValues.getPosition()).isEqualTo(nouvellePosition);
        assertThat(columnValues.getValeurPossible()).isEqualTo(nouvelleValeur);
        assertThat(columnValues.getColumnDefinition()).isEqualTo(mockColumnDefinition);
    }
}