package com.enterprise.app.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Admin Client configuration.
 * Used for programmatic user management (create, update, delete users).
 */
@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakConfig {

    private String authServerUrl;
    private String realm;
    private String resource;
    private Credentials credentials;
    private Admin admin;

    @Getter
    @Setter
    public static class Credentials {
        private String secret;
    }

    @Getter
    @Setter
    public static class Admin {
        private String username;
        private String password;
        private String clientId;
    }

    /**
     * Create Keycloak admin client bean.
     */
    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
            .serverUrl(authServerUrl)
            .realm(realm)
            .grantType(OAuth2Constants.PASSWORD)
            .clientId(admin.getClientId())
            .username(admin.getUsername())
            .password(admin.getPassword())
            .build();
    }
}
