package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.domain.model.Secteur;
import com.enterprise.app.domain.model.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormationRepositoryImplTest {

    @Mock
    private JpaFormationRepository jpaRepository;

    @InjectMocks
    private FormationRepositoryImpl formationRepository;

    private Formation formation;
    private Long formationId;
    private Pageable pageable;
    private Secteur secteur;
    private Region region;
    private Long secteurId;
    private Long regionId;

    @BeforeEach
    void setUp() {
        formationId = 1L;
        secteurId = 1L;
        regionId = 1L;
        pageable = PageRequest.of(0, 10);

        secteur = Secteur.builder()
            .id(secteurId)
            .code("IT")
            .nom("Informatique")
            .actif(true)
            .build();

        region = Region.builder()
            .id(regionId)
            .code("IDF")
            .nom("Île-de-France")
            .actif(true)
            .build();

        formation = Formation.builder()
            .id(formationId)
            .libelle("Formation Test")
            .formateurs("Formateur Test")
            .description("Description test")
            .dateFormation(LocalDate.now().plusDays(7))
            .heureDebut(LocalTime.of(9, 0))
            .heureFin(LocalTime.of(17, 0))
            .secteur(secteur)
            .region(region)
            .modalite(ModaliteFormation.PRESENTIEL)
            .nbParticipants(20)
            .lieu("Paris")
            .ville("Paris")
            .build();
    }

    @Test
    void findById_ShouldReturnFormation_WhenExists() {
        // Given
        when(jpaRepository.findByIdWithRelations(formationId)).thenReturn(Optional.of(formation));

        // When
        Optional<Formation> result = formationRepository.findById(formationId);

        // Then
        assertThat(result).isPresent();
        assertThat(formation).isEqualTo(result.get());
        verify(jpaRepository).findByIdWithRelations(formationId);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(jpaRepository.findByIdWithRelations(formationId)).thenReturn(Optional.empty());

        // When
        Optional<Formation> result = formationRepository.findById(formationId);

        // Then
        assertThat(result).isEmpty();
        verify(jpaRepository).findByIdWithRelations(formationId);
    }

    @Test
    void save_ShouldValidateAndSaveFormation() {
        // Given
        when(jpaRepository.save(formation)).thenReturn(formation);

        // When
        Formation result = formationRepository.save(formation);

        // Then
        assertThat(result).isEqualTo(formation);
        verify(jpaRepository).save(formation);
    }

    @Test
    void save_ShouldThrowException_WhenInvalidDates() {
        // Given
        Formation invalidFormation = Formation.builder()
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(17, 0)) // End before start
                .heureFin(LocalTime.of(9, 0))
                .secteur(secteur)
                .region(region)
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

        // When & Then
        assertThatThrownBy(() -> formationRepository.save(invalidFormation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heure de fin doit être postérieure");

        verify(jpaRepository, never()).save(any());
    }

    @Test
    void save_ShouldThrowException_WhenInvalidNbParticipants() {
        // Given
        Formation invalidFormation = Formation.builder()
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur(secteur)
                .region(region)
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(0) // Invalid
                .lieu("Paris")
                .ville("Paris")
                .build();

        // When & Then
        assertThatThrownBy(() -> formationRepository.save(invalidFormation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre de participants doit être positif");

        verify(jpaRepository, never()).save(any());
    }

    @Test
    void deleteById_ShouldCallJpaRepository() {
        // When
        formationRepository.deleteById(formationId);

        // Then
        verify(jpaRepository).deleteById(formationId);
    }

    @Test
    void existsById_ShouldReturnTrue_WhenExists() {
        // Given
        when(jpaRepository.existsById(formationId)).thenReturn(true);

        // When
        boolean result = formationRepository.existsById(formationId);

        // Then
        assertThat(result).isTrue();
        verify(jpaRepository).existsById(formationId);
    }

    @Test
    void existsById_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(jpaRepository.existsById(formationId)).thenReturn(false);

        // When
        boolean result = formationRepository.existsById(formationId);

        // Then
        assertThat(result).isFalse();
        verify(jpaRepository).existsById(formationId);
    }

    @Test
    void count_ShouldReturnCount() {
        // Given
        when(jpaRepository.count()).thenReturn(5L);

        // When
        long result = formationRepository.count();

        // Then
        assertThat(result).isEqualTo(5L);
        verify(jpaRepository).count();
    }

    @Test
    void countParticipantsInscrits_ShouldReturnCount() {
        // Given
        when(jpaRepository.countParticipantsInscrits(formationId)).thenReturn(15);

        // When
        int result = formationRepository.countParticipantsInscrits(formationId);

        // Then
        assertThat(result).isEqualTo(15);
        verify(jpaRepository).countParticipantsInscrits(formationId);
    }

    @Test
    void isFormationComplete_ShouldReturnTrue_WhenComplete() {
        // Given
        when(jpaRepository.isFormationComplete(formationId)).thenReturn(true);

        // When
        boolean result = formationRepository.isFormationComplete(formationId);

        // Then
        assertThat(result).isTrue();
        verify(jpaRepository).isFormationComplete(formationId);
    }

    @Test
    void isFormationComplete_ShouldReturnFalse_WhenNotComplete() {
        // Given
        when(jpaRepository.isFormationComplete(formationId)).thenReturn(false);

        // When
        boolean result = formationRepository.isFormationComplete(formationId);

        // Then
        assertThat(result).isFalse();
        verify(jpaRepository).isFormationComplete(formationId);
    }

    @Test
    void isFormationComplete_ShouldReturnFalse_WhenNull() {
        // Given
        when(jpaRepository.isFormationComplete(formationId)).thenReturn(null);

        // When
        boolean result = formationRepository.isFormationComplete(formationId);

        // Then
        assertThat(result).isFalse();
        verify(jpaRepository).isFormationComplete(formationId);
    }

}