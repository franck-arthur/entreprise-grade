package com.enterprise.app.application.service.audit;

import com.enterprise.app.application.dto.audit.CreateAuditEventCommand;
import com.enterprise.app.domain.model.AuditEventType;
import com.enterprise.app.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Audit Service - Facade for recording audit events.
 *
 * This service provides a simple interface for other services to record audit events.
 * It abstracts the CQRS command/query separation and provides convenient methods.
 *
 * Usage example:
 * <pre>
 * auditService.auditUserCreation(currentUser, createdUser, true);
 * auditService.auditLoginAttempt(username, ipAddress, true, null);
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditCommandService commandService;

    /**
     * Audit user creation.
     */
    public void auditUserCreation(User performedBy, User targetUser, boolean success) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.USER_CREATED)
            .userId(performedBy != null ? performedBy.getId() : null)
            .username(performedBy != null ? performedBy.getUsername() : "system")
            .targetEntityType("User")
            .targetEntityId(targetUser.getId())
            .targetEntityName(targetUser.getUsername())
            .success(success)
            .details("User created: " + targetUser.getUsername())
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit user update.
     */
    public void auditUserUpdate(User performedBy, User targetUser, boolean success) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.USER_UPDATED)
            .userId(performedBy != null ? performedBy.getId() : null)
            .username(performedBy != null ? performedBy.getUsername() : "system")
            .targetEntityType("User")
            .targetEntityId(targetUser.getId())
            .targetEntityName(targetUser.getUsername())
            .success(success)
            .details("User updated: " + targetUser.getUsername())
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit user deletion.
     */
    public void auditUserDeletion(User performedBy, UUID targetUserId, String targetUsername, boolean success) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.USER_DELETED)
            .userId(performedBy != null ? performedBy.getId() : null)
            .username(performedBy != null ? performedBy.getUsername() : "system")
            .targetEntityType("User")
            .targetEntityId(targetUserId)
            .targetEntityName(targetUsername)
            .success(success)
            .details("User deleted: " + targetUsername)
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit user activation.
     */
    public void auditUserActivation(User performedBy, User targetUser) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.USER_ACTIVATED)
            .userId(performedBy != null ? performedBy.getId() : null)
            .username(performedBy != null ? performedBy.getUsername() : "system")
            .targetEntityType("User")
            .targetEntityId(targetUser.getId())
            .targetEntityName(targetUser.getUsername())
            .success(true)
            .details("User activated: " + targetUser.getUsername())
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit user deactivation.
     */
    public void auditUserDeactivation(User performedBy, User targetUser) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.USER_DEACTIVATED)
            .userId(performedBy != null ? performedBy.getId() : null)
            .username(performedBy != null ? performedBy.getUsername() : "system")
            .targetEntityType("User")
            .targetEntityId(targetUser.getId())
            .targetEntityName(targetUser.getUsername())
            .success(true)
            .details("User deactivated: " + targetUser.getUsername())
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit login attempt.
     */
    public void auditLoginAttempt(
        String username, String ipAddress, boolean success, String errorMessage
    ) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(success ? AuditEventType.LOGIN_SUCCESS : AuditEventType.LOGIN_FAILED)
            .username(username)
            .ipAddress(ipAddress)
            .success(success)
            .errorMessage(errorMessage)
            .details(success ? "Login successful" : "Login failed")
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit logout.
     */
    public void auditLogout(User user, String ipAddress) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.LOGOUT)
            .userId(user.getId())
            .username(user.getUsername())
            .ipAddress(ipAddress)
            .success(true)
            .details("User logged out")
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit batch import start.
     */
    public void auditBatchImportStarted(
        User initiatedBy, UUID batchImportId, String fileName
    ) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.BATCH_IMPORT_STARTED)
            .userId(initiatedBy.getId())
            .username(initiatedBy.getUsername())
            .targetEntityType("BatchImport")
            .targetEntityId(batchImportId)
            .targetEntityName(fileName)
            .success(true)
            .details("Batch import started: " + fileName)
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit batch import completion.
     */
    public void auditBatchImportCompleted(
        User initiatedBy, UUID batchImportId, String fileName,
        int successCount, int failedCount
    ) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.BATCH_IMPORT_COMPLETED)
            .userId(initiatedBy.getId())
            .username(initiatedBy.getUsername())
            .targetEntityType("BatchImport")
            .targetEntityId(batchImportId)
            .targetEntityName(fileName)
            .success(true)
            .details(String.format("Batch import completed: %s (Success: %d, Failed: %d)",
                fileName, successCount, failedCount))
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit batch import failure.
     */
    public void auditBatchImportFailed(
        User initiatedBy, UUID batchImportId, String fileName, String errorMessage
    ) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.BATCH_IMPORT_FAILED)
            .userId(initiatedBy.getId())
            .username(initiatedBy.getUsername())
            .targetEntityType("BatchImport")
            .targetEntityId(batchImportId)
            .targetEntityName(fileName)
            .success(false)
            .errorMessage(errorMessage)
            .details("Batch import failed: " + fileName)
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit unauthorized access attempt.
     */
    public void auditUnauthorizedAccess(
        String username, String ipAddress, String resource
    ) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.UNAUTHORIZED_ACCESS)
            .username(username)
            .ipAddress(ipAddress)
            .success(false)
            .details("Unauthorized access attempt to: " + resource)
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit forbidden access attempt.
     */
    public void auditForbiddenAccess(
        User user, String ipAddress, String resource
    ) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.FORBIDDEN_ACCESS)
            .userId(user != null ? user.getId() : null)
            .username(user != null ? user.getUsername() : "unknown")
            .ipAddress(ipAddress)
            .success(false)
            .details("Forbidden access attempt to: " + resource)
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Audit system error.
     */
    public void auditSystemError(String errorMessage, String details) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(AuditEventType.SYSTEM_ERROR)
            .success(false)
            .errorMessage(errorMessage)
            .details(details)
            .build();

        commandService.recordEvent(command);
    }

    /**
     * Generic audit event method.
     */
    public void auditEvent(
        AuditEventType eventType,
        User performedBy,
        String targetEntityType,
        UUID targetEntityId,
        String targetEntityName,
        boolean success,
        String details,
        String errorMessage
    ) {
        CreateAuditEventCommand command = CreateAuditEventCommand.builder()
            .eventType(eventType)
            .userId(performedBy != null ? performedBy.getId() : null)
            .username(performedBy != null ? performedBy.getUsername() : "system")
            .targetEntityType(targetEntityType)
            .targetEntityId(targetEntityId)
            .targetEntityName(targetEntityName)
            .success(success)
            .details(details)
            .errorMessage(errorMessage)
            .build();

        commandService.recordEvent(command);
    }
}
