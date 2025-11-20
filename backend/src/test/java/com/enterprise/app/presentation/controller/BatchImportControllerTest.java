package com.enterprise.app.presentation.controller;

import com.enterprise.app.application.dto.BatchImportDetailResponse;
import com.enterprise.app.application.dto.BatchImportResponse;
import com.enterprise.app.application.mapper.BatchImportMapper;
import com.enterprise.app.application.service.BatchImportService;
import com.enterprise.app.application.usecase.UserService;
import com.enterprise.app.domain.model.BatchImport;
import com.enterprise.app.domain.model.BatchImportStatus;
import com.enterprise.app.domain.model.Role;
import com.enterprise.app.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BatchImportController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BatchImportController Tests")
class BatchImportControllerTest {

    @Mock
    private BatchImportService batchImportService;

    @Mock
    private UserService userService;

    @Mock
    private BatchImportMapper batchImportMapper;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private BatchImportController batchImportController;

    private User testUser;
    private BatchImport testBatchImport;
    private BatchImportResponse testResponse;
    private BatchImportDetailResponse testDetailResponse;
    private UUID batchImportId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        batchImportId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);

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

        testDetailResponse = BatchImportDetailResponse.builder()
            .id(batchImportId)
            .fileName("users.csv")
            .status(BatchImportStatus.PENDING)
            .build();

        // Setup authentication mock
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    }

    @Test
    @DisplayName("Should upload CSV file successfully")
    void shouldUploadCsvFile() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            "text/csv",
            "username,email,firstName,lastName\ntest,test@example.com,Test,User".getBytes()
        );

        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(batchImportService.createBatchImport(any(), eq(testUser)))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport)).thenReturn(testResponse);

        // When
        ResponseEntity<BatchImportResponse> result =
            batchImportController.uploadCsvFile(file, authentication);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(batchImportId);
        assertThat(result.getBody().getFileName()).isEqualTo("users.csv");
        assertThat(result.getBody().getStatus()).isEqualTo(BatchImportStatus.PENDING);

        verify(userService).getUserEntityByUsername("testuser");
        verify(batchImportService).createBatchImport(any(), eq(testUser));
        verify(batchImportMapper).toResponse(testBatchImport);
    }

    @Test
    @DisplayName("Should return 400 when file is empty")
    void shouldReturn400WhenFileIsEmpty() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "users.csv",
            "text/csv",
            new byte[0]
        );

        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);

        // When & Then
        assertThatThrownBy(() ->
            batchImportController.uploadCsvFile(emptyFile, authentication)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("File cannot be empty");

        verify(batchImportService, never()).createBatchImport(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when file is not CSV")
    void shouldReturn400WhenFileIsNotCsv() {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "users.txt",
            "text/plain",
            "content".getBytes()
        );

        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);

        // When & Then
        assertThatThrownBy(() ->
            batchImportController.uploadCsvFile(invalidFile, authentication)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("must be a CSV file");

        verify(batchImportService, never()).createBatchImport(any(), any());
    }

    @Test
    @DisplayName("Should get batch import by ID")
    void shouldGetBatchImportById() {
        // Given
        when(batchImportService.getBatchImportById(batchImportId))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toDetailResponse(testBatchImport))
            .thenReturn(testDetailResponse);

        // When
        ResponseEntity<BatchImportDetailResponse> result =
            batchImportController.getBatchImportById(batchImportId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();

        verify(batchImportService).getBatchImportById(batchImportId);
        verify(batchImportMapper).toDetailResponse(testBatchImport);
    }

    @Test
    @DisplayName("Should get batch import status")
    void shouldGetBatchImportStatus() {
        // Given
        when(batchImportService.getBatchImportById(batchImportId))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport))
            .thenReturn(testResponse);

        // When
        ResponseEntity<BatchImportResponse> result =
            batchImportController.getBatchImportStatus(batchImportId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(batchImportId);
        assertThat(result.getBody().getStatus()).isEqualTo(BatchImportStatus.PENDING);

        verify(batchImportService).getBatchImportById(batchImportId);
        verify(batchImportMapper).toResponse(testBatchImport);
    }

    @Test
    @DisplayName("Should get all batch imports")
    void shouldGetAllBatchImports() {
        // Given
        List<BatchImport> imports = Arrays.asList(testBatchImport);
        Page<BatchImport> page = new PageImpl<>(imports);

        when(batchImportService.getAllBatchImports(any(Pageable.class))).thenReturn(page);
        when(batchImportMapper.toResponse(any(BatchImport.class))).thenReturn(testResponse);

        // When
        ResponseEntity<Page<BatchImportResponse>> result =
            batchImportController.getAllBatchImports(pageable);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getContent().get(0).getId()).isEqualTo(batchImportId);

        verify(batchImportService).getAllBatchImports(any(Pageable.class));
    }

    @Test
    @DisplayName("Should get my batch imports")
    void shouldGetMyBatchImports() {
        // Given
        List<BatchImport> imports = Arrays.asList(testBatchImport);
        Page<BatchImport> page = new PageImpl<>(imports);

        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(batchImportService.getBatchImportsByUser(eq(testUser.getId()), any(Pageable.class)))
            .thenReturn(page);
        when(batchImportMapper.toResponse(any(BatchImport.class))).thenReturn(testResponse);

        // When
        ResponseEntity<Page<BatchImportResponse>> result =
            batchImportController.getMyBatchImports(pageable, authentication);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getContent().get(0).getId()).isEqualTo(batchImportId);

        verify(userService).getUserEntityByUsername("testuser");
        verify(batchImportService).getBatchImportsByUser(eq(testUser.getId()), any(Pageable.class));
    }

    @Test
    @DisplayName("Should cancel batch import")
    void shouldCancelBatchImport() {
        // Given
        testBatchImport.setStatus(BatchImportStatus.CANCELLED);
        testResponse.setStatus(BatchImportStatus.CANCELLED);

        doNothing().when(batchImportService).cancelBatchImport(batchImportId);
        when(batchImportService.getBatchImportById(batchImportId))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport))
            .thenReturn(testResponse);

        // When
        ResponseEntity<BatchImportResponse> result =
            batchImportController.cancelBatchImport(batchImportId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(batchImportId);
        assertThat(result.getBody().getStatus()).isEqualTo(BatchImportStatus.CANCELLED);

        verify(batchImportService).cancelBatchImport(batchImportId);
        verify(batchImportService).getBatchImportById(batchImportId);
        verify(batchImportMapper).toResponse(testBatchImport);
    }

    @Test
    @DisplayName("Should use subject when preferred_username is null")
    void shouldUseSubjectWhenPreferredUsernameIsNull() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            "text/csv",
            "username,email\ntest,test@example.com".getBytes()
        );

        when(jwt.getClaimAsString("preferred_username")).thenReturn(null);
        when(jwt.getSubject()).thenReturn("testuser");
        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(batchImportService.createBatchImport(any(), eq(testUser)))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport)).thenReturn(testResponse);

        // When
        ResponseEntity<BatchImportResponse> result =
            batchImportController.uploadCsvFile(file, authentication);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(jwt).getSubject();
        verify(userService).getUserEntityByUsername("testuser");
    }

    @Test
    @DisplayName("Should handle large file upload")
    void shouldHandleLargeFileUpload() {
        // Given
        byte[] largeContent = new byte[10000];
        Arrays.fill(largeContent, (byte) 'a');

        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large-users.csv",
            "text/csv",
            largeContent
        );

        when(userService.getUserEntityByUsername("testuser")).thenReturn(testUser);
        when(batchImportService.createBatchImport(any(), eq(testUser)))
            .thenReturn(testBatchImport);
        when(batchImportMapper.toResponse(testBatchImport)).thenReturn(testResponse);

        // When
        ResponseEntity<BatchImportResponse> result =
            batchImportController.uploadCsvFile(largeFile, authentication);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(batchImportService).createBatchImport(any(), eq(testUser));
    }
}
