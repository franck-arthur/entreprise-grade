package com.enterprise.app.application.service.audit;

import com.enterprise.app.application.dto.audit.CreateAuditEventCommand;
import com.enterprise.app.domain.model.AuditEventCommand;
import com.enterprise.app.domain.model.AuditEventProjection;
import com.enterprise.app.domain.model.AuditEventType;
import com.enterprise.app.domain.port.AuditEventCommandPort;
import com.enterprise.app.domain.port.AuditEventQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditCommandService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditCommandService Tests")
class AuditCommandServiceTest {

    @Mock
    private AuditEventCommandPort auditEventCommandPort;

    @Mock
    private AuditEventQueryPort auditEventQueryPort;

    @InjectMocks
    private AuditCommandService auditCommandService;

    private CreateAuditEventCommand createCommand;
    private AuditEventCommand savedCommandEvent;
    private UUID testUserId;
    private UUID testTargetEntityId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testTargetEntityId = UUID.randomUUID();

        createCommand = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.USER_CREATED)
            .userId(testUserId)
            .username("testuser")
            .targetEntityType("User")
            .targetEntityId(testTargetEntityId)
            .targetEntityName("John Doe")
            .ipAddress("192.168.1.100")
            .userAgent("Mozilla/5.0")
            .details("User created successfully")
            .success(true)
            .build();

        savedCommandEvent = AuditEventCommand.builder()
            .id(UUID.randomUUID())
            .eventType(AuditEventType.USER_CREATED)
            .userId(testUserId)
            .username("testuser")
            .targetEntityType("User")
            .targetEntityId(testTargetEntityId)
            .ipAddress("192.168.1.100")
            .userAgent("Mozilla/5.0")
            .details("User created successfully")
            .success(true)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("Should record audit event successfully")
    void shouldRecordAuditEvent() {
        // Given
        when(auditEventCommandPort.save(any(AuditEventCommand.class)))
            .thenReturn(savedCommandEvent);

        // When
        AuditEventCommand result = auditCommandService.recordEvent(createCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedCommandEvent.getId());
        assertThat(result.getEventType()).isEqualTo(AuditEventType.USER_CREATED);
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.isSuccess()).isTrue();

        // Verify save was called
        ArgumentCaptor<AuditEventCommand> captor = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditEventCommandPort).save(captor.capture());

        AuditEventCommand capturedEvent = captor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(AuditEventType.USER_CREATED);
        assertThat(capturedEvent.getUserId()).isEqualTo(testUserId);
        assertThat(capturedEvent.getDetails()).isEqualTo("User created successfully");
    }

    @Test
    @DisplayName("Should record failed event with error message")
    void shouldRecordFailedEvent() {
        // Given
        createCommand.setSuccess(false);
        createCommand.setErrorMessage("Validation failed");

        AuditEventCommand failedEvent = AuditEventCommand.builder()
            .id(UUID.randomUUID())
            .eventType(AuditEventType.USER_CREATED)
            .userId(testUserId)
            .username("testuser")
            .success(false)
            .errorMessage("Validation failed")
            .timestamp(LocalDateTime.now())
            .build();

        when(auditEventCommandPort.save(any(AuditEventCommand.class)))
            .thenReturn(failedEvent);

        // When
        AuditEventCommand result = auditCommandService.recordEvent(createCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Validation failed");

        verify(auditEventCommandPort).save(any(AuditEventCommand.class));
    }

    @Test
    @DisplayName("Should project event asynchronously with target entity name")
    void shouldProjectEventAsync() {
        // Given
        String targetEntityName = "John Doe";

        // When
        auditCommandService.projectEventAsync(savedCommandEvent, targetEntityName);

        // Then
        verify(auditEventQueryPort).save(any(AuditEventProjection.class));
    }

    @Test
    @DisplayName("Should project event asynchronously without target entity name")
    void shouldProjectEventAsyncWithoutEntityName() {
        // Given - targetEntityName is null

        // When
        auditCommandService.projectEventAsync(savedCommandEvent, null);

        // Then
        verify(auditEventQueryPort).save(any(AuditEventProjection.class));
    }

    @Test
    @DisplayName("Should handle projection failure gracefully")
    void shouldHandleProjectionFailureGracefully() {
        // Given
        when(auditEventQueryPort.save(any(AuditEventProjection.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        assertThatCode(() ->
            auditCommandService.projectEventAsync(savedCommandEvent, "John Doe")
        ).doesNotThrowAnyException();

        verify(auditEventQueryPort).save(any(AuditEventProjection.class));
    }

    @Test
    @DisplayName("Should project event synchronously")
    void shouldProjectEventSync() {
        // Given
        String targetEntityName = "John Doe";
        AuditEventProjection mockProjection = AuditEventProjection.builder()
            .id(savedCommandEvent.getId())
            .eventType(savedCommandEvent.getEventType())
            .build();

        when(auditEventQueryPort.save(any(AuditEventProjection.class)))
            .thenReturn(mockProjection);

        // When
        auditCommandService.projectEventSync(savedCommandEvent, targetEntityName);

        // Then
        ArgumentCaptor<AuditEventProjection> captor =
            ArgumentCaptor.forClass(AuditEventProjection.class);
        verify(auditEventQueryPort).save(captor.capture());

        AuditEventProjection capturedProjection = captor.getValue();
        assertThat(capturedProjection.getId()).isEqualTo(savedCommandEvent.getId());
        assertThat(capturedProjection.getEventType()).isEqualTo(savedCommandEvent.getEventType());
    }

    @Test
    @DisplayName("Should project event synchronously without target entity name")
    void shouldProjectEventSyncWithoutEntityName() {
        // Given
        when(auditEventQueryPort.save(any(AuditEventProjection.class)))
            .thenReturn(any(AuditEventProjection.class));

        // When
        auditCommandService.projectEventSync(savedCommandEvent, null);

        // Then
        verify(auditEventQueryPort).save(any(AuditEventProjection.class));
    }

    @Test
    @DisplayName("Should rebuild all projections from command model")
    void shouldRebuildProjections() {
        // Given
        AuditEventCommand command1 = AuditEventCommand.builder()
            .id(UUID.randomUUID())
            .eventType(AuditEventType.USER_CREATED)
            .timestamp(LocalDateTime.now())
            .build();

        AuditEventCommand command2 = AuditEventCommand.builder()
            .id(UUID.randomUUID())
            .eventType(AuditEventType.USER_UPDATED)
            .timestamp(LocalDateTime.now())
            .build();

        AuditEventCommand command3 = AuditEventCommand.builder()
            .id(UUID.randomUUID())
            .eventType(AuditEventType.LOGIN_SUCCESS)
            .timestamp(LocalDateTime.now())
            .build();

        List<AuditEventCommand> allCommands = Arrays.asList(command1, command2, command3);

        when(auditEventCommandPort.findAll()).thenReturn(allCommands);
        when(auditEventQueryPort.count()).thenReturn(3L);
        doNothing().when(auditEventQueryPort).deleteAll();

        // When
        auditCommandService.rebuildProjections();

        // Then
        verify(auditEventQueryPort).deleteAll();
        verify(auditEventCommandPort).findAll();
        verify(auditEventQueryPort, times(3)).save(any(AuditEventProjection.class));
        verify(auditEventQueryPort).count();
    }

    @Test
    @DisplayName("Should rebuild empty projections when no commands exist")
    void shouldRebuildEmptyProjections() {
        // Given
        when(auditEventCommandPort.findAll()).thenReturn(List.of());
        when(auditEventQueryPort.count()).thenReturn(0L);
        doNothing().when(auditEventQueryPort).deleteAll();

        // When
        auditCommandService.rebuildProjections();

        // Then
        verify(auditEventQueryPort).deleteAll();
        verify(auditEventCommandPort).findAll();
        verify(auditEventQueryPort, never()).save(any(AuditEventProjection.class));
        verify(auditEventQueryPort).count();
    }

    @Test
    @DisplayName("Should record authentication event")
    void shouldRecordAuthenticationEvent() {
        // Given
        CreateAuditEventCommand authCommand = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.LOGIN_SUCCESS)
            .username("testuser")
            .ipAddress("192.168.1.100")
            .userAgent("Mozilla/5.0")
            .success(true)
            .build();

        AuditEventCommand authEvent = AuditEventCommand.builder()
            .id(UUID.randomUUID())
            .eventType(AuditEventType.LOGIN_SUCCESS)
            .username("testuser")
            .ipAddress("192.168.1.100")
            .success(true)
            .timestamp(LocalDateTime.now())
            .build();

        when(auditEventCommandPort.save(any(AuditEventCommand.class)))
            .thenReturn(authEvent);

        // When
        AuditEventCommand result = auditCommandService.recordEvent(authCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getIpAddress()).isEqualTo("192.168.1.100");

        verify(auditEventCommandPort).save(any(AuditEventCommand.class));
    }

    @Test
    @DisplayName("Should record batch import event")
    void shouldRecordBatchImportEvent() {
        // Given
        CreateAuditEventCommand batchCommand = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.BATCH_IMPORT_STARTED)
            .userId(testUserId)
            .username("admin")
            .targetEntityType("BatchImport")
            .targetEntityId(UUID.randomUUID())
            .details("Started importing 1000 users")
            .success(true)
            .build();

        AuditEventCommand batchEvent = AuditEventCommand.builder()
            .id(UUID.randomUUID())
            .eventType(AuditEventType.BATCH_IMPORT_STARTED)
            .userId(testUserId)
            .username("admin")
            .success(true)
            .timestamp(LocalDateTime.now())
            .build();

        when(auditEventCommandPort.save(any(AuditEventCommand.class)))
            .thenReturn(batchEvent);

        // When
        AuditEventCommand result = auditCommandService.recordEvent(batchCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isEqualTo(AuditEventType.BATCH_IMPORT_STARTED);
        assertThat(result.getUserId()).isEqualTo(testUserId);

        verify(auditEventCommandPort).save(any(AuditEventCommand.class));
    }
}
