package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.PresenceRequest;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.infrastructure.persistence.JpaFormationParticipationRepository;
import com.enterprise.app.infrastructure.persistence.JpaFormationRepository;
import com.enterprise.app.infrastructure.persistence.JpaUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FormationParticipationController using Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("FormationParticipationController Integration Tests")
class FormationParticipationControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaFormationRepository formationRepository;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private JpaFormationParticipationRepository participationRepository;

    private Formation formation;
    private User user;
    private UUID formationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        participationRepository.deleteAll();
        formationRepository.deleteAll();
        userRepository.deleteAll();

        formation = Formation.builder()
                .libelle("Formation Spring Boot")
                .formateurs("Expert Spring")
                .description("Formation complète sur Spring Boot")
                .dateFormation(LocalDate.now().plusDays(30))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Centre de formation")
                .ville("Paris")
                .build();

        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .password("$2a$10$encrypted")
                .active(true)
                .roles(Set.of(Role.USER))
                .build();

        Formation savedFormation = formationRepository.save(formation);
        User savedUser = userRepository.save(user);

        formationId = savedFormation.getId();
        userId = savedUser.getId();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should create inscription successfully")
    void inscrireUtilisateur_ShouldCreateInscription_WhenValidRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/formations/{formationId}/inscriptions", formationId)
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.formationId").value(formationId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.statutParticipation").value("ABSENT"));

        // Verify in database
        Optional<FormationParticipation> participation = participationRepository.findByFormationIdAndUserId(formationId, userId);
        org.assertj.core.api.Assertions.assertThat(participation).isPresent();
        org.assertj.core.api.Assertions.assertThat(participation.get().getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should prevent duplicate inscription")
    void inscrireUtilisateur_ShouldPreventDuplicate_WhenAlreadyInscribed() throws Exception {
        // Given - Create existing inscription
        FormationParticipation existingParticipation = FormationParticipation.builder()
                .formation(formationRepository.findById(formationId).orElseThrow())
                .user(userRepository.findById(userId).orElseThrow())
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .build();
        participationRepository.save(existingParticipation);

        // When & Then
        mockMvc.perform(post("/api/v1/formations/{formationId}/inscriptions", formationId)
                .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should allow self inscription")
    void inscrireUtilisateur_ShouldAllowSelfInscription() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/formations/{formationId}/inscriptions", formationId)
                .with(csrf()))
                .andExpect(status().isCreated());
    }

    // Ce test n'est plus pertinent car l'userId est maintenant récupéré automatiquement
    // Les utilisateurs ne peuvent plus inscrire d'autres utilisateurs
    @Test
    @WithMockUser(username = "otheruser", roles = "USER")
    @DisplayName("Should allow any user to self-inscribe")
    void inscrireUtilisateur_ShouldAllowUserSelfInscription() throws Exception {
        // Create otheruser in database
        User otherUser = User.builder()
                .username("otheruser")
                .email("other@example.com")
                .firstName("Other")
                .lastName("User")
                .password("$2a$10$encrypted")
                .active(true)
                .roles(Set.of(Role.USER))
                .build();
        User savedOtherUser = userRepository.save(otherUser);

        // When & Then - otheruser can self-inscribe
        mockMvc.perform(post("/api/v1/formations/{formationId}/inscriptions", formationId)
                .with(csrf()))
                .andExpect(status().isCreated());

        // Verify inscription was created for otheruser, not testuser
        Optional<FormationParticipation> participation = participationRepository.findByFormationIdAndUserId(formationId, savedOtherUser.getId());
        org.assertj.core.api.Assertions.assertThat(participation).isPresent();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Should remove inscription successfully")
    void desinscrireUtilisateur_ShouldRemoveInscription() throws Exception {
        // Given - Create existing inscription
        FormationParticipation existingParticipation = FormationParticipation.builder()
                .formation(formationRepository.findById(formationId).orElseThrow())
                .user(userRepository.findById(userId).orElseThrow())
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .build();
        participationRepository.save(existingParticipation);

        // When & Then
        mockMvc.perform(delete("/api/v1/formations/{formationId}/inscriptions", formationId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify deletion
        Optional<FormationParticipation> participation = participationRepository.findByFormationIdAndUserId(formationId, userId);
        org.assertj.core.api.Assertions.assertThat(participation).isEmpty();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should retrieve user formations")
    void getFormationsUtilisateur_ShouldReturnUserFormations() throws Exception {
        // Given - Create participation
        FormationParticipation participation = FormationParticipation.builder()
                .formation(formationRepository.findById(formationId).orElseThrow())
                .user(userRepository.findById(userId).orElseThrow())
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .build();
        participationRepository.save(participation);

        // When & Then
        mockMvc.perform(get("/api/v1/formations/mes-inscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].formationLibelle").value("Formation Spring Boot"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should allow user to view own formations")
    void getFormationsUtilisateur_ShouldAllowSelfAccess() throws Exception {
        // Given - Create participation
        FormationParticipation participation = FormationParticipation.builder()
                .formation(formationRepository.findById(formationId).orElseThrow())
                .user(userRepository.findById(userId).orElseThrow())
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .build();
        participationRepository.save(participation);

        // When & Then
        mockMvc.perform(get("/api/v1/formations/mes-inscriptions")
                .with(request -> {
                    request.setRemoteUser(userId.toString());
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should mark presence successfully")
    void marquerPresence_ShouldMarkPresence() throws Exception {
        // Given - Create ongoing formation and participation
        Formation ongoingFormation = formation.toBuilder()
                .dateFormation(LocalDate.now())
                .heureDebut(LocalTime.of(8, 0))
                .heureFin(LocalTime.of(23, 0))
                .build();
        Formation savedOngoingFormation = formationRepository.save(ongoingFormation);

        FormationParticipation participation = FormationParticipation.builder()
                .formation(savedOngoingFormation)
                .user(userRepository.findById(userId).orElseThrow())
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .build();
        participationRepository.save(participation);

        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(true);
        presenceRequest.setCommentaire("Présent toute la journée");

        // When & Then
        mockMvc.perform(put("/api/v1/formations/{formationId}/participants/{userId}/presence",
                savedOngoingFormation.getId(), userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presenceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutParticipation").value("PRESENT"))
                .andExpect(jsonPath("$.commentaire").value("Présent toute la journée"));

        // Verify in database
        Optional<FormationParticipation> updatedParticipation = participationRepository
                .findByFormationIdAndUserId(savedOngoingFormation.getId(), userId);
        org.assertj.core.api.Assertions.assertThat(updatedParticipation).isPresent();
        org.assertj.core.api.Assertions.assertThat(updatedParticipation.get().getStatutParticipation()).isEqualTo(StatutParticipation.PRESENT);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Should mark absence successfully")
    void marquerPresence_ShouldMarkAbsence() throws Exception {
        // Given - Create ongoing formation and participation
        Formation ongoingFormation = formation.toBuilder()
                .dateFormation(LocalDate.now())
                .heureDebut(LocalTime.of(8, 0))
                .heureFin(LocalTime.of(23, 0))
                .build();
        Formation savedOngoingFormation = formationRepository.save(ongoingFormation);

        FormationParticipation participation = FormationParticipation.builder()
                .formation(savedOngoingFormation)
                .user(userRepository.findById(userId).orElseThrow())
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .build();
        participationRepository.save(participation);

        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(false);
        presenceRequest.setCommentaire("Absent pour raisons personnelles");

        // When & Then
        mockMvc.perform(put("/api/v1/formations/{formationId}/participants/{userId}/presence",
                savedOngoingFormation.getId(), userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presenceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutParticipation").value("ABSENT"))
                .andExpect(jsonPath("$.commentaire").value("Absent pour raisons personnelles"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should prevent regular users from marking presence")
    void marquerPresence_ShouldPreventUserAccess() throws Exception {
        // Given
        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(true);

        // When & Then
        mockMvc.perform(put("/api/v1/formations/{formationId}/participants/{userId}/presence", formationId, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presenceRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should prevent inscription when formation is full")
    void inscrireUtilisateur_ShouldPreventInscription_WhenFormationFull() throws Exception {
        // Given - Create formation with limited capacity
        Formation limitedFormation = formation.toBuilder()
                .nbParticipants(1)
                .libelle("Formation limitée")
                .build();
        Formation savedLimitedFormation = formationRepository.save(limitedFormation);

        // Create another user and inscribe them to fill the formation
        User otherUser = User.builder()
                .username("otheruser")
                .email("other@example.com")
                .firstName("Other")
                .lastName("User")
                .password("$2a$10$encrypted")
                .active(true)
                .roles(Set.of(Role.USER))
                .build();
        User savedOtherUser = userRepository.save(otherUser);

        FormationParticipation existingParticipation = FormationParticipation.builder()
                .formation(savedLimitedFormation)
                .user(savedOtherUser)
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .build();
        participationRepository.save(existingParticipation);

        // When & Then
        mockMvc.perform(post("/api/v1/formations/{formationId}/inscriptions",
                savedLimitedFormation.getId())
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should prevent inscription to past formations")
    void inscrireUtilisateur_ShouldPreventInscription_WhenFormationPast() throws Exception {
        // Given - Create past formation
        Formation pastFormation = formation.toBuilder()
                .dateFormation(LocalDate.now().minusDays(1))
                .libelle("Formation passée")
                .build();
        Formation savedPastFormation = formationRepository.save(pastFormation);

        // When & Then
        mockMvc.perform(post("/api/v1/formations/{formationId}/inscriptions",
                savedPastFormation.getId())
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/formations/{formationId}/inscriptions", formationId)
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/formations/mes-inscriptions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/formations/{formationId}/inscriptions", formationId)
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}