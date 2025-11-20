package com.enterprise.app.infrastructure.keycloak;

import com.enterprise.app.domain.model.Role;
import com.enterprise.app.domain.model.User;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter for Keycloak user management operations.
 *
 * This adapter handles all interactions with Keycloak Admin API
 * for creating, updating, and managing users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserAdapter {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Create user in Keycloak and return the Keycloak user ID.
     */
    public String createUser(User user, String password) {
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        UserRepresentation userRepresentation = mapToKeycloakUser(user);

        // Set password
        if (password != null && !password.isEmpty()) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);
            userRepresentation.setCredentials(Collections.singletonList(credential));
        }

        // Create user
        Response response = usersResource.create(userRepresentation);

        if (response.getStatus() != 201) {
            log.error("Failed to create user in Keycloak: {}", response.getStatusInfo());
            throw new RuntimeException("Failed to create user in Keycloak: " +
                response.getStatusInfo());
        }

        // Extract user ID from location header
        String locationHeader = response.getHeaderString("Location");
        String keycloakId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);

        // Assign roles
        assignRoles(keycloakId, user.getRoles());

        log.info("User created in Keycloak with ID: {}", keycloakId);
        return keycloakId;
    }

    /**
     * Update user in Keycloak.
     */
    public void updateUser(String keycloakId, User user) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakId);

        UserRepresentation userRepresentation = mapToKeycloakUser(user);
        userRepresentation.setId(keycloakId);

        userResource.update(userRepresentation);

        // Update roles
        assignRoles(keycloakId, user.getRoles());

        log.info("User updated in Keycloak: {}", keycloakId);
    }

    /**
     * Delete user from Keycloak.
     */
    public void deleteUser(String keycloakId) {
        RealmResource realmResource = keycloak.realm(realm);
        realmResource.users().delete(keycloakId);

        log.info("User deleted from Keycloak: {}", keycloakId);
    }

    /**
     * Assign roles to user in Keycloak.
     */
    public void assignRoles(String keycloakId, Set<Role> roles) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakId);

        // Get all realm roles
        List<RoleRepresentation> allRealmRoles = realmResource.roles().list();

        // Filter roles to assign
        List<RoleRepresentation> rolesToAssign = allRealmRoles.stream()
            .filter(role -> roles.stream()
                .anyMatch(r -> r.name().equals(role.getName())))
            .collect(Collectors.toList());

        // Remove existing roles first
        List<RoleRepresentation> existingRoles = userResource.roles().realmLevel().listAll();
        if (!existingRoles.isEmpty()) {
            userResource.roles().realmLevel().remove(existingRoles);
        }

        // Assign new roles
        if (!rolesToAssign.isEmpty()) {
            userResource.roles().realmLevel().add(rolesToAssign);
        }

        log.debug("Roles assigned to user {}: {}", keycloakId, roles);
    }

    /**
     * Reset user password in Keycloak.
     */
    public void resetPassword(String keycloakId, String newPassword) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakId);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        userResource.resetPassword(credential);

        log.info("Password reset for user: {}", keycloakId);
    }

    /**
     * Enable/disable user in Keycloak.
     */
    public void setUserEnabled(String keycloakId, boolean enabled) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakId);

        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEnabled(enabled);

        userResource.update(userRepresentation);

        log.info("User {} enabled status set to: {}", keycloakId, enabled);
    }

    /**
     * Map domain User to Keycloak UserRepresentation.
     */
    private UserRepresentation mapToKeycloakUser(User user) {
        UserRepresentation userRepresentation = new UserRepresentation();

        userRepresentation.setUsername(user.getUsername());
        userRepresentation.setEmail(user.getEmail());
        userRepresentation.setFirstName(user.getFirstName());
        userRepresentation.setLastName(user.getLastName());
        userRepresentation.setEnabled(user.isActive());
        userRepresentation.setEmailVerified(user.isEmailVerified());

        return userRepresentation;
    }
}
