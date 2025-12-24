package com.enterprise.app.testing.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Classe de base pour les tests unitaires avec Mockito.
 * Fournit une configuration commune et des utilitaires pour tous les tests unitaires.
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {

    @BeforeEach
    void resetMocks() {
        Object[] mocks = getAllMocks();
        if (mocks != null && mocks.length > 0) {
            Mockito.reset(mocks);
        }
    }

    /**
     * Retourne tous les mocks utilisés dans le test pour permettre leur reset.
     * Les classes filles doivent implémenter cette méthode.
     */
    protected abstract Object[] getAllMocks();

    /**
     * Utilitaire pour créer un ArgumentCaptor typé.
     */
    protected static <T> ArgumentCaptor<T> captor(Class<T> clazz) {
        return ArgumentCaptor.forClass(clazz);
    }

    /**
     * Vérifie qu'aucune interaction supplémentaire n'a eu lieu sur les mocks.
     */
    protected static void verifyNoMoreInteractionsOnAllMocks(Object... mocks) {
        verifyNoMoreInteractions(mocks);
    }
}