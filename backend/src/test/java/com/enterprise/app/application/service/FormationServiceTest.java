package com.enterprise.app.application.service;

import com.enterprise.app.domain.exception.BusinessException;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.domain.repository.FormationParticipationRepository;
import com.enterprise.app.domain.repository.FormationRepository;
import com.enterprise.app.domain.repository.UserRepository;
import com.enterprise.app.domain.repository.SecteurRepository;
import com.enterprise.app.domain.repository.RegionRepository;
import com.enterprise.app.testing.fixtures.FormationParticipationFixtures;
import com.enterprise.app.testing.fixtures.UserFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static com.enterprise.app.testing.fixtures.FormationFixtures.*;
import static com.enterprise.app.testing.fixtures.FormationFixtures.tirageAuSortSecretaireEnCours;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormationServiceTest {

    @Mock
    private FormationRepository formationRepository;

    @Mock
    private FormationParticipationRepository participationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecteurRepository secteurRepository;

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private FormationService formationService;

    private Formation formation;
    private User user;
    private Region region;
    private Long formationId;
    private Long userId;

    @BeforeEach
    void setUp() {
        formationId = 1L;
        userId = 1L;

        formation = defaultFormationPresentiel();

        user = UserFixtures.defaultUser();
    }

    @Test
    @DisplayName("Doit retourner une formation quand elle existe")
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
    @DisplayName("Doit lancer une exception quand la formation n'est pas trouvée")
    void getFormationById_ShouldThrowException_WhenFormationNotFound() {
        // Given
        when(formationRepository.findById(formationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> formationService.getFormationById(formationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Formation non trouvée");
    }

    @Test
    @DisplayName("Doit sauvegarder et retourner une formation lors de la création")
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
    @DisplayName("Doit créer une participation quand l'inscription est valide")
    void inscrireUtilisateur_ShouldCreateParticipation_WhenValid() {
        // Given
        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(false);
        when(formationRepository.countParticipantsInscrits(formationId)).thenReturn(5);

        FormationParticipation expectedParticipation = FormationParticipation.builder()
                .formation(formation)
                .user(user)
                .statutParticipation(StatutParticipation.ABSENT)
                .build();

        when(participationRepository.save(any(FormationParticipation.class))).thenReturn(expectedParticipation);

        // When
        FormationParticipation result = formationService.inscrireUtilisateur(formationId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFormation()).isEqualTo(formation);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);

        verify(participationRepository).save(any(FormationParticipation.class));
    }

    @Test
    @DisplayName("Doit lancer une exception quand l'utilisateur est déjà inscrit")
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
    @DisplayName("Doit lancer une exception quand la formation est complète")
    void inscrireUtilisateur_ShouldThrowException_WhenFormationIsFull() {
        // Given
        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(false);
        when(formationRepository.countParticipantsInscrits(formationId)).thenReturn(30); // Formation complète

        // When & Then
        assertThatThrownBy(() -> formationService.inscrireUtilisateur(formationId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Formation complète");
    }

    @Test
    @DisplayName("Doit mettre à jour la présence et la date utilisateur quand présent")
    void marquerPresence_ShouldUpdatePresenceAndUserDate_WhenPresent() {
        // Given
        FormationParticipation participation = FormationParticipationFixtures.participationPresent();

        when(participationRepository.findByFormationIdAndUserId(formationId, userId))
                .thenReturn(Optional.of(participation));
        when(userRepository.save(participation.getUser())).thenReturn(user);
        when(participationRepository.save(participation)).thenReturn(participation);

        // When
        FormationParticipation result = formationService.marquerPresence(formationId, userId, true);

        // Then
        assertThat(result.getStatutParticipation()).isEqualTo(StatutParticipation.PRESENT);

        verify(userRepository).save(participation.getUser());
        verify(participationRepository).save(participation);
    }

    @Test
    @DisplayName("Doit mettre à jour l'absence et réinitialiser la date utilisateur quand absent")
    void marquerPresence_ShouldUpdateAbsenceAndResetUserDate_WhenAbsent() {
        // Given
        FormationParticipation participation = FormationParticipationFixtures.participationPresent();

        when(participationRepository.findByFormationIdAndUserId(formationId, userId))
                .thenReturn(Optional.of(participation));
        when(userRepository.save(participation.getUser())).thenReturn(participation.getUser());
        when(participationRepository.save(participation)).thenReturn(participation);

        // When
        FormationParticipation result = formationService.marquerPresence(formationId, userId, false);

        // Then
        assertThat(result.getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);
        assertThat(result.getDatePresence()).isNull();
        assertThat(user.getDateDerniereFormation()).isNull();

        verify(userRepository).save(any(User.class));
        verify(participationRepository).save(participation);
    }

    @Test
    @DisplayName("Doit lancer une exception quand la formation est terminée")
    void updateFormation_ShouldThrowException_WhenFormationIsFinished() {
        // Given
        Formation finishedFormation = tirageAuSortSecretaireEnCours().toBuilder()
                .dateFormation(LocalDate.now().minusDays(2))
                .build();

        when(formationRepository.findById(formationId)).thenReturn(Optional.of(finishedFormation));

        // When & Then
        assertThatThrownBy(() -> formationService.updateFormation(formationId, formation))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formation terminée");
    }

    @Test
    @DisplayName("Doit réussir la création d'une formation présentielle avec ville et lieu")
    void createFormation_ShouldSucceed_WhenPresentielWithVilleAndLieu() {
        // Given
        Formation formationPresentiel = defaultFormationPresentiel();

        when(formationRepository.save(any(Formation.class))).thenReturn(formationPresentiel);

        // When
        Formation result = formationService.createFormation(formationPresentiel);

        // Then
        assertThat(result).isNotNull();
        verify(formationRepository).save(formationPresentiel);
    }

    @Test
    @DisplayName("Doit réussir la création d'une formation en ligne avec lien")
    void createFormation_ShouldSucceed_WhenEnLigneWithLien() {
        // Given
        Formation formationEnLigne = tirageAuSortEnLigneRG();

        when(formationRepository.save(any(Formation.class))).thenReturn(formationEnLigne);

        // When
        Formation result = formationService.createFormation(formationEnLigne);

        // Then
        assertThat(result).isNotNull();
        verify(formationRepository).save(formationEnLigne);
    }

    @Test
    @DisplayName("Doit lancer une exception quand la formation n'est pas en cours")
    void marquerPresence_ShouldThrowException_WhenFormationNotInProgress() {
        // Given
        FormationParticipation participation = FormationParticipationFixtures.defaultParticipation();

        when(participationRepository.findByFormationIdAndUserId(formationId, userId))
                .thenReturn(Optional.of(participation));

        // When & Then
        assertThatThrownBy(() -> formationService.marquerPresence(formationId, userId, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formations en cours");
    }

    @Test
    @DisplayName("Doit lancer une exception quand la formation n'est pas à venir")
    void inscrireUtilisateur_ShouldThrowException_WhenFormationNotAVenir() {
        // Given
        Formation formationEnCours = tirageAuSortSecretaireEnCours();

        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formationEnCours));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> formationService.inscrireUtilisateur(formationId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Inscription impossible");
    }

    @Test
    @DisplayName("Doit lancer une exception lors de la désinscription si la formation n'est pas à venir")
    void desinscrireUtilisateur_ShouldThrowException_WhenFormationNotAVenir() {
        // Given
        FormationParticipation participation = FormationParticipationFixtures.participationPresent();

        when(participationRepository.findByFormationIdAndUserId(formationId, userId))
                .thenReturn(Optional.of(participation));

        // When & Then
        assertThatThrownBy(() -> formationService.desinscrireUtilisateur(formationId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formation déjà commencée");
    }
}