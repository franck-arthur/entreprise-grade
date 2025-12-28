package com.enterprise.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User domain entity (JPA entity).
 *
 * Represents the core business model for users in the system.
 * Uses JPA annotations for persistence but contains domain logic.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_keycloak_id", columnList = "keycloak_id", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "keycloak_id", unique = true, length = 100)
    private String keycloakId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "date_derniere_formation")
    private LocalDateTime dateDerniereFormation;

    @Version
    private Long version;

    /**
     * Business logic: Check if user has a specific role.
     */
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    /**
     * Business logic: Check if user has any of the specified roles.
     */
    public boolean hasAnyRole(Role... rolesToCheck) {
        for (Role role : rolesToCheck) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Business logic: Add a role to the user.
     */
    public void addRole(Role role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    /**
     * Business logic: Remove a role from the user.
     */
    public void removeRole(Role role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Business logic: Get user's full name.
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }

    /**
     * Business logic: Update last login timestamp.
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Business logic: Activate user account.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Business logic: Deactivate user account.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Business logic: Update last formation date when user is marked as present.
     */
    public void updateDateDerniereFormation() {
        this.dateDerniereFormation = LocalDateTime.now();
    }

    /**
     * Business logic: Reset formation date when user is marked as absent.
     */
    public void resetDateDerniereFormation() {
        this.dateDerniereFormation = null;
    }
}
