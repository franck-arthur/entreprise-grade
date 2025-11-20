package com.enterprise.app.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom JWT converter to extract Keycloak roles from token claims.
 *
 * Keycloak stores roles in nested structure:
 * {
 *   "realm_access": {
 *     "roles": ["USER", "ADMIN"]
 *   },
 *   "resource_access": {
 *     "backend-client": {
 *       "roles": ["manage-users"]
 *     }
 *   }
 * }
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Extract realm roles
        Collection<String> realmRoles = extractRealmRoles(jwt);
        authorities.addAll(realmRoles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList()));

        // Extract resource/client roles
        Collection<String> resourceRoles = extractResourceRoles(jwt);
        authorities.addAll(resourceRoles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList()));

        return authorities;
    }

    /**
     * Extract roles from realm_access.roles claim.
     */
    @SuppressWarnings("unchecked")
    private Collection<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || realmAccess.isEmpty()) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");
        if (roles instanceof Collection) {
            return (Collection<String>) roles;
        }

        return List.of();
    }

    /**
     * Extract roles from resource_access.<client>.roles claim.
     */
    @SuppressWarnings("unchecked")
    private Collection<String> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return List.of();
        }

        List<String> allResourceRoles = new ArrayList<>();

        // Iterate through all clients/resources
        resourceAccess.values().forEach(resource -> {
            if (resource instanceof Map) {
                Map<String, Object> resourceMap = (Map<String, Object>) resource;
                Object roles = resourceMap.get("roles");

                if (roles instanceof Collection) {
                    allResourceRoles.addAll((Collection<String>) roles);
                }
            }
        });

        return allResourceRoles;
    }
}
