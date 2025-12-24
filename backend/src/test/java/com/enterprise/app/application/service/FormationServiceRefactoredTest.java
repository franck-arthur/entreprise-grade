package com.enterprise.app.application.service;

import com.enterprise.app.domain.exception.BusinessException;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.domain.model.User;
import com.enterprise.app.domain.repository.FormationParticipationRepository;
import com.enterprise.app.domain.repository.FormationRepository;
import com.enterprise.app.domain.repository.UserRepository;
import com.enterprise.app.testing.config.BaseUnitTest;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import com.enterprise.app.testing.fixtures.UserFixtures;
import com.enterprise.app.testing.builders.FormationTestDataBuilder;
import com.enterprise.app.testing.builders.UserTestDataBuilder;
import com.enterprise.app.testing.helpers.FormationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test refactorisé du FormationService utilisant l'architecture centralisée des mocks.
 * Démontre l'utilisation des fixtures, builders, et helpers centralisés.
 */
@DisplayName("Formation Service - Tests Refactorisés")
class FormationServiceRefactoredTest extends BaseUnitTest {

    @Mock
    private FormationRepository formationRepository;

    @Mock
    private FormationParticipationRepository participationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FormationService formationService;

    private UUID formationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        formationId = FormationFixtures.FORMATION_ID_1;
        userId = UserFixtures.USER_ID_1;
    }

    @Override
    protected Object[] getAllMocks() {
        return new Object[]{formationRepository, participationRepository, userRepository};
    }

    @Test
    @DisplayName("Should return formation when it exists")
    void getFormationById_ShouldReturnFormation_WhenFormationExists() {
        // Given
        Formation expectedFormation = FormationFixtures.defaultFormationPresentiel();
        when(formationRepository.findById(formationId)).thenReturn(Optional.of(expectedFormation));

        // When
        Formation result = formationService.getFormationById(formationId);

        // Then
        FormationTestHelper.assertFormationEquals(expectedFormation, result);
        verify(formationRepository).findById(formationId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when formation does not exist")
    void getFormationById_ShouldThrowException_WhenFormationNotFound() {
        // Given
        when(formationRepository.findById(formationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> formationService.getFormationById(formationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Formation non trouvée");

        verify(formationRepository).findById(formationId);
    }

    @Test
    @DisplayName("Should save and return formation when creating")
    void createFormation_ShouldSaveAndReturnFormation() {
        // Given
        Formation formationToCreate = FormationTestDataBuilder.aFormation()
                .presentiel()
                .aVenir()
                .build();

        Formation savedFormation = formationToCreate.toBuilder()
                .id(UUID.randomUUID())
                .build();

        when(formationRepository.save(formationToCreate)).thenReturn(savedFormation);

        // When
        Formation result = formationService.createFormation(formationToCreate);

        // Then
        FormationTestHelper.assertFormationEquals(savedFormation, result);
        FormationTestHelper.assertFormationPresentielleIsValid(result);
        verify(formationRepository).save(formationToCreate);
    }

    @Test
    @DisplayName("Should create participation when user inscription is valid")
    void inscrireUtilisateur_ShouldCreateParticipation_WhenValid() {
        // Given
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        User user = UserFixtures.defaultUser();

        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(false);
        when(formationRepository.countParticipantsInscrits(formationId)).thenReturn(5);
        when(participationRepository.save(any(FormationParticipation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        FormationParticipation result = formationService.inscrireUtilisateur(formationId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFormation()).isEqualTo(formation);
        assertThat(result.getUser()).isEqualTo(user);

        verify(formationRepository).findById(formationId);
        verify(userRepository).findById(userId);
        verify(participationRepository).existsByFormationIdAndUserId(formationId, userId);
        verify(formationRepository).countParticipantsInscrits(formationId);
        verify(participationRepository).save(any(FormationParticipation.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when user already registered")
    void inscrireUtilisateur_ShouldThrowException_WhenUserAlreadyRegistered() {
        // Given
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        User user = UserFixtures.defaultUser();

        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> formationService.inscrireUtilisateur(formationId, userId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("déjà inscrit");

        verify(formationRepository).findById(formationId);
        verify(userRepository).findById(userId);
        verify(participationRepository).existsByFormationIdAndUserId(formationId, userId);
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessException when formation is full")
    void inscrireUtilisateur_ShouldThrowException_WhenFormationIsFull() {
        // Given
        Formation formation = FormationTestDataBuilder.aFormation()
                .withId(formationId)
                .withNbParticipants(10)
                .presentiel()
                .aVenir()
                .build();

        User user = UserFixtures.defaultUser();

        when(formationRepository.findById(formationId)).thenReturn(Optional.of(formation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByFormationIdAndUserId(formationId, userId)).thenReturn(false);
        when(formationRepository.countParticipantsInscrits(formationId)).thenReturn(10); // Formation complète

        // When & Then
        assertThatThrownBy(() -> formationService.inscrireUtilisateur(formationId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Formation complète");

        verify(formationRepository).findById(formationId);
        verify(userRepository).findById(userId);
        verify(participationRepository).existsByFormationIdAndUserId(formationId, userId);
        verify(formationRepository).countParticipantsInscrits(formationId);
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessException when trying to update finished formation")
    void updateFormation_ShouldThrowException_WhenFormationIsFinished() {
        // Given
        Formation finishedFormation = FormationTestDataBuilder.aFormation()
                .withId(formationId)
                .terminee()
                .build();

        Formation updateData = FormationFixtures.defaultFormationPresentiel();

        when(formationRepository.findById(formationId)).thenReturn(Optional.of(finishedFormation));

        // When & Then
        assertThatThrownBy(() -> formationService.updateFormation(formationId, updateData))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formation terminée");

        verify(formationRepository).findById(formationId);
        verify(formationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when presentiel formation missing location")
    void createFormation_ShouldThrowException_WhenPresentielWithoutLocation() {
        // Given - Utilisation du builder pour créer des données invalides sans validation
        Formation formationSansLieu = FormationTestDataBuilder.aFormation()
                .withModalite(com.enterprise.app.domain.model.ModaliteFormation.PRESENTIEL)
                .withLieu(null)
                .withVille(null)
                .buildWithoutValidation();

        // When & Then
        assertThatThrownBy(() -> formationService.createFormation(formationSansLieu))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ville est obligatoire");

        verify(formationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when online formation missing link")
    void createFormation_ShouldThrowException_WhenEnLigneWithoutLink() {
        // Given
        Formation formationSansLien = FormationTestDataBuilder.aFormation()
                .withModalite(com.enterprise.app.domain.model.ModaliteFormation.EN_LIGNE)
                .withLienParticipation(null)
                .buildWithoutValidation();

        // When & Then
        assertThatThrownBy(() -> formationService.createFormation(formationSansLien))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lien de participation est obligatoire");

        verify(formationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should succeed when creating valid presentiel formation")
    void createFormation_ShouldSucceed_WhenPresentielWithLocationData() {
        // Given
        Formation formationPresentiel = FormationTestDataBuilder.aFormation()
                .presentiel()
                .aVenir()
                .build();

        when(formationRepository.save(formationPresentiel)).thenReturn(formationPresentiel);

        // When
        Formation result = formationService.createFormation(formationPresentiel);

        // Then
        FormationTestHelper.assertFormationPresentielleIsValid(result);
        verify(formationRepository).save(formationPresentiel);
    }

    @Test
    @DisplayName("Should succeed when creating valid online formation")
    void createFormation_ShouldSucceed_WhenEnLigneWithLink() {
        // Given
        Formation formationEnLigne = FormationTestDataBuilder.aFormation()
                .enLigne()
                .aVenir()
                .build();

        when(formationRepository.save(formationEnLigne)).thenReturn(formationEnLigne);

        // When
        Formation result = formationService.createFormation(formationEnLigne);

        // Then
        FormationTestHelper.assertFormationEnLigneIsValid(result);
        verify(formationRepository).save(formationEnLigne);
    }

    @Test
    @DisplayName("Should succeed when creating valid hybrid formation")
    void createFormation_ShouldSucceed_WhenHybridWithAllRequiredData() {
        // Given
        Formation formationHybride = FormationTestDataBuilder.aFormation()
                .hybride()
                .aVenir()
                .build();

        when(formationRepository.save(formationHybride)).thenReturn(formationHybride);

        // When
        Formation result = formationService.createFormation(formationHybride);

        // Then
        FormationTestHelper.assertFormationHybrideIsValid(result);
        verify(formationRepository).save(formationHybride);
    }
}