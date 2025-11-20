package com.enterprise.app.application.usecase;

import com.enterprise.app.application.dto.CreateUserRequest;
import com.enterprise.app.application.dto.UpdateUserRequest;
import com.enterprise.app.application.dto.UserDTO;
import com.enterprise.app.application.mapper.UserMapper;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.Role;
import com.enterprise.app.domain.model.User;
import com.enterprise.app.domain.repository.UserRepository;
import com.enterprise.app.infrastructure.keycloak.KeycloakUserAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakUserAdapter keycloakAdapter;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;
    private CreateUserRequest createRequest;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testUser = User.builder()
            .id(testUserId)
            .username("testuser")
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .active(true)
            .emailVerified(false)
            .roles(Set.of(Role.USER))
            .keycloakId("keycloak-123")
            .build();

        testUserDTO = UserDTO.builder()
            .id(testUserId)
            .username("testuser")
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .active(true)
            .roles(Set.of(Role.USER))
            .build();

        createRequest = CreateUserRequest.builder()
            .username("newuser")
            .email("new@example.com")
            .password("password123")
            .firstName("New")
            .lastName("User")
            .roles(Set.of(Role.USER))
            .build();
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserById() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        // When
        UserDTO result = userService.getUserById(testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getUsername()).isEqualTo("testuser");

        verify(userRepository).findById(testUserId);
        verify(userMapper).toDTO(testUser);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");

        verify(userRepository).findById(nonExistentId);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUser() {
        // Given
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(createRequest.getUsername())).thenReturn(false);
        when(userMapper.toEntity(createRequest)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(keycloakAdapter.createUser(any(User.class), anyString())).thenReturn("keycloak-123");
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        // When
        UserDTO result = userService.createUser(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");

        verify(userRepository).existsByEmail(createRequest.getEmail());
        verify(userRepository).existsByUsername(createRequest.getUsername());
        verify(keycloakAdapter).createUser(any(User.class), eq(createRequest.getPassword()));
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("email");

        verify(userRepository).existsByEmail(createRequest.getEmail());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(keycloakAdapter);
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUser() {
        // Given
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
            .firstName("Updated")
            .lastName("Name")
            .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);
        doNothing().when(userMapper).updateEntityFromDTO(updateRequest, testUser);
        doNothing().when(keycloakAdapter).updateUser(anyString(), any(User.class));

        // When
        UserDTO result = userService.updateUser(testUserId, updateRequest);

        // Then
        assertThat(result).isNotNull();

        verify(userRepository).findById(testUserId);
        verify(userMapper).updateEntityFromDTO(updateRequest, testUser);
        verify(userRepository).save(testUser);
        verify(keycloakAdapter).updateUser(eq("keycloak-123"), any(User.class));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUser() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        doNothing().when(keycloakAdapter).deleteUser(anyString());
        doNothing().when(userRepository).deleteById(testUserId);

        // When
        userService.deleteUser(testUserId);

        // Then
        verify(userRepository).findById(testUserId);
        verify(keycloakAdapter).deleteUser("keycloak-123");
        verify(userRepository).deleteById(testUserId);
    }

    @Test
    @DisplayName("Should activate user successfully")
    void shouldActivateUser() {
        // Given
        testUser.setActive(false);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);
        doNothing().when(keycloakAdapter).setUserEnabled(anyString(), eq(true));

        // When
        UserDTO result = userService.activateUser(testUserId);

        // Then
        assertThat(result).isNotNull();

        verify(userRepository).findById(testUserId);
        verify(userRepository).save(testUser);
        verify(keycloakAdapter).setUserEnabled("keycloak-123", true);
    }

    @Test
    @DisplayName("Should deactivate user successfully")
    void shouldDeactivateUser() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);
        doNothing().when(keycloakAdapter).setUserEnabled(anyString(), eq(false));

        // When
        UserDTO result = userService.deactivateUser(testUserId);

        // Then
        assertThat(result).isNotNull();

        verify(userRepository).findById(testUserId);
        verify(userRepository).save(testUser);
        verify(keycloakAdapter).setUserEnabled("keycloak-123", false);
    }
}
