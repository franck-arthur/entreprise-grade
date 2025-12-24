package com.enterprise.app.testing.fixtures;

import com.enterprise.app.domain.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fixtures centralisées pour les données de test User.
 */
public final class UserFixtures {

    private UserFixtures() {}

    // IDs constants pour les tests
    public static final UUID USER_ID_1 = UUID.fromString("456e7890-e89b-12d3-a456-426614174001");
    public static final UUID USER_ID_2 = UUID.fromString("456e7890-e89b-12d3-a456-426614174002");
    public static final UUID USER_ID_3 = UUID.fromString("456e7890-e89b-12d3-a456-426614174003");

    // Usernames constants
    public static final String USERNAME_TESTUSER = "testuser";
    public static final String USERNAME_ADMIN = "admin";
    public static final String USERNAME_MANAGER = "manager";

    /**
     * Utilisateur standard pour les tests.
     */
    public static User defaultUser() {
        return User.builder()
                .id(USER_ID_1)
                .username(USERNAME_TESTUSER)
                .email("testuser@example.com")
                .firstName("Test")
                .lastName("User")
                .nom("User")
                .prenom("Test")
                .active(true)
                .build();
    }

    /**
     * Utilisateur administrateur.
     */
    public static User adminUser() {
        return User.builder()
                .id(USER_ID_2)
                .username(USERNAME_ADMIN)
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("System")
                .nom("System")
                .prenom("Admin")
                .active(true)
                .build();
    }

    /**
     * Utilisateur manager.
     */
    public static User managerUser() {
        return User.builder()
                .id(USER_ID_3)
                .username(USERNAME_MANAGER)
                .email("manager@example.com")
                .firstName("Manager")
                .lastName("Lead")
                .nom("Lead")
                .prenom("Manager")
                .active(true)
                .build();
    }

    /**
     * Utilisateur inactif.
     */
    public static User inactiveUser() {
        return defaultUser().toBuilder()
                .active(false)
                .build();
    }

    /**
     * Utilisateur avec dernière formation.
     */
    public static User userWithLastFormation() {
        return defaultUser().toBuilder()
                .dateDerniereFormation(LocalDateTime.now().minusDays(10))
                .build();
    }
}