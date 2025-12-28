package com.enterprise.app.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

class FormationTest {

    @Test
    void getStatut_ShouldReturnAVenir_WhenFormationIsInFuture() {
        // Given
        Formation formation = Formation.builder()
                .dateFormation(LocalDate.now().plusDays(1))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .build();

        // When
        FormationStatut statut = formation.getStatut();

        // Then
        assertThat(statut).isEqualTo(FormationStatut.A_VENIR);
    }

    @Test
    void getStatut_ShouldReturnTerminee_WhenFormationIsInPast() {
        // Given
        Formation formation = Formation.builder()
                .dateFormation(LocalDate.now().minusDays(1))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .build();

        // When
        FormationStatut statut = formation.getStatut();

        // Then
        assertThat(statut).isEqualTo(FormationStatut.TERMINEE);
    }

    @Test
    void getStatut_ShouldReturnEnCours_WhenFormationIsOngoing() {
        // Given - Formation today with extended hours to ensure it's ongoing
        Formation formation = Formation.builder()
                .dateFormation(LocalDate.now())
                .heureDebut(LocalTime.of(0, 0))
                .heureFin(LocalTime.of(23, 59))
                .build();

        // When
        FormationStatut statut = formation.getStatut();

        // Then
        assertThat(statut).isEqualTo(FormationStatut.EN_COURS);
    }

    @Test
    void peutAccepterInscription_ShouldReturnTrue_WhenFormationIsAVenirAndNotComplete() {
        // Given
        Formation formation = Formation.builder()
                .dateFormation(LocalDate.now().plusDays(1))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .build();

        // When
        boolean result = formation.peutAccepterInscription();

        // Then
        assertThat(result).isTrue(); // true car isComplet() retourne toujours false dans notre implémentation simplifiée
    }

    @Test
    void getDateDebutFormation_ShouldReturnCorrectDateTime() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 15);
        LocalTime time = LocalTime.of(9, 30);
        Formation formation = Formation.builder()
                .dateFormation(date)
                .heureDebut(time)
                .build();

        // When
        var result = formation.getDateDebutFormation();

        // Then
        assertThat(result.toLocalDate()).isEqualTo(date);
        assertThat(result.toLocalTime()).isEqualTo(time);
    }

    @Test
    void getDateFinFormation_ShouldReturnCorrectDateTime() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 15);
        LocalTime time = LocalTime.of(17, 30);
        Formation formation = Formation.builder()
                .dateFormation(date)
                .heureFin(time)
                .build();

        // When
        var result = formation.getDateFinFormation();

        // Then
        assertThat(result.toLocalDate()).isEqualTo(date);
        assertThat(result.toLocalTime()).isEqualTo(time);
    }
}