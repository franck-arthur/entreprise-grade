package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormationRepositoryImplTest {

    @Mock
    private JpaFormationRepository jpaRepository;

    @InjectMocks
    private FormationRepositoryImpl formationRepository;

    private Formation formation;
    private UUID formationId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        formationId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10);

        formation = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .description("Description test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();
    }

    @Test
    void findById_ShouldReturnFormation_WhenExists() {
        // Given
        when(jpaRepository.findById(formationId)).thenReturn(Optional.of(formation));

        // When
        Optional<Formation> result = formationRepository.findById(formationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(formation);
        verify(jpaRepository).findById(formationId);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(jpaRepository.findById(formationId)).thenReturn(Optional.empty());

        // When
        Optional<Formation> result = formationRepository.findById(formationId);

        // Then
        assertThat(result).isEmpty();
        verify(jpaRepository).findById(formationId);
    }

    @Test
    void findAll_ShouldReturnPageOfFormations() {
        // Given
        Page<Formation> expectedPage = new PageImpl<>(Arrays.asList(formation), pageable, 1);
        when(jpaRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<Formation> result = formationRepository.findAll(pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(formation);
        verify(jpaRepository).findAll(pageable);
    }

    @Test
    void findByFilters_ShouldReturnFilteredFormations_WithoutStatutFilter() {
        // Given
        String secteur = "IT";
        String region = "Île-de-France";
        ModaliteFormation modalite = ModaliteFormation.PRESENTIEL;
        Page<Formation> expectedPage = new PageImpl<>(Arrays.asList(formation), pageable, 1);

        when(jpaRepository.findByFilters(secteur, region, modalite, pageable)).thenReturn(expectedPage);

        // When
        Page<Formation> result = formationRepository.findByFilters(secteur, region, modalite, null, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        verify(jpaRepository).findByFilters(secteur, region, modalite, pageable);
    }

    @Test
    void findByFilters_ShouldFilterByStatut_WhenStatutProvided() {
        // Given
        String secteur = "IT";
        String region = "Île-de-France";
        ModaliteFormation modalite = ModaliteFormation.PRESENTIEL;
        FormationStatut statut = FormationStatut.A_VENIR;

        // Formation with future date (A_VENIR status)
        Formation formationAVenir = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

        // Formation with past date (TERMINEE status)
        Formation formationTerminee = Formation.builder()
                .id(UUID.randomUUID())
                .libelle("Formation Passée")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().minusDays(1))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

        Page<Formation> allFormations = new PageImpl<>(Arrays.asList(formationAVenir, formationTerminee), pageable, 2);

        when(jpaRepository.findByFilters(secteur, region, modalite, pageable)).thenReturn(allFormations);

        // When
        Page<Formation> result = formationRepository.findByFilters(secteur, region, modalite, statut, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatut()).isEqualTo(FormationStatut.A_VENIR);
        verify(jpaRepository).findByFilters(secteur, region, modalite, pageable);
    }

    @Test
    void findBySecteur_ShouldReturnFormationsBySecteur() {
        // Given
        String secteur = "IT";
        Page<Formation> expectedPage = new PageImpl<>(Arrays.asList(formation), pageable, 1);
        when(jpaRepository.findBySecteur(secteur, pageable)).thenReturn(expectedPage);

        // When
        Page<Formation> result = formationRepository.findBySecteur(secteur, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        verify(jpaRepository).findBySecteur(secteur, pageable);
    }

    @Test
    void findByRegion_ShouldReturnFormationsByRegion() {
        // Given
        String region = "Île-de-France";
        Page<Formation> expectedPage = new PageImpl<>(Arrays.asList(formation), pageable, 1);
        when(jpaRepository.findByRegion(region, pageable)).thenReturn(expectedPage);

        // When
        Page<Formation> result = formationRepository.findByRegion(region, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        verify(jpaRepository).findByRegion(region, pageable);
    }

    @Test
    void findByModalite_ShouldReturnFormationsByModalite() {
        // Given
        ModaliteFormation modalite = ModaliteFormation.PRESENTIEL;
        Page<Formation> expectedPage = new PageImpl<>(Arrays.asList(formation), pageable, 1);
        when(jpaRepository.findByModalite(modalite, pageable)).thenReturn(expectedPage);

        // When
        Page<Formation> result = formationRepository.findByModalite(modalite, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        verify(jpaRepository).findByModalite(modalite, pageable);
    }

    @Test
    void findByDateFormationBetween_ShouldReturnFormationsBetweenDates() {
        // Given
        LocalDate dateDebut = LocalDate.now();
        LocalDate dateFin = LocalDate.now().plusDays(30);
        Page<Formation> expectedPage = new PageImpl<>(Arrays.asList(formation), pageable, 1);
        when(jpaRepository.findByDateFormationBetween(dateDebut, dateFin, pageable)).thenReturn(expectedPage);

        // When
        Page<Formation> result = formationRepository.findByDateFormationBetween(dateDebut, dateFin, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        verify(jpaRepository).findByDateFormationBetween(dateDebut, dateFin, pageable);
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
                .secteur("IT")
                .region("Île-de-France")
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
                .secteur("IT")
                .region("Île-de-France")
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

    @Test
    void findProjectionById_ShouldReturnProjection_WhenExists() {
        // Given
        FormationProjection projection = mock(FormationProjection.class);
        when(jpaRepository.findProjectionById(formationId)).thenReturn(Optional.of(projection));

        // When
        Optional<FormationProjection> result = formationRepository.findProjectionById(formationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(projection);
        verify(jpaRepository).findProjectionById(formationId);
    }

    @Test
    void findAllProjections_ShouldReturnAllProjections() {
        // Given
        FormationProjection projection1 = mock(FormationProjection.class);
        FormationProjection projection2 = mock(FormationProjection.class);
        List<FormationProjection> projections = Arrays.asList(projection1, projection2);
        when(jpaRepository.findAllProjections()).thenReturn(projections);

        // When
        List<FormationProjection> result = formationRepository.findAllProjections();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(projection1, projection2);
        verify(jpaRepository).findAllProjections();
    }

    @Test
    void findProjectionsByFilters_ShouldReturnFilteredProjections() {
        // Given
        String secteur = "IT";
        String region = "Île-de-France";
        ModaliteFormation modalite = ModaliteFormation.PRESENTIEL;
        FormationProjection projection = mock(FormationProjection.class);
        Page<FormationProjection> expectedPage = new PageImpl<>(Arrays.asList(projection), pageable, 1);

        when(jpaRepository.findProjectionsByFilters(secteur, region, modalite, pageable)).thenReturn(expectedPage);

        // When
        Page<FormationProjection> result = formationRepository.findProjectionsByFilters(secteur, region, modalite, pageable);

        // Then
        assertThat(result).isEqualTo(expectedPage);
        assertThat(result.getContent()).hasSize(1);
        verify(jpaRepository).findProjectionsByFilters(secteur, region, modalite, pageable);
    }
}