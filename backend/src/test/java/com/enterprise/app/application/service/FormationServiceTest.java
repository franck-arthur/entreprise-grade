package com.enterprise.app.application.service;

import com.enterprise.app.domain.exception.BusinessException;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.domain.repository.FormationParticipationRepository;
import com.enterprise.app.domain.repository.FormationRepository;
import com.enterprise.app.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormationServiceTest {

    @Mock
    private FormationRepository formationRepository;

    @Mock
    private FormationParticipationRepository participationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FormationService formationService;

    private Formation formation;
    private User user;
    private UUID formationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        formationId = UUID.randomUUID();
        userId = UUID.randomUUID();

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
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .lienParticipation("https://example.com")
                .build();

        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .active(true)
                .build();
    }

    @Test
    void getFormationById_ShouldReturnFormation_WhenFormationExists() {
        // Given
        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formation));

        // When
        Formation result = formationService.getFormationById(formationId);

        // Then
        assertThat(result).isEqualTo(formation);
        verify(formationRepository).findById(formationId);
    }

    @Test
    void getFormationById_ShouldThrowException_WhenFormationNotFound() {
        // Given
        when(formationRepository.findById(formationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> formationService.getFormationById(formationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Formation non trouvée");
    }

    @Test
    void createFormation_ShouldSaveAndReturnFormation() {
        // Given
        when(formationRepository.save(formation)).thenReturn(formation);

        // When
        Formation result = formationService.createFormation(formation);

        // Then
        assertThat(result).isEqualTo(formation);
        verify(formationRepository).save(formation);
    }

    @Test
    void inscrireUtilisateur_ShouldCreateParticipation_WhenValid() {
        // Given
        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(false);
        when(formationRepository.countParticipantsInscrits(formationId)).thenReturn(5);

        FormationParticipation expectedParticipation = FormationParticipation.builder()
                .formation(formation)
                .user(user)
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();

        when(participationRepository.save(any(FormationParticipation.class))).thenReturn(expectedParticipation);

        // When
        FormationParticipation result = formationService.inscrireUtilisateur(formationId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFormation()).isEqualTo(formation);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getStatutParticipation()).isEqualTo(StatutParticipation.INSCRIT);

        verify(participationRepository).save(any(FormationParticipation.class));
    }

    @Test
    void inscrireUtilisateur_ShouldThrowException_WhenUserAlreadyRegistered() {
        // Given
        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> formationService.inscrireUtilisateur(formationId, userId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("déjà inscrit");
    }

    @Test
    void inscrireUtilisateur_ShouldThrowException_WhenFormationIsFull() {
        // Given
        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(false);
        when(formationRepository.countParticipantsInscrits(formationId)).thenReturn(20); // Formation complète

        // When & Then
        assertThatThrownBy(() -> formationService.inscrireUtilisateur(formationId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Formation complète");
    }

    @Test
    void marquerPresence_ShouldUpdatePresenceAndUserDate_WhenPresent() {
        // Given
        Formation formationEnCours = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now()) // Aujourd'hui
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(23, 59)) // Formation en cours
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

        FormationParticipation participation = FormationParticipation.builder()
                .id(UUID.randomUUID())
                .formation(formationEnCours)
                .user(user)
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();

        when(participationRepository.findByFormationIdAndUserId(formationId, userId))
                .thenReturn(Optional.of(participation));
        when(userRepository.save(user)).thenReturn(user);
        when(participationRepository.save(participation)).thenReturn(participation);

        // When
        FormationParticipation result = formationService.marquerPresence(formationId, userId, true);

        // Then
        assertThat(result.getStatutParticipation()).isEqualTo(StatutParticipation.PRESENT);
        assertThat(result.getDatePresence()).isNotNull();
        assertThat(user.getDateDerniereFormation()).isNotNull();

        verify(userRepository).save(user);
        verify(participationRepository).save(participation);
    }

    @Test
    void marquerPresence_ShouldUpdateAbsenceAndResetUserDate_WhenAbsent() {
        // Given
        Formation formationEnCours = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now()) // Aujourd'hui
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(23, 59)) // Formation en cours
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

        FormationParticipation participation = FormationParticipation.builder()
                .id(UUID.randomUUID())
                .formation(formationEnCours)
                .user(user)
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();

        when(participationRepository.findByFormationIdAndUserId(formationId, userId))
                .thenReturn(Optional.of(participation));
        when(userRepository.save(user)).thenReturn(user);
        when(participationRepository.save(participation)).thenReturn(participation);

        // When
        FormationParticipation result = formationService.marquerPresence(formationId, userId, false);

        // Then
        assertThat(result.getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);
        assertThat(result.getDatePresence()).isNull();
        assertThat(user.getDateDerniereFormation()).isNull();

        verify(userRepository).save(user);
        verify(participationRepository).save(participation);
    }

    @Test
    void updateFormation_ShouldThrowException_WhenFormationIsFinished() {
        // Given
        Formation finishedFormation = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .dateFormation(LocalDate.now().minusDays(1)) // Formation passée
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .build();

        when(formationRepository.findById(formationId)).thenReturn(Optional.of(finishedFormation));

        // When & Then
        assertThatThrownBy(() -> formationService.updateFormation(formationId, formation))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formation terminée");
    }

    @Test
    void createFormation_ShouldThrowException_WhenPresentielWithoutVille() {
        // Given
        Formation formationSansVille = Formation.builder()
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .lieu("Salle 101")
                .ville(null) // Ville manquante
                .build();

        // When & Then
        assertThatThrownBy(() -> formationService.createFormation(formationSansVille))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ville est obligatoire");
    }

    @Test
    void createFormation_ShouldThrowException_WhenPresentielWithoutLieu() {
        // Given
        Formation formationSansLieu = Formation.builder()
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .lieu(null) // Lieu manquant
                .ville("Paris")
                .build();

        // When & Then
        assertThatThrownBy(() -> formationService.createFormation(formationSansLieu))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lieu est obligatoire");
    }

    @Test
    void createFormation_ShouldThrowException_WhenEnLigneWithoutLien() {
        // Given
        Formation formationSansLien = Formation.builder()
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_LIGNE)
                .nbParticipants(20)
                .lienParticipation(null) // Lien manquant
                .build();

        // When & Then
        assertThatThrownBy(() -> formationService.createFormation(formationSansLien))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lien de participation est obligatoire");
    }

    @Test
    void createFormation_ShouldSucceed_WhenPresentielWithVilleAndLieu() {
        // Given
        Formation formationPresentiel = Formation.builder()
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .lieu("Salle 101")
                .ville("Paris")
                .build();

        when(formationRepository.save(any(Formation.class))).thenReturn(formationPresentiel);

        // When
        Formation result = formationService.createFormation(formationPresentiel);

        // Then
        assertThat(result).isNotNull();
        verify(formationRepository).save(formationPresentiel);
    }

    @Test
    void createFormation_ShouldSucceed_WhenEnLigneWithLien() {
        // Given
        Formation formationEnLigne = Formation.builder()
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_LIGNE)
                .nbParticipants(20)
                .lienParticipation("https://example.com/formation")
                .build();

        when(formationRepository.save(any(Formation.class))).thenReturn(formationEnLigne);

        // When
        Formation result = formationService.createFormation(formationEnLigne);

        // Then
        assertThat(result).isNotNull();
        verify(formationRepository).save(formationEnLigne);
    }

    @Test
    void marquerPresence_ShouldThrowException_WhenFormationNotInProgress() {
        // Given
        Formation formationAVenir = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now().plusDays(7)) // Formation future
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

        FormationParticipation participation = FormationParticipation.builder()
                .id(UUID.randomUUID())
                .formation(formationAVenir)
                .user(user)
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();

        when(participationRepository.findByFormationIdAndUserId(formationId, userId))
                .thenReturn(Optional.of(participation));

        // When & Then
        assertThatThrownBy(() -> formationService.marquerPresence(formationId, userId, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formations en cours");
    }

    @Test
    void inscrireUtilisateur_ShouldThrowException_WhenFormationNotAVenir() {
        // Given
        Formation formationEnCours = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now()) // Aujourd'hui
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(23, 59)) // Formation en cours
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formationEnCours));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> formationService.inscrireUtilisateur(formationId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Inscription impossible");
    }

    @Test
    void desinscrireUtilisateur_ShouldThrowException_WhenFormationNotAVenir() {
        // Given
        Formation formationEnCours = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .dateFormation(LocalDate.now()) // Aujourd'hui
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(23, 59)) // Formation en cours
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

        FormationParticipation participation = FormationParticipation.builder()
                .id(UUID.randomUUID())
                .formation(formationEnCours)
                .user(user)
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();

        when(participationRepository.findByFormationIdAndUserId(formationId, userId))
                .thenReturn(Optional.of(participation));

        // When & Then
        assertThatThrownBy(() -> formationService.desinscrireUtilisateur(formationId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formation déjà commencée");
    }
}