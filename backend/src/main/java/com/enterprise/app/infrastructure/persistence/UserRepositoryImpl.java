package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.User;
import com.enterprise.app.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation bridge between domain port and JPA repository.
 *
 * This class delegates calls from the domain UserRepository interface
 * to the JPA-specific JpaUserRepository implementation.
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaUserRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByKeycloakId(String keycloakId) {
        return jpaUserRepository.findByKeycloakId(keycloakId);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaUserRepository.findAll(pageable);
    }

    @Override
    public Page<User> findAllActive(Pageable pageable) {
        return jpaUserRepository.findAllActive(pageable);
    }

    @Override
    public User save(User user) {
        return jpaUserRepository.save(user);
    }

    @Override
    public void deleteById(UUID id) {
        jpaUserRepository.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaUserRepository.existsByUsername(username);
    }

    @Override
    public long count() {
        return jpaUserRepository.count();
    }

    @Override
    public long countActive() {
        return jpaUserRepository.countActive();
    }
}
