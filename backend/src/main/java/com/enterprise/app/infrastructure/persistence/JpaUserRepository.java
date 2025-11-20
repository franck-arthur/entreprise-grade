package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of UserRepository (Adapter).
 *
 * This is an adapter in hexagonal architecture - concrete implementation
 * of the domain port using Spring Data JPA.
 *
 * Spring Data JPA provides automatic implementation of common methods.
 */
@Repository
public interface JpaUserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakId(String keycloakId);

    @Query("SELECT u FROM User u WHERE u.active = true")
    Page<User> findAllActive(Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActive();
}
