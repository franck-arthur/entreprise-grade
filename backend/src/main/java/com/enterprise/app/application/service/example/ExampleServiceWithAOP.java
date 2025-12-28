package com.enterprise.app.application.service.example;

import com.enterprise.app.domain.model.User;
import com.enterprise.app.infrastructure.logging.BusinessAuditLogger;
import com.enterprise.app.infrastructure.logging.TechnicalLogging;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exemple de service utilisant l'approche hybride optimale :
 * - AOP (@TechnicalLogging) pour les logs techniques automatiques
 * - BusinessAuditLogger pour la traçabilité métier explicite
 *
 * Avantages :
 * 1. Code métier pur sans pollution de logs techniques
 * 2. Logging technique automatique et configurable
 * 3. Traçabilité métier explicite et structurée
 */
@Service
@RequiredArgsConstructor
@TechnicalLogging(  // Logs techniques automatiques pour toute la classe
    entryLevel = TechnicalLogging.LogLevel.DEBUG,
    exitLevel = TechnicalLogging.LogLevel.DEBUG,
    includeExecutionTime = true,
    prefix = "USER_SERVICE"
)
public class ExampleServiceWithAOP {

    private final BusinessAuditLogger businessAuditLogger;

    /**
     * Création d'utilisateur avec approche hybride.
     * AOP gère les logs techniques, BusinessAuditLogger la traçabilité.
     */
    public User createUser(String username, String email, List<String> roles) {
        // Code métier pur - AUCUN log technique manuel !
        validateUserData(username, email);

        User user = User.builder()
            .id(System.currentTimeMillis()) // Generating a Long ID
            .username(username)
            .email(email)
            .build();

        // Seul le log métier est explicite pour la traçabilité
        businessAuditLogger.logBusinessAction(
            getCurrentUserId(),
            getCurrentUsername(),
            "USER_CREATION",
            "User",
            user.getId().toString(),
            String.format("Created user %s with email %s", username, email)
        );

        return user;
        // L'AOP génère automatiquement les logs techniques :
        // [abc123] → USER_SERVICE | HybridLoggingService.createUser(username=john, email=john@example.com, roles=[USER])
        // [abc123] ← USER_SERVICE | HybridLoggingService.createUser → User{id=..., username=john} (took 45ms)
    }

    /**
     * Opération sensible avec masquage automatique des paramètres.
     */
    @TechnicalLogging(
        entryLevel = TechnicalLogging.LogLevel.INFO,
        exitLevel = TechnicalLogging.LogLevel.INFO,
        includeArgs = false,  // Masquer les mots de passe
        includeResult = true,
        prefix = "SECURITY"
    )
    public boolean updateUserPassword(Long userId, String oldPassword, String newPassword) {
        // Logique métier pure
        User user = findUserById(userId);
        validatePassword(oldPassword, user);
        //user.updatePassword(newPassword);

        // Log métier pour traçabilité sécuritaire
        businessAuditLogger.logSecurityEvent(
            "PASSWORD_CHANGE",
            user.getUsername(),
            getCurrentUserIp(),
            true,
            "Password successfully updated"
        );

        return true;
        // AOP génère : [xyz] → SECURITY | updateUserPassword() sans les paramètres sensibles
    }

    /**
     * Logging conditionnel avec SpEL.
     */
    @TechnicalLogging(
        condition = "#roles.size() > 1",  // Log seulement si assignation multiple
        entryLevel = TechnicalLogging.LogLevel.WARN,
        prefix = "MULTI_ROLE"
    )
    public void assignRoles(Long userId, List<String> roles) {
        User user = findUserById(userId);
        //user.setRoles(roles);

        // Traçabilité métier obligatoire
        businessAuditLogger.logBusinessAction(
            getCurrentUserId(),
            getCurrentUsername(),
            "ROLE_ASSIGNMENT",
            "User",
            userId.toString(),
            "Assigned roles: " + String.join(", ", roles)
        );
    }

    /**
     * Méthode publique sans logging technique.
     */
    @TechnicalLogging(
        entryLevel = TechnicalLogging.LogLevel.OFF,
        exitLevel = TechnicalLogging.LogLevel.OFF,
        errorLevel = TechnicalLogging.LogLevel.OFF
    )
    public List<User> getPublicUserList() {
        return findAllActiveUsers();  // Pas de logs techniques pour méthode publique
    }

    /**
     * Gestion d'erreur avec double logging.
     */
    public void deleteUser(Long userId) {
        try {
            User user = findUserById(userId);
            performUserDeletion(user);

            // Traçabilité métier du succès
            businessAuditLogger.logBusinessAction(
                getCurrentUserId(),
                getCurrentUsername(),
                "USER_DELETION",
                "User",
                userId.toString(),
                "User deleted: " + user.getUsername()
            );

        } catch (Exception ex) {
            // Traçabilité métier de l'erreur
            businessAuditLogger.logBusinessError(
                "USER_DELETION_FAILED",
                "DELETE_USER_OPERATION",
                ex.getMessage(),
                getCurrentUsername()
            );

            throw ex;  // AOP gère automatiquement l'erreur technique
        }
    }

    // Méthodes utilitaires (simulées)
    private void validateUserData(String username, String email) {
        // Validation...
    }

    private User findUserById(Long userId) {
        // Recherche...
        return User.builder().id(userId).username("example").build();
    }

    private void validatePassword(String password, User user) {
        // Validation...
    }

    private List<User> findAllActiveUsers() {
        // Recherche...
        return List.of();
    }

    private void performUserDeletion(User user) {
        // Suppression...
    }

    private String getCurrentUserId() {
        return "current-user-id";
    }

    private String getCurrentUsername() {
        return "current-user";
    }

    private String getCurrentUserIp() {
        return "192.168.1.100";
    }
}