package com.enterprise.app.testing.fixtures;

import com.enterprise.app.domain.model.User;
import org.instancio.Instancio;
import static org.instancio.Select.field;

import java.time.LocalDateTime;

/**
 * Fixtures centralisées pour les données de test User.
 */
public final class UserFixtures {

    private UserFixtures() {}

    // IDs constants pour les tests
    public static final Long USER_ID_1 = 1L;
    public static final Long USER_ID_2 = 2L;
    public static final Long USER_ID_3 = 3L;

    // Usernames constants
    public static final String USERNAME_TESTUSER = "testuser";
    public static final String USERNAME_ADMIN = "admin";
    public static final String USERNAME_MANAGER = "manager";

    /**
     * Utilisateur standard pour les tests.
     */
    public static User defaultUser() {
        return Instancio.of(User.class)
                .set(field(User::getId), USER_ID_1)
                .set(field(User::getUsername), USERNAME_TESTUSER)
                .set(field(User::getEmail), "testuser@example.com")
                .set(field(User::getFirstName), "Test")
                .set(field(User::getLastName), "User")
                .set(field(User::isActive), true)
                .set(field(User::getDateDerniereFormation), null)
                .create();
    }

    /**
     * Utilisateur administrateur.
     */
    public static User adminUser() {
        return Instancio.of(User.class)
                .set(field(User::getId), USER_ID_2)
                .set(field(User::getUsername), USERNAME_ADMIN)
                .set(field(User::getEmail), "admin@example.com")
                .set(field(User::getFirstName), "Admin")
                .set(field(User::getLastName), "System")
                .set(field(User::isActive), true)
                .set(field(User::getDateDerniereFormation), null)
                .create();
    }

    /**
     * Utilisateur manager.
     */
    public static User managerUser() {
        return Instancio.of(User.class)
                .set(field(User::getId), USER_ID_3)
                .set(field(User::getUsername), USERNAME_MANAGER)
                .set(field(User::getEmail), "manager@example.com")
                .set(field(User::getFirstName), "Manager")
                .set(field(User::getLastName), "Lead")
                .set(field(User::isActive), true)
                .set(field(User::getDateDerniereFormation), null)
                .create();
    }

    /**
     * Utilisateur inactif.
     */
    public static User inactiveUser() {
        return Instancio.of(User.class)
                .set(field(User::getId), USER_ID_1)
                .set(field(User::getUsername), USERNAME_TESTUSER)
                .set(field(User::getEmail), "testuser@example.com")
                .set(field(User::getFirstName), "Test")
                .set(field(User::getLastName), "User")
                .set(field(User::isActive), false)
                .set(field(User::getDateDerniereFormation), null)
                .create();
    }

    /**
     * Utilisateur avec dernière formation.
     */
    public static User userWithLastFormation() {
        return Instancio.of(User.class)
                .set(field(User::getId), USER_ID_1)
                .set(field(User::getUsername), USERNAME_TESTUSER)
                .set(field(User::getEmail), "testuser@example.com")
                .set(field(User::getFirstName), "Test")
                .set(field(User::getLastName), "User")
                .set(field(User::isActive), true)
                .set(field(User::getDateDerniereFormation), LocalDateTime.now().minusDays(10))
                .create();
    }

    /**
     * Génère un utilisateur aléatoire avec Instancio.
     */
    public static User randomUser() {
        return Instancio.of(User.class)
                .set(field(User::isActive), true)
                .create();
    }

    /**
     * Génère un utilisateur aléatoire avec ID spécifique.
     */
    public static User randomUserWithId(Long id) {
        return Instancio.of(User.class)
                .set(field(User::getId), id)
                .set(field(User::isActive), true)
                .create();
    }

    /**
     * Participant Joelle DUPRES - Secrétariat.
     */
    public static User joelleDupres() {
        return Instancio.of(User.class)
                .set(field(User::getId), 10L)
                .set(field(User::getEmail), "Joelle.dupres@secretariat.com")
                .set(field(User::getFirstName), "Joelle")
                .set(field(User::getLastName), "DUPRES")
                .set(field(User::getPhoneNumber), "0600000001")
                .set(field(User::getDateDerniereFormation), LocalDateTime.of(2017, 10, 6, 0, 0))
                .set(field(User::getUsername), "joelle.dupres")
                .set(field(User::isActive), true)
                .create();
    }

    /**
     * Participant Nicolas MOREL - Infirmier.
     */
    public static User nicolasMorel() {
        return Instancio.of(User.class)
                .set(field(User::getId), 11L)
                .set(field(User::getEmail), "Aline.leclaire@infirmier.com")
                .set(field(User::getFirstName), "Nicolas")
                .set(field(User::getLastName), "MOREL")
                .set(field(User::getPhoneNumber), "0600000001")
                .set(field(User::getDateDerniereFormation), null)
                .set(field(User::getUsername), "nicolas.morel")
                .set(field(User::isActive), true)
                .create();
    }

    /**
     * Participant Clément GOS.
     */
    public static User clementGos() {
        return Instancio.of(User.class)
                .set(field(User::getId), 12L)
                .set(field(User::getEmail), "goclement@gmail.com")
                .set(field(User::getFirstName), "Clement")
                .set(field(User::getLastName), "GOS")
                .set(field(User::getPhoneNumber), "0600000001")
                .set(field(User::getDateDerniereFormation), LocalDateTime.of(2025, 10, 6, 0, 0))
                .set(field(User::getUsername), "clement.gos")
                .set(field(User::isActive), true)
                .create();
    }
}