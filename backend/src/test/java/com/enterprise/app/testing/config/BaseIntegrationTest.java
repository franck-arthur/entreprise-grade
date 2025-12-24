package com.enterprise.app.testing.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Classe de base pour les tests d'intégration avec base de données.
 * Utilise Testcontainers pour fournir une base PostgreSQL isolée.
 */
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @BeforeEach
    void cleanDatabase() {
        // Nettoyage de la base de données avant chaque test
        cleanupDatabase();
    }

    /**
     * Méthode à implémenter par les classes filles pour nettoyer leurs données spécifiques.
     */
    protected abstract void cleanupDatabase();

    /**
     * Vérifie que la base de données est accessible.
     */
    protected void assertDatabaseIsRunning() {
        assert postgres.isRunning() : "La base de données de test n'est pas accessible";
    }
}