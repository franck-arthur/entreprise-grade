package com.enterprise.app.domain.model.csvexport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests pour StatutExecution")
class StatutExecutionTest {

    @Test
    @DisplayName("Doit avoir les bonnes descriptions pour chaque statut")
    void getDescription_ShouldReturnCorrectDescriptions() {
        // When & Then
        assertThat(StatutExecution.EN_COURS.getDescription()).isEqualTo("En cours d'exécution");
        assertThat(StatutExecution.TERMINE.getDescription()).isEqualTo("Terminé avec succès");
        assertThat(StatutExecution.ECHEC.getDescription()).isEqualTo("Échec d'exécution");
        assertThat(StatutExecution.ANNULE.getDescription()).isEqualTo("Annulé par l'utilisateur");
    }

    @Test
    @DisplayName("Doit identifier correctement les statuts terminés")
    void isCompleted_ShouldReturnCorrectValues() {
        // When & Then
        assertThat(StatutExecution.EN_COURS.isCompleted()).isFalse();
        assertThat(StatutExecution.TERMINE.isCompleted()).isTrue();
        assertThat(StatutExecution.ECHEC.isCompleted()).isTrue();
        assertThat(StatutExecution.ANNULE.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Doit identifier correctement les statuts actifs")
    void isActive_ShouldReturnCorrectValues() {
        // When & Then
        assertThat(StatutExecution.EN_COURS.isActive()).isTrue();
        assertThat(StatutExecution.TERMINE.isActive()).isFalse();
        assertThat(StatutExecution.ECHEC.isActive()).isFalse();
        assertThat(StatutExecution.ANNULE.isActive()).isFalse();
    }

    @Test
    @DisplayName("Doit identifier correctement les statuts de succès avec pattern matching")
    void isSuccessful_ShouldUsePatternMatchingCorrectly() {
        // When & Then
        assertThat(StatutExecution.TERMINE.isSuccessful()).isTrue();
        assertThat(StatutExecution.EN_COURS.isSuccessful()).isFalse();
        assertThat(StatutExecution.ECHEC.isSuccessful()).isFalse();
        assertThat(StatutExecution.ANNULE.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Doit identifier correctement les statuts d'échec avec pattern matching")
    void isFailed_ShouldUsePatternMatchingCorrectly() {
        // When & Then
        assertThat(StatutExecution.ECHEC.isFailed()).isTrue();
        assertThat(StatutExecution.ANNULE.isFailed()).isTrue();
        assertThat(StatutExecution.EN_COURS.isFailed()).isFalse();
        assertThat(StatutExecution.TERMINE.isFailed()).isFalse();
    }

    @Test
    @DisplayName("Doit retourner les bonnes priorités pour l'affichage UI")
    void getPriority_ShouldReturnCorrectPriorities() {
        // When & Then
        assertThat(StatutExecution.EN_COURS.getPriority()).isEqualTo(StatutExecution.Priority.INFO);
        assertThat(StatutExecution.TERMINE.getPriority()).isEqualTo(StatutExecution.Priority.SUCCESS);
        assertThat(StatutExecution.ECHEC.getPriority()).isEqualTo(StatutExecution.Priority.ERROR);
        assertThat(StatutExecution.ANNULE.getPriority()).isEqualTo(StatutExecution.Priority.WARNING);
    }

    @Test
    @DisplayName("Doit retourner tous les statuts terminés avec getCompletedStatuses")
    void getCompletedStatuses_ShouldReturnAllCompletedStatuses() {
        // When
        StatutExecution[] completedStatuses = StatutExecution.getCompletedStatuses();

        // Then
        assertThat(completedStatuses).hasSize(3);
        assertThat(completedStatuses).contains(StatutExecution.TERMINE, StatutExecution.ECHEC, StatutExecution.ANNULE);
        assertThat(completedStatuses).doesNotContain(StatutExecution.EN_COURS);
    }

    @ParameterizedTest
    @EnumSource(StatutExecution.class)
    @DisplayName("Doit avoir une description non-nulle pour tous les statuts")
    void allStatuses_ShouldHaveNonNullDescription(StatutExecution statut) {
        // When & Then
        assertThat(statut.getDescription()).isNotNull();
        assertThat(statut.getDescription()).isNotBlank();
    }

    @Test
    @DisplayName("Doit créer le bon statut avec fromCondition pour succès")
    void fromCondition_ShouldReturnTermineForSuccess() {
        // When
        StatutExecution statut = StatutExecution.fromCondition(true, false);

        // Then
        assertThat(statut).isEqualTo(StatutExecution.TERMINE);
    }

    @Test
    @DisplayName("Doit créer le bon statut avec fromCondition pour échec")
    void fromCondition_ShouldReturnEchecForFailure() {
        // When
        StatutExecution statut = StatutExecution.fromCondition(false, false);

        // Then
        assertThat(statut).isEqualTo(StatutExecution.ECHEC);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Doit créer le bon statut avec fromCondition pour annulation")
    void fromCondition_ShouldReturnAnnuleForCancellation(boolean success) {
        // When
        StatutExecution statut = StatutExecution.fromCondition(success, true);

        // Then
        assertThat(statut).isEqualTo(StatutExecution.ANNULE);
    }

    @Test
    @DisplayName("Doit avoir une énumération Priority complète")
    void priority_ShouldHaveAllExpectedValues() {
        // When
        StatutExecution.Priority[] priorities = StatutExecution.Priority.values();

        // Then
        assertThat(priorities).hasSize(4);
        assertThat(priorities).contains(
                StatutExecution.Priority.INFO,
                StatutExecution.Priority.SUCCESS,
                StatutExecution.Priority.WARNING,
                StatutExecution.Priority.ERROR
        );
    }

    @Test
    @DisplayName("Doit maintenir la cohérence entre completed et active")
    void completedAndActive_ShouldBeConsistent() {
        // When & Then - Un statut ne peut pas être à la fois completed et active
        for (StatutExecution statut : StatutExecution.values()) {
            if (statut.isCompleted()) {
                assertThat(statut.isActive()).isFalse();
            }
            if (statut.isActive()) {
                assertThat(statut.isCompleted()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("Doit maintenir la cohérence entre successful et failed")
    void successfulAndFailed_ShouldBeConsistent() {
        // When & Then - Un statut ne peut pas être à la fois successful et failed
        for (StatutExecution statut : StatutExecution.values()) {
            if (statut.isSuccessful()) {
                assertThat(statut.isFailed()).isFalse();
            }
            if (statut.isFailed()) {
                assertThat(statut.isSuccessful()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("Doit avoir exactement un statut actif")
    void exactlyOneStatus_ShouldBeActive() {
        // When
        long activeCount = java.util.Arrays.stream(StatutExecution.values())
                .filter(StatutExecution::isActive)
                .count();

        // Then
        assertThat(activeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Doit avoir exactement un statut de succès")
    void exactlyOneStatus_ShouldBeSuccessful() {
        // When
        long successfulCount = java.util.Arrays.stream(StatutExecution.values())
                .filter(StatutExecution::isSuccessful)
                .count();

        // Then
        assertThat(successfulCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Doit utiliser les nouvelles fonctionnalités Java 21 correctement")
    void javaFeatures_ShouldWorkCorrectly() {
        // Given - Test du pattern matching avec conditions
        StatutExecution statut = StatutExecution.TERMINE;

        // When & Then - Le pattern matching avec switch expression fonctionne
        boolean isSuccessful = switch (statut) {
            case TERMINE -> true;
            case EN_COURS, ECHEC, ANNULE -> false;
        };

        assertThat(isSuccessful).isTrue();
        assertThat(statut.isSuccessful()).isTrue();
    }
}