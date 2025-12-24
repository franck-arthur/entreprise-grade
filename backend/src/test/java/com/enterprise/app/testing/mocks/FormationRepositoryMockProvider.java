package com.enterprise.app.testing.mocks;

import com.enterprise.app.domain.repository.FormationRepository;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Provider de mocks configurés pour FormationRepository.
 */
public class FormationRepositoryMockProvider {

    /**
     * Crée un mock avec des comportements de succès standard.
     */
    public static FormationRepository createSuccessfulMock() {
        FormationRepository mock = mock(FormationRepository.class);

        // Configuration des comportements standards
        when(mock.findById(any(UUID.class)))
                .thenReturn(Optional.of(FormationFixtures.defaultFormationPresentiel()));

        when(mock.save(any(Formation.class)))
                .thenAnswer(invocation -> {
                    Formation formation = invocation.getArgument(0);
                    if (formation.getId() == null) {
                        return formation.toBuilder().id(UUID.randomUUID()).build();
                    }
                    return formation;
                });

        when(mock.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(FormationFixtures.defaultFormationPresentiel())));

        when(mock.existsById(any(UUID.class)))
                .thenReturn(true);

        when(mock.count())
                .thenReturn(1L);

        doNothing().when(mock).deleteById(any(UUID.class));

        return mock;
    }

    /**
     * Crée un mock qui simule des entités non trouvées.
     */
    public static FormationRepository createNotFoundMock() {
        FormationRepository mock = mock(FormationRepository.class);

        when(mock.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        when(mock.existsById(any(UUID.class)))
                .thenReturn(false);

        when(mock.count())
                .thenReturn(0L);

        return mock;
    }

    /**
     * Crée un mock pour les tests de recherche.
     */
    public static FormationRepository createSearchMock() {
        FormationRepository mock = createSuccessfulMock();

        when(mock.findBySecteur(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(FormationFixtures.defaultFormationPresentiel())));

        when(mock.findByRegion(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(FormationFixtures.defaultFormationEnLigne())));

        when(mock.findByModalite(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(FormationFixtures.defaultFormationHybride())));

        return mock;
    }

    /**
     * Configure un mock existant pour retourner une formation spécifique.
     */
    public static void configureFormationById(FormationRepository mock, UUID formationId, Formation formation) {
        when(mock.findById(formationId)).thenReturn(Optional.of(formation));
        when(mock.existsById(formationId)).thenReturn(true);
    }

    /**
     * Configure un mock pour simuler une formation non trouvée.
     */
    public static void configureFormationNotFound(FormationRepository mock, UUID formationId) {
        when(mock.findById(formationId)).thenReturn(Optional.empty());
        when(mock.existsById(formationId)).thenReturn(false);
    }

    /**
     * Configure un mock pour les tests de pagination.
     */
    public static void configurePaginatedResults(FormationRepository mock, Pageable pageable, Formation... formations) {
        Page<Formation> page = new PageImpl<>(Arrays.asList(formations), pageable, formations.length);
        when(mock.findAll(pageable)).thenReturn(page);
    }

    /**
     * Vérifie les interactions standard de lecture.
     */
    public static void verifyStandardReadInteractions(FormationRepository mock, UUID formationId) {
        verify(mock).findById(formationId);
        verifyNoMoreInteractions(mock);
    }

    /**
     * Vérifie les interactions standard de sauvegarde.
     */
    public static void verifyStandardSaveInteractions(FormationRepository mock) {
        verify(mock).save(any(Formation.class));
        verifyNoMoreInteractions(mock);
    }
}