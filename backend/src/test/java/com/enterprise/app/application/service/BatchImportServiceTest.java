package com.enterprise.app.application.service;

import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.domain.port.BatchImportPort;
import com.enterprise.app.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.util.*;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BatchImportService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BatchImportService Tests")
class BatchImportServiceTest {

    @Mock
    private BatchImportPort batchImportPort;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageSource messageSource;

    @Mock
    private Executor csvProcessorExecutor;

    private BatchImportService batchImportService;

    private User testUser;
    private BatchImport testBatchImport;
    private UUID batchImportId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        batchImportId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);

        // Create a spy to prevent async method execution in tests
        batchImportService = spy(new BatchImportService(
            batchImportPort, userRepository, messageSource, csvProcessorExecutor
        ));

        // Stub the async method to do nothing (prevent actual async execution)
        // Use lenient() to avoid UnnecessaryStubbingException for tests that don't call createBatchImport
        lenient().doNothing().when(batchImportService).processFileAsync(any(UUID.class), any());

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
            .build();
    }

    @AfterEach
    void tearDown() {
        // Reset all mocks and clear any state to ensure clean JVM shutdown
        reset(batchImportPort, userRepository, messageSource, csvProcessorExecutor);
    }

    @Test
    @DisplayName("Should create batch import successfully")
    void shouldCreateBatchImport() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            "text/csv",
            "username,email,firstName,lastName\ntest,test@example.com,Test,User".getBytes()
        );

        when(batchImportPort.save(any(BatchImport.class))).thenReturn(testBatchImport);

        // When
        BatchImport result = batchImportService.createBatchImport(file, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(batchImportId);
        assertThat(result.getFileName()).isEqualTo("users.csv");
        assertThat(result.getStatus()).isEqualTo(BatchImportStatus.PENDING);
        assertThat(result.getInitiatedBy()).isEqualTo(testUser);

        ArgumentCaptor<BatchImport> captor = ArgumentCaptor.forClass(BatchImport.class);
        verify(batchImportPort).save(captor.capture());

        BatchImport capturedImport = captor.getValue();
        assertThat(capturedImport.getFileName()).isEqualTo("users.csv");
        assertThat(capturedImport.getFileSize()).isEqualTo(file.getSize());
        assertThat(capturedImport.getInitiatedBy()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should get batch import by ID successfully")
    void shouldGetBatchImportById() {
        // Given
        when(batchImportPort.findById(batchImportId)).thenReturn(Optional.of(testBatchImport));

        // When
        BatchImport result = batchImportService.getBatchImportById(batchImportId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(batchImportId);
        assertThat(result.getFileName()).isEqualTo("users.csv");

        verify(batchImportPort).findById(batchImportId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when batch import not found")
    void shouldThrowExceptionWhenBatchImportNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(batchImportPort.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> batchImportService.getBatchImportById(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("BatchImport")
            .hasMessageContaining(nonExistentId.toString());

        verify(batchImportPort).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should get all batch imports with pagination")
    void shouldGetAllBatchImports() {
        // Given
        List<BatchImport> imports = Arrays.asList(testBatchImport);
        Page<BatchImport> page = new PageImpl<>(imports, pageable, 1);

        when(batchImportPort.findAll(pageable)).thenReturn(page);

        // When
        Page<BatchImport> result = batchImportService.getAllBatchImports(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(batchImportId);

        verify(batchImportPort).findAll(pageable);
    }

    @Test
    @DisplayName("Should get batch imports by user")
    void shouldGetBatchImportsByUser() {
        // Given
        List<BatchImport> imports = Arrays.asList(testBatchImport);
        Page<BatchImport> page = new PageImpl<>(imports, pageable, 1);

        when(batchImportPort.findByInitiatedByUserId(testUser.getId(), pageable))
            .thenReturn(page);

        // When
        Page<BatchImport> result = batchImportService.getBatchImportsByUser(
            testUser.getId(), pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInitiatedBy()).isEqualTo(testUser);

        verify(batchImportPort).findByInitiatedByUserId(testUser.getId(), pageable);
    }

    @Test
    @DisplayName("Should cancel batch import when in progress")
    void shouldCancelBatchImportWhenInProgress() {
        // Given
        testBatchImport.setStatus(BatchImportStatus.PROCESSING);

        when(batchImportPort.findById(batchImportId)).thenReturn(Optional.of(testBatchImport));
        when(batchImportPort.save(any(BatchImport.class))).thenReturn(testBatchImport);

        // When
        batchImportService.cancelBatchImport(batchImportId);

        // Then
        verify(batchImportPort).findById(batchImportId);
        verify(batchImportPort).save(testBatchImport);
        assertThat(testBatchImport.getStatus()).isEqualTo(BatchImportStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should not cancel batch import when already completed")
    void shouldNotCancelBatchImportWhenCompleted() {
        // Given
        testBatchImport.setStatus(BatchImportStatus.COMPLETED);

        when(batchImportPort.findById(batchImportId)).thenReturn(Optional.of(testBatchImport));

        // When
        batchImportService.cancelBatchImport(batchImportId);

        // Then
        verify(batchImportPort).findById(batchImportId);
        verify(batchImportPort, never()).save(any());
        assertThat(testBatchImport.getStatus()).isEqualTo(BatchImportStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should fail batch import with error message")
    void shouldFailBatchImport() {
        // Given
        String errorMessage = "File processing failed";

        when(batchImportPort.findById(batchImportId)).thenReturn(Optional.of(testBatchImport));
        when(batchImportPort.save(any(BatchImport.class))).thenReturn(testBatchImport);

        // When
        batchImportService.failBatchImport(batchImportId, errorMessage);

        // Then
        verify(batchImportPort).findById(batchImportId);
        verify(batchImportPort).save(testBatchImport);
        assertThat(testBatchImport.getStatus()).isEqualTo(BatchImportStatus.FAILED);
        assertThat(testBatchImport.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("Should update batch import results with processed lines")
    void shouldUpdateBatchImportResults() {
        // Given
        User createdUser1 = User.builder().id(UUID.randomUUID()).username("user1").build();
        User createdUser2 = User.builder().id(UUID.randomUUID()).username("user2").build();

        BatchImportLine successLine = BatchImportLine.builder()
            .lineNumber(1)
            .rawData("user1,user1@example.com,User,One")
            .success(true)
            .createdUser(createdUser1)
            .build();

        BatchImportLine failedLine = BatchImportLine.builder()
            .lineNumber(2)
            .rawData("user2,invalid-email,User,Two")
            .success(false)
            .errorMessage("Invalid email format")
            .build();

        List<BatchImportLine> lines = Arrays.asList(successLine, failedLine);

        when(batchImportPort.findById(batchImportId)).thenReturn(Optional.of(testBatchImport));
        when(batchImportPort.save(any(BatchImport.class))).thenReturn(testBatchImport);

        // When
        batchImportService.updateBatchImportResults(batchImportId, lines);

        // Then
        verify(batchImportPort).findById(batchImportId);
        verify(batchImportPort).save(testBatchImport);

        assertThat(testBatchImport.getSuccessLines()).isEqualTo(1);
        assertThat(testBatchImport.getFailedLines()).isEqualTo(1);
        assertThat(testBatchImport.getStatus()).isEqualTo(BatchImportStatus.COMPLETED_WITH_ERRORS);
    }

    @Test
    @DisplayName("Should mark batch import as completed when all lines succeed")
    void shouldMarkAsCompletedWhenAllLinesSucceed() {
        // Given
        User createdUser = User.builder().id(UUID.randomUUID()).username("user1").build();

        BatchImportLine successLine = BatchImportLine.builder()
            .lineNumber(1)
            .rawData("user1,user1@example.com,User,One")
            .success(true)
            .createdUser(createdUser)
            .build();

        List<BatchImportLine> lines = Arrays.asList(successLine);

        when(batchImportPort.findById(batchImportId)).thenReturn(Optional.of(testBatchImport));
        when(batchImportPort.save(any(BatchImport.class))).thenReturn(testBatchImport);

        // When
        batchImportService.updateBatchImportResults(batchImportId, lines);

        // Then
        assertThat(testBatchImport.getSuccessLines()).isEqualTo(1);
        assertThat(testBatchImport.getFailedLines()).isEqualTo(0);
        assertThat(testBatchImport.getStatus()).isEqualTo(BatchImportStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should return empty page when no batch imports exist")
    void shouldReturnEmptyPageWhenNoBatchImportsExist() {
        // Given
        Page<BatchImport> emptyPage = Page.empty(pageable);
        when(batchImportPort.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<BatchImport> result = batchImportService.getAllBatchImports(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(batchImportPort).findAll(pageable);
    }

    @Test
    @DisplayName("Should return empty page when user has no batch imports")
    void shouldReturnEmptyPageWhenUserHasNoBatchImports() {
        // Given
        Page<BatchImport> emptyPage = Page.empty(pageable);
        when(batchImportPort.findByInitiatedByUserId(testUser.getId(), pageable))
            .thenReturn(emptyPage);

        // When
        Page<BatchImport> result = batchImportService.getBatchImportsByUser(
            testUser.getId(), pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(batchImportPort).findByInitiatedByUserId(testUser.getId(), pageable);
    }

    @Test
    @DisplayName("Should create batch import with correct file information")
    void shouldCreateBatchImportWithCorrectFileInfo() {
        // Given
        String fileName = "import-2024.csv";
        byte[] content = "username,email\nuser1,user1@example.com".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            "text/csv",
            content
        );

        when(batchImportPort.save(any(BatchImport.class))).thenAnswer(invocation -> {
            BatchImport saved = invocation.getArgument(0);
            saved.setId(batchImportId);
            return saved;
        });

        // When
        BatchImport result = batchImportService.createBatchImport(file, testUser);

        // Then
        assertThat(result.getFileName()).isEqualTo(fileName);
        assertThat(result.getFileSize()).isEqualTo(content.length);

        verify(batchImportPort).save(any(BatchImport.class));
    }

    @Test
    @DisplayName("Should throw exception when canceling non-existent batch import")
    void shouldThrowExceptionWhenCancelingNonExistentBatchImport() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(batchImportPort.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> batchImportService.cancelBatchImport(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("BatchImport");

        verify(batchImportPort).findById(nonExistentId);
        verify(batchImportPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when failing non-existent batch import")
    void shouldThrowExceptionWhenFailingNonExistentBatchImport() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(batchImportPort.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            batchImportService.failBatchImport(nonExistentId, "Error message")
        ).isInstanceOf(ResourceNotFoundException.class);

        verify(batchImportPort).findById(nonExistentId);
        verify(batchImportPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when updating results for non-existent batch import")
    void shouldThrowExceptionWhenUpdatingNonExistentBatchImport() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(batchImportPort.findById(nonExistentId)).thenReturn(Optional.empty());

        List<BatchImportLine> lines = new ArrayList<>();

        // When & Then
        assertThatThrownBy(() ->
            batchImportService.updateBatchImportResults(nonExistentId, lines)
        ).isInstanceOf(ResourceNotFoundException.class);

        verify(batchImportPort).findById(nonExistentId);
        verify(batchImportPort, never()).save(any());
    }
}
