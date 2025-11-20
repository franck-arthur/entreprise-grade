package com.enterprise.app.presentation.controller;

import com.enterprise.app.application.dto.BatchImportResponse;
import com.enterprise.app.application.mapper.BatchImportMapper;
import com.enterprise.app.application.service.BatchImportService;
import com.enterprise.app.application.usecase.UserService;
import com.enterprise.app.domain.model.BatchImport;
import com.enterprise.app.domain.model.BatchImportStatus;
import com.enterprise.app.domain.model.Role;
import com.enterprise.app.domain.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for BatchImportController.
 */
@WebMvcTest(BatchImportController.class)
@DisplayName("BatchImportController Tests")
class BatchImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatchImportService batchImportService;

    @MockBean
    private UserService userService;

    @MockBean
    private BatchImportMapper batchImportMapper;

    private User testUser;
    private BatchImport testBatchImport;
    private BatchImportResponse testResponse;
    private UUID batchImportId;

    @BeforeEach
    void setUp() {
        batchImportId = UUID.randomUUID();

        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .active(true)
            .roles(Set.of(Role.ADMIN))
            .build();

        testBatchImport = BatchImport.builder()
            .id(batchImportId)
            .fileName("users.csv")
            .fileSize(1024L)
            .status(BatchImportStatus.PENDING)
            .totalLines(10)
            .processedLines(0)
            .successLines(0)
            .failedLines(0)
            .initiatedBy(testUser)
            .createdAt(LocalDateTime.now())
            .build();

        testResponse = BatchImportResponse.builder()
            .id(batchImportId)
            .fileName("users.csv")
            .fileSize(1024L)
            .status(BatchImportStatus.PENDING)
            .totalLines(10)
            .processedLines(0)
            .successLines(0)
            .failedLines(0)
            .progressPercentage(0.0)
            .initiatedByUserId(testUser.getId())
            .initiatedByUsername(testUser.getUsername())
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("Should upload CSV file successfully")
    void shouldUploadCsvFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            MediaType.TEXT_PLAIN_VALUE,
            "username,email,firstName,lastName\ntest,test@example.com,Test,User".getBytes()
        );

        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(batchImportService.createBatchImport(any(), eq(testUser)))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport)).thenReturn(testResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/batch-imports/upload")
                .file(file)
                .with(jwt().jwt(jwt -> jwt
                    .claim("preferred_username", "testuser")
                    .subject("testuser"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(batchImportId.toString()))
            .andExpect(jsonPath("$.fileName").value("users.csv"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(userService).getUserEntityByUsername("testuser");
        verify(batchImportService).createBatchImport(any(), eq(testUser));
        verify(batchImportMapper).toResponse(testBatchImport);
    }

    @Test
    @DisplayName("Should return 400 when file is empty")
    void shouldReturn400WhenFileIsEmpty() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "users.csv",
            MediaType.TEXT_PLAIN_VALUE,
            new byte[0]
        );

        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);

        // When & Then
        mockMvc.perform(multipart("/api/v1/batch-imports/upload")
                .file(emptyFile)
                .with(jwt().jwt(jwt -> jwt
                    .claim("preferred_username", "testuser")
                    .subject("testuser"))))
            .andExpect(status().isBadRequest());

        verify(batchImportService, never()).createBatchImport(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when file is not CSV")
    void shouldReturn400WhenFileIsNotCsv() throws Exception {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "users.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "some content".getBytes()
        );

        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);

        // When & Then
        mockMvc.perform(multipart("/api/v1/batch-imports/upload")
                .file(invalidFile)
                .with(jwt().jwt(jwt -> jwt
                    .claim("preferred_username", "testuser")
                    .subject("testuser"))))
            .andExpect(status().isBadRequest());

        verify(batchImportService, never()).createBatchImport(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get batch import by ID")
    void shouldGetBatchImportById() throws Exception {
        // Given
        when(batchImportService.getBatchImportById(batchImportId))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toDetailResponse(testBatchImport))
            .thenReturn(null); // DetailResponse would be created here

        // When & Then
        mockMvc.perform(get("/api/v1/batch-imports/{id}", batchImportId))
            .andExpect(status().isOk());

        verify(batchImportService).getBatchImportById(batchImportId);
        verify(batchImportMapper).toDetailResponse(testBatchImport);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get batch import status")
    void shouldGetBatchImportStatus() throws Exception {
        // Given
        when(batchImportService.getBatchImportById(batchImportId))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport))
            .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/batch-imports/{id}/status", batchImportId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(batchImportId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(batchImportService).getBatchImportById(batchImportId);
        verify(batchImportMapper).toResponse(testBatchImport);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all batch imports")
    void shouldGetAllBatchImports() throws Exception {
        // Given
        List<BatchImport> imports = Arrays.asList(testBatchImport);
        Page<BatchImport> page = new PageImpl<>(imports);

        when(batchImportService.getAllBatchImports(any(Pageable.class))).thenReturn(page);
        when(batchImportMapper.toResponse(any(BatchImport.class))).thenReturn(testResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/batch-imports")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(batchImportId.toString()));

        verify(batchImportService).getAllBatchImports(any(Pageable.class));
    }

    @Test
    @DisplayName("Should get my batch imports")
    void shouldGetMyBatchImports() throws Exception {
        // Given
        List<BatchImport> imports = Arrays.asList(testBatchImport);
        Page<BatchImport> page = new PageImpl<>(imports);

        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(batchImportService.getBatchImportsByUser(eq(testUser.getId()), any(Pageable.class)))
            .thenReturn(page);
        when(batchImportMapper.toResponse(any(BatchImport.class))).thenReturn(testResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/batch-imports/my-imports")
                .with(jwt().jwt(jwt -> jwt
                    .claim("preferred_username", "testuser")
                    .subject("testuser"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(batchImportId.toString()));

        verify(userService).getUserEntityByUsername("testuser");
        verify(batchImportService).getBatchImportsByUser(eq(testUser.getId()), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should cancel batch import")
    void shouldCancelBatchImport() throws Exception {
        // Given
        testBatchImport.setStatus(BatchImportStatus.CANCELLED);
        testResponse.setStatus(BatchImportStatus.CANCELLED);

        doNothing().when(batchImportService).cancelBatchImport(batchImportId);
        when(batchImportService.getBatchImportById(batchImportId))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport))
            .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/batch-imports/{id}/cancel", batchImportId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(batchImportId.toString()))
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(batchImportService).cancelBatchImport(batchImportId);
        verify(batchImportService).getBatchImportById(batchImportId);
        verify(batchImportMapper).toResponse(testBatchImport);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when user without admin role tries to upload")
    void shouldReturn403ForNonAdmin() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            MediaType.TEXT_PLAIN_VALUE,
            "content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/v1/batch-imports/upload")
                .file(file))
            .andExpect(status().isForbidden());

        verify(batchImportService, never()).createBatchImport(any(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Should allow MANAGER to upload CSV")
    void shouldAllowManagerToUpload() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            MediaType.TEXT_PLAIN_VALUE,
            "username,email,firstName,lastName\ntest,test@example.com,Test,User".getBytes()
        );

        when(userService.getUserEntityByUsername("manager")).thenReturn(testUser);
        when(batchImportService.createBatchImport(any(), any()))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport)).thenReturn(testResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/batch-imports/upload")
                .file(file)
                .with(jwt().jwt(jwt -> jwt
                    .claim("preferred_username", "manager")
                    .subject("manager"))))
            .andExpect(status().isCreated());

        verify(batchImportService).createBatchImport(any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when USER tries to cancel import")
    void shouldReturn403WhenUserTriesToCancel() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/batch-imports/{id}/cancel", batchImportId))
            .andExpect(status().isForbidden());

        verify(batchImportService, never()).cancelBatchImport(any());
    }

    @Test
    @WithMockUser(roles = "TECH_LEAD")
    @DisplayName("Should allow TECH_LEAD to cancel import")
    void shouldAllowTechLeadToCancel() throws Exception {
        // Given
        testBatchImport.setStatus(BatchImportStatus.CANCELLED);
        testResponse.setStatus(BatchImportStatus.CANCELLED);

        doNothing().when(batchImportService).cancelBatchImport(batchImportId);
        when(batchImportService.getBatchImportById(batchImportId))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport))
            .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/batch-imports/{id}/cancel", batchImportId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(batchImportService).cancelBatchImport(batchImportId);
    }
}
