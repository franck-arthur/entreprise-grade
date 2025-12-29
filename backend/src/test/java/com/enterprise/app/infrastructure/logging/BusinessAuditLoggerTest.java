package com.enterprise.app.infrastructure.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests pour BusinessAuditLogger utilisant Mockito.
 * Utilise une approche hybride : ListAppender pour capturer les logs + Mockito pour mocker MDC.
 */
@ExtendWith(MockitoExtension.class)
class BusinessAuditLoggerTest {

    private BusinessAuditLogger businessAuditLogger;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger businessLogger;
    private MockedStatic<MDC> mockedMDC;

    @BeforeEach
    void setUp() {
        businessAuditLogger = new BusinessAuditLogger();

        // Configuration du logger pour capturer les logs
        businessLogger = (Logger) LoggerFactory.getLogger("BUSINESS_AUDIT");
        listAppender = new ListAppender<>();
        listAppender.start();
        businessLogger.addAppender(listAppender);

        // Mock MDC pour les tests de comportement
        mockedMDC = mockStatic(MDC.class);
    }

    @AfterEach
    void tearDown() {
        // Nettoyage du logger
        businessLogger.detachAppender(listAppender);
        listAppender.stop();

        mockedMDC.close();
    }

    @Test
    void shouldLogBusinessActionWithCorrectFormat() {
        String userId = "USER123";
        String username = "john.doe";
        String action = "CREATE_FORMATION";
        String entityType = "Formation";
        String entityId = "FORM456";
        String details = "Formation Java créée";

        // Mock MDC get pour sessionId
        mockedMDC.when(() -> MDC.get("sessionId")).thenReturn("test-session-id");

        businessAuditLogger.logBusinessAction(userId, username, action, entityType, entityId, details);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("ACTION:CREATE_FORMATION");
        assertThat(event.getFormattedMessage()).contains("USER:john.doe");
        assertThat(event.getFormattedMessage()).contains("ENTITY:Formation#FORM456");
        assertThat(event.getFormattedMessage()).contains("DETAILS:Formation Java créée");

        // Vérification des appels MDC avec Mockito
        mockedMDC.verify(() -> MDC.put("businessEventType", "USER_ACTION"));
        mockedMDC.verify(() -> MDC.put("userId", "USER123"));
        mockedMDC.verify(() -> MDC.put("username", "john.doe"));
        mockedMDC.verify(() -> MDC.put("entityType", "Formation"));
        mockedMDC.verify(() -> MDC.put("entityId", "FORM456"));
        mockedMDC.verify(() -> MDC.put("sessionId", "test-session-id"));
        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogBusinessActionWithNullValues() {
        // Mock MDC get pour sessionId
        mockedMDC.when(() -> MDC.get("sessionId")).thenReturn("test-session-id");

        businessAuditLogger.logBusinessAction(null, null, "DELETE", "Formation", null, null);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("ACTION:DELETE");
        assertThat(event.getFormattedMessage()).contains("USER:null");
        assertThat(event.getFormattedMessage()).contains("ENTITY:Formation#null");
        assertThat(event.getFormattedMessage()).contains("DETAILS:null");

        // Vérification des appels MDC
        mockedMDC.verify(() -> MDC.put("businessEventType", "USER_ACTION"));
        mockedMDC.verify(() -> MDC.put("userId", null));
        mockedMDC.verify(() -> MDC.put("username", null));
        mockedMDC.verify(() -> MDC.put("entityType", "Formation"));
        mockedMDC.verify(() -> MDC.put("entityId", null));
        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogSecurityEventSuccess() {
        String eventType = "LOGIN_SUCCESS";
        String username = "admin.user";
        String ipAddress = "192.168.1.100";
        boolean success = true;
        String details = "Connexion réussie";

        businessAuditLogger.logSecurityEvent(eventType, username, ipAddress, success, details);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("SECURITY:LOGIN_SUCCESS");
        assertThat(event.getFormattedMessage()).contains("USER:admin.user");
        assertThat(event.getFormattedMessage()).contains("IP:192.168.1.100");
        assertThat(event.getFormattedMessage()).contains("SUCCESS:true");
        assertThat(event.getFormattedMessage()).contains("DETAILS:Connexion réussie");

        // Vérification des appels MDC
        mockedMDC.verify(() -> MDC.put("businessEventType", "SECURITY"));
        mockedMDC.verify(() -> MDC.put("username", "admin.user"));
        mockedMDC.verify(() -> MDC.put("ipAddress", "192.168.1.100"));
        mockedMDC.verify(() -> MDC.put("success", "true"));
        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogSecurityEventFailure() {
        String eventType = "LOGIN_FAILED";
        String username = "invalid.user";
        String ipAddress = "10.0.0.1";
        boolean success = false;
        String details = "Mot de passe incorrect";

        businessAuditLogger.logSecurityEvent(eventType, username, ipAddress, success, details);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("SUCCESS:false");

        // Vérification des appels MDC
        mockedMDC.verify(() -> MDC.put("success", "false"));
        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogBusinessProcessWithMetadata() {
        String processType = "BATCH_IMPORT";
        String processId = "BATCH_001";
        String status = "COMPLETED";
        String initiatedBy = "scheduler";

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalRecords", 150);
        metadata.put("successCount", 145);
        metadata.put("errorCount", 5);
        metadata.put("duration", "00:02:30");

        businessAuditLogger.logBusinessProcess(processType, processId, status, initiatedBy, metadata);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("PROCESS:BATCH_IMPORT");
        assertThat(event.getFormattedMessage()).contains("ID:BATCH_001");
        assertThat(event.getFormattedMessage()).contains("STATUS:COMPLETED");
        assertThat(event.getFormattedMessage()).contains("BY:scheduler");
        // Les métadonnées sont formatées en chaîne
        assertThat(event.getFormattedMessage()).contains("totalRecords=150");
        assertThat(event.getFormattedMessage()).contains("successCount=145");
        assertThat(event.getFormattedMessage()).contains("errorCount=5");
        assertThat(event.getFormattedMessage()).contains("duration=00:02:30");

        // Vérification des appels MDC
        mockedMDC.verify(() -> MDC.put("businessEventType", "PROCESS"));
        mockedMDC.verify(() -> MDC.put("processType", "BATCH_IMPORT"));
        mockedMDC.verify(() -> MDC.put("processId", "BATCH_001"));
        mockedMDC.verify(() -> MDC.put("status", "COMPLETED"));
        mockedMDC.verify(() -> MDC.put("initiatedBy", "scheduler"));
        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogBusinessProcessWithNullMetadata() {
        businessAuditLogger.logBusinessProcess("USER_EXPORT", "EXP_002", "STARTED", "user123", null);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("PROCESS:USER_EXPORT");
        assertThat(event.getFormattedMessage()).contains("META:"); // Métadonnées vides

        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogBusinessProcessWithEmptyMetadata() {
        Map<String, Object> emptyMetadata = new HashMap<>();

        businessAuditLogger.logBusinessProcess("DATA_SYNC", "SYNC_003", "FAILED", "system", emptyMetadata);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("META:"); // Métadonnées vides

        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogBusinessError() {
        String errorType = "VALIDATION_ERROR";
        String context = "Formation Creation";
        String description = "La date de formation doit être dans le futur";
        String affectedUser = "trainer.jane";

        businessAuditLogger.logBusinessError(errorType, context, description, affectedUser);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("BUSINESS_ERROR:VALIDATION_ERROR");
        assertThat(event.getFormattedMessage()).contains("CONTEXT:Formation Creation");
        assertThat(event.getFormattedMessage()).contains("USER:trainer.jane");
        assertThat(event.getFormattedMessage()).contains("DESC:La date de formation doit être dans le futur");

        // Vérification des appels MDC
        mockedMDC.verify(() -> MDC.put("businessEventType", "BUSINESS_ERROR"));
        mockedMDC.verify(() -> MDC.put("errorType", "VALIDATION_ERROR"));
        mockedMDC.verify(() -> MDC.put("context", "Formation Creation"));
        mockedMDC.verify(() -> MDC.put("affectedUser", "trainer.jane"));
        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogCustomBusinessEvent() {
        String eventType = "REPORT_GENERATED";
        Map<String, Object> context = new HashMap<>();
        context.put("reportType", "MONTHLY_STATS");
        context.put("period", "2024-12");
        context.put("generatedBy", "analyst.bob");
        context.put("fileSize", "2.5MB");
        context.put("recordCount", 1250);

        businessAuditLogger.logCustomBusinessEvent(eventType, context);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("CUSTOM_EVENT:REPORT_GENERATED");
        // Le contexte est formaté en chaîne
        assertThat(event.getFormattedMessage()).contains("reportType=MONTHLY_STATS");
        assertThat(event.getFormattedMessage()).contains("period=2024-12");
        assertThat(event.getFormattedMessage()).contains("generatedBy=analyst.bob");
        assertThat(event.getFormattedMessage()).contains("fileSize=2.5MB");
        assertThat(event.getFormattedMessage()).contains("recordCount=1250");

        // Vérification des appels MDC
        mockedMDC.verify(() -> MDC.put("businessEventType", "CUSTOM"));
        mockedMDC.verify(() -> MDC.put("customEventType", "REPORT_GENERATED"));
        mockedMDC.verify(() -> MDC.put("custom_reportType", "MONTHLY_STATS"));
        mockedMDC.verify(() -> MDC.put("custom_period", "2024-12"));
        mockedMDC.verify(() -> MDC.put("custom_generatedBy", "analyst.bob"));
        mockedMDC.verify(() -> MDC.put("custom_fileSize", "2.5MB"));
        mockedMDC.verify(() -> MDC.put("custom_recordCount", "1250"));
        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogCustomBusinessEventWithNullContext() {
        businessAuditLogger.logCustomBusinessEvent("SYSTEM_STARTUP", null);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("CUSTOM_EVENT:SYSTEM_STARTUP");
        assertThat(event.getFormattedMessage()).contains("CONTEXT:"); // Contexte vide

        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldLogCustomBusinessEventWithEmptyContext() {
        Map<String, Object> emptyContext = new HashMap<>();

        businessAuditLogger.logCustomBusinessEvent("MAINTENANCE_START", emptyContext);

        // Vérification du contenu des logs
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("CUSTOM_EVENT:MAINTENANCE_START");
        assertThat(event.getFormattedMessage()).contains("CONTEXT:"); // Contexte vide

        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldClearMDCAfterEachLog() {
        // Mock MDC get pour sessionId
        mockedMDC.when(() -> MDC.get("sessionId")).thenReturn("test-session-id");

        // Premier log
        businessAuditLogger.logBusinessAction("USER1", "user1", "CREATE", "Entity", "1", "Test");

        // Second log avec des valeurs différentes
        businessAuditLogger.logSecurityEvent("LOGIN", "user2", "127.0.0.1", true, "Success");

        // Vérifier que MDC.clear() a été appelé deux fois (une fois par log)
        mockedMDC.verify(() -> MDC.clear(), times(2));

        // Vérifier que les deux logs ont été enregistrés
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(2);
        assertThat(logEvents.get(0).getFormattedMessage()).contains("user1");
        assertThat(logEvents.get(1).getFormattedMessage()).contains("user2");
    }

    @Test
    void shouldGenerateSessionIdWhenNotInMDC() {
        // MDC est vide au départ - retourne null
        mockedMDC.when(() -> MDC.get("sessionId")).thenReturn(null);

        businessAuditLogger.logBusinessAction("USER1", "test", "ACTION", "Entity", "1", "Test");

        // Vérifier qu'un sessionId a été généré et ajouté au MDC
        // Comme le sessionId est null, la méthode getSessionId() génère un UUID
        mockedMDC.verify(() -> MDC.put(eq("sessionId"), matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
        mockedMDC.verify(() -> MDC.clear());
    }

    @Test
    void shouldUseExistingSessionIdFromMDC() {
        String existingSessionId = "existing-session-123";
        // Mock MDC get pour retourner le sessionId existant
        mockedMDC.when(() -> MDC.get("sessionId")).thenReturn(existingSessionId);

        businessAuditLogger.logBusinessAction("USER1", "test", "ACTION", "Entity", "1", "Test");

        // Vérifier que le sessionId existant a été utilisé
        mockedMDC.verify(() -> MDC.put("sessionId", existingSessionId));
        mockedMDC.verify(() -> MDC.clear());
    }
}