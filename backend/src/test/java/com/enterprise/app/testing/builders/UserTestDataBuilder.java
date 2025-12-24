package com.enterprise.app.testing.builders;

import com.enterprise.app.domain.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builder pour créer des instances de User personnalisées pour les tests.
 */
public class UserTestDataBuilder {

    private UUID id = UUID.randomUUID();
    private String username = "testuser";
    private String email = "test@example.com";
    private String firstName = "Test";
    private String lastName = "User";
    private String nom = "User";
    private String prenom = "Test";
    private boolean active = true;
    private LocalDateTime dateDerniereFormation;

    /**
     * Point d'entrée pour créer un builder.
     */
    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }

    /**
     * Définit l'ID de l'utilisateur.
     */
    public UserTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Définit le nom d'utilisateur.
     */
    public UserTestDataBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * Définit l'email.
     */
    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    /**
     * Définit le prénom.
     */
    public UserTestDataBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        this.prenom = firstName;
        return this;
    }

    /**
     * Définit le nom.
     */
    public UserTestDataBuilder withLastName(String lastName) {
        this.lastName = lastName;
        this.nom = lastName;
        return this;
    }

    /**
     * Définit le statut actif.
     */
    public UserTestDataBuilder withActive(boolean active) {
        this.active = active;
        return this;
    }

    /**
     * Définit la date de dernière formation.
     */
    public UserTestDataBuilder withDateDerniereFormation(LocalDateTime dateDerniereFormation) {
        this.dateDerniereFormation = dateDerniereFormation;
        return this;
    }

    // Méthodes de convenance

    /**
     * Configure un utilisateur administrateur.
     */
    public UserTestDataBuilder admin() {
        return withUsername("admin")
                .withEmail("admin@example.com")
                .withFirstName("Admin")
                .withLastName("System");
    }

    /**
     * Configure un utilisateur manager.
     */
    public UserTestDataBuilder manager() {
        return withUsername("manager")
                .withEmail("manager@example.com")
                .withFirstName("Manager")
                .withLastName("Lead");
    }

    /**
     * Configure un utilisateur inactif.
     */
    public UserTestDataBuilder inactive() {
        return withActive(false);
    }

    /**
     * Configure un utilisateur avec une formation récente.
     */
    public UserTestDataBuilder withRecentFormation() {
        return withDateDerniereFormation(LocalDateTime.now().minusDays(7));
    }

    /**
     * Configure un utilisateur avec une formation ancienne.
     */
    public UserTestDataBuilder withOldFormation() {
        return withDateDerniereFormation(LocalDateTime.now().minusMonths(6));
    }

    /**
     * Construit l'instance User.
     */
    public User build() {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .nom(nom)
                .prenom(prenom)
                .active(active)
                .dateDerniereFormation(dateDerniereFormation)
                .build();
    }
}