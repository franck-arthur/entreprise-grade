package com.enterprise.app.testing.mocks;

import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.domain.exception.BusinessException;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationParticipation;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import com.enterprise.app.testing.fixtures.FormationParticipationFixtures;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Provider de mocks configurés pour FormationService.
 * Centralise la création et configuration des mocks pour les tests.
 */
public class FormationServiceMockProvider {

    /**
     * Crée un mock avec des comportements de succès standard.
     */
    public static FormationService createSuccessfulMock() {
        FormationService mock = mock(FormationService.class);

        // Configuration des comportements standards
        when(mock.getFormationById(any(UUID.class)))
                .thenReturn(FormationFixtures.defaultFormationPresentiel());

        when(mock.createFormation(any(Formation.class)))
                .thenAnswer(invocation -> {
                    Formation formation = invocation.getArgument(0);
                    return formation.toBuilder().id(UUID.randomUUID()).build();
                });

        when(mock.updateFormation(any(UUID.class), any(Formation.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        doNothing().when(mock).deleteFormation(any(UUID.class));

        // Configuration pour les recherches
        when(mock.searchFormationProjections(anyString(), anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(createEmptyPage());

        return mock;
    }

    /**
     * Crée un mock qui génère des erreurs standard.
     */
    public static FormationService createErrorMock() {
        FormationService mock = mock(FormationService.class);

        when(mock.getFormationById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Formation non trouvée"));

        when(mock.createFormation(any(Formation.class)))
                .thenThrow(new BusinessException("Erreur lors de la création"));

        when(mock.updateFormation(any(UUID.class), any(Formation.class)))
                .thenThrow(new BusinessException("Erreur lors de la mise à jour"));

        doThrow(new BusinessException("Erreur lors de la suppression"))
                .when(mock).deleteFormation(any(UUID.class));

        return mock;
    }

    /**
     * Crée un mock configuré pour les tests de participation.
     */
    public static FormationService createParticipationMock() {
        FormationService mock = createSuccessfulMock();

        when(mock.inscrireUtilisateur(any(UUID.class), any(UUID.class)))
                .thenReturn(FormationParticipationFixtures.defaultParticipation());

        when(mock.desinscrireUtilisateur(any(UUID.class), any(UUID.class)))
                .thenReturn(FormationParticipationFixtures.defaultParticipation());

        when(mock.marquerPresence(any(UUID.class), any(UUID.class), anyBoolean()))
                .thenReturn(FormationParticipationFixtures.participationPresent());

        when(mock.getParticipantsFormation(any(UUID.class)))
                .thenReturn(Arrays.asList(FormationParticipationFixtures.defaultParticipation()));

        return mock;
    }

    /**
     * Crée un mock pour les tests d'erreurs de participation.
     */
    public static FormationService createParticipationErrorMock() {
        FormationService mock = createSuccessfulMock();

        when(mock.inscrireUtilisateur(any(UUID.class), any(UUID.class)))
                .thenThrow(new DuplicateResourceException("Utilisateur déjà inscrit"));

        when(mock.desinscrireUtilisateur(any(UUID.class), any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Participation non trouvée"));

        when(mock.marquerPresence(any(UUID.class), any(UUID.class), anyBoolean()))
                .thenThrow(new BusinessException("Impossible de marquer la présence"));

        return mock;
    }

    /**
     * Crée un mock pour les tests de formation complète.
     */
    public static FormationService createFullFormationMock() {
        FormationService mock = createSuccessfulMock();

        when(mock.inscrireUtilisateur(any(UUID.class), any(UUID.class)))
                .thenThrow(new BusinessException("Formation complète"));

        return mock;
    }

    /**
     * Crée un mock pour les tests de validation métier.
     */
    public static FormationService createValidationMock() {
        FormationService mock = mock(FormationService.class);

        when(mock.createFormation(any(Formation.class)))
                .thenThrow(new IllegalArgumentException("Données invalides"));

        when(mock.updateFormation(any(UUID.class), any(Formation.class)))
                .thenThrow(new BusinessException("Impossible de modifier une formation terminée"));

        return mock;
    }

    /**
     * Configure un mock existant avec des données de recherche.
     */
    public static void configureSearchMock(FormationService mock, List<Formation> formations) {
        when(mock.searchFormationProjections(anyString(), anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(formations));
    }

    /**
     * Configure un mock pour retourner des formations spécifiques.
     */
    public static void configureFormationById(FormationService mock, UUID formationId, Formation formation) {
        when(mock.getFormationById(formationId)).thenReturn(formation);
    }

    /**
     * Configure un mock pour les erreurs spécifiques à un ID.
     */
    public static void configureFormationByIdError(FormationService mock, UUID formationId, String errorMessage) {
        when(mock.getFormationById(formationId))
                .thenThrow(new ResourceNotFoundException(errorMessage));
    }

    /**
     * Crée une page vide pour les tests.
     */
    private static Page createEmptyPage() {
        return new PageImpl<>(Arrays.asList());
    }

    /**
     * Vérifie qu'aucune interaction n'a eu lieu sur le mock.
     */
    public static void verifyNoInteractions(FormationService mock) {
        verifyNoMoreInteractions(mock);
    }

    /**
     * Vérifie les interactions standard de lecture.
     */
    public static void verifyStandardReadInteractions(FormationService mock, UUID formationId) {
        verify(mock).getFormationById(formationId);
        verifyNoMoreInteractions(mock);
    }

    /**
     * Vérifie les interactions standard de création.
     */
    public static void verifyStandardCreateInteractions(FormationService mock) {
        verify(mock).createFormation(any(Formation.class));
        verifyNoMoreInteractions(mock);
    }
}