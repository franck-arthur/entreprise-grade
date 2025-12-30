package com.enterprise.app.presentation.controller.v1;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Configuration de test minimale pour les tests de contrôleurs.
 * N'inclut aucun scan automatique pour éviter les dépendances non nécessaires.
 */
@SpringBootApplication
@Import(TestSecurityConfig.class)
public class TestWebMvcConfiguration {
}