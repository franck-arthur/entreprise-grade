package com.enterprise.app.domain.port;

import com.enterprise.app.domain.model.Role;
import com.enterprise.app.domain.model.User;

import java.util.Set;

/**
 * Port interface for external user management operations (e.g., Keycloak).
 * Follows hexagonal architecture pattern.
 *
 * This port defines the contract for managing users in external identity providers
 * without coupling the domain to any specific implementation.
 */
public interface ExternalUserManagementPort {

    /**
     * Create user in external identity provider and return the external user ID.
     *
     * @param user The user to create
     * @param password The initial password
     * @return The external user ID (e.g., Keycloak ID)
     */
    String createUser(User user, String password);

    /**
     * Update user in external identity provider.
     *
     * @param externalUserId The external user ID
     * @param user The updated user data
     */
    void updateUser(String externalUserId, User user);

    /**
     * Delete user from external identity provider.
     *
     * @param externalUserId The external user ID
     */
    void deleteUser(String externalUserId);

    /**
     * Assign roles to user in external identity provider.
     *
     * @param externalUserId The external user ID
     * @param roles The roles to assign
     */
    void assignRoles(String externalUserId, Set<Role> roles);

    /**
     * Reset user password in external identity provider.
     *
     * @param externalUserId The external user ID
     * @param newPassword The new password
     */
    void resetPassword(String externalUserId, String newPassword);

    /**
     * Enable or disable user in external identity provider.
     *
     * @param externalUserId The external user ID
     * @param enabled True to enable, false to disable
     */
    void setUserEnabled(String externalUserId, boolean enabled);
}
