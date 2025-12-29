package com.enterprise.app.infrastructure.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessAuditLoggerTest {

    private BusinessAuditLogger businessAuditLogger;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger businessLogger;

    @BeforeEach
    void setUp() {
        businessAuditLogger = new BusinessAuditLogger();

        // Configuration du logger pour capturer les logs
        businessLogger = (Logger) LoggerFactory.getLogger("BUSINESS_AUDIT");
        listAppender = new ListAppender<>();
        listAppender.start();
        businessLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        // Nettoyage du MDC
        MDC.clear();

        // Nettoyage du logger
        businessLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void shouldLogBusinessActionWithCorrectFormat() {
        String userId = "USER123";
        String username = "john.doe";
        String action = "CREATE_FORMATION";
        String entityType = "Formation";
        String entityId = "FORM456";
        String details = "Formation Java créée";

        businessAuditLogger.logBusinessAction(userId, username, action, entityType, entityId, details);

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("ACTION:CREATE_FORMATION");
        assertThat(event.getFormattedMessage()).contains("USER:john.doe");
        assertThat(event.getFormattedMessage()).contains("ENTITY:Formation#FORM456");
        assertThat(event.getFormattedMessage()).contains("DETAILS:Formation Java créée");

        // Vérification des propriétés MDC
        Map<String, String> mdc = event.getMDCPropertyMap();
        assertThat(mdc.get("businessEventType")).isEqualTo("USER_ACTION");
        assertThat(mdc.get("userId")).isEqualTo("USER123");
        assertThat(mdc.get("username")).isEqualTo("john.doe");
        assertThat(mdc.get("entityType")).isEqualTo("Formation");
        assertThat(mdc.get("entityId")).isEqualTo("FORM456");
        assertThat(mdc.get("sessionId")).isNotNull();
    }

    @Test
    void shouldLogBusinessActionWithNullValues() {
        businessAuditLogger.logBusinessAction(null, null, "DELETE", "Formation", null, null);

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("ACTION:DELETE");
        assertThat(event.getFormattedMessage()).contains("USER:null");
        assertThat(event.getFormattedMessage()).contains("ENTITY:Formation#null");
        assertThat(event.getFormattedMessage()).contains("DETAILS:null");
    }

    @Test
    void shouldLogSecurityEventSuccess() {
        String eventType = "LOGIN_SUCCESS";
        String username = "admin.user";
        String ipAddress = "192.168.1.100";
        boolean success = true;
        String details = "Connexion réussie";

        businessAuditLogger.logSecurityEvent(eventType, username, ipAddress, success, details);

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("SECURITY:LOGIN_SUCCESS");
        assertThat(event.getFormattedMessage()).contains("USER:admin.user");
        assertThat(event.getFormattedMessage()).contains("IP:192.168.1.100");
        assertThat(event.getFormattedMessage()).contains("SUCCESS:true");
        assertThat(event.getFormattedMessage()).contains("DETAILS:Connexion réussie");

        Map<String, String> mdc = event.getMDCPropertyMap();
        assertThat(mdc.get("businessEventType")).isEqualTo("SECURITY");
        assertThat(mdc.get("username")).isEqualTo("admin.user");
        assertThat(mdc.get("ipAddress")).isEqualTo("192.168.1.100");
        assertThat(mdc.get("success")).isEqualTo("true");
    }

    @Test
    void shouldLogSecurityEventFailure() {
        String eventType = "LOGIN_FAILED";
        String username = "invalid.user";
        String ipAddress = "10.0.0.1";
        boolean success = false;
        String details = "Mot de passe incorrect";

        businessAuditLogger.logSecurityEvent(eventType, username, ipAddress, success, details);

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("SUCCESS:false");

        Map<String, String> mdc = event.getMDCPropertyMap();
        assertThat(mdc.get("success")).isEqualTo("false");
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

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("PROCESS:BATCH_IMPORT");
        assertThat(event.getFormattedMessage()).contains("ID:BATCH_001");
        assertThat(event.getFormattedMessage()).contains("STATUS:COMPLETED");
        assertThat(event.getFormattedMessage()).contains("BY:scheduler");
        assertThat(event.getFormattedMessage()).contains("totalRecords=150");
        assertThat(event.getFormattedMessage()).contains("successCount=145");
        assertThat(event.getFormattedMessage()).contains("errorCount=5");
        assertThat(event.getFormattedMessage()).contains("duration=00:02:30");

        Map<String, String> mdc = event.getMDCPropertyMap();
        assertThat(mdc.get("businessEventType")).isEqualTo("PROCESS");
        assertThat(mdc.get("processType")).isEqualTo("BATCH_IMPORT");
        assertThat(mdc.get("processId")).isEqualTo("BATCH_001");
        assertThat(mdc.get("status")).isEqualTo("COMPLETED");
        assertThat(mdc.get("initiatedBy")).isEqualTo("scheduler");
    }

    @Test
    void shouldLogBusinessProcessWithNullMetadata() {
        businessAuditLogger.logBusinessProcess("USER_EXPORT", "EXP_002", "STARTED", "user123", null);

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("PROCESS:USER_EXPORT");
        assertThat(event.getFormattedMessage()).contains("META:"); // Métadonnées vides
    }

    @Test
    void shouldLogBusinessProcessWithEmptyMetadata() {
        Map<String, Object> emptyMetadata = new HashMap<>();

        businessAuditLogger.logBusinessProcess("DATA_SYNC", "SYNC_003", "FAILED", "system", emptyMetadata);

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("META:"); // Métadonnées vides
    }

    @Test
    void shouldLogBusinessError() {
        String errorType = "VALIDATION_ERROR";
        String context = "Formation Creation";
        String description = "La date de formation doit être dans le futur";
        String affectedUser = "trainer.jane";

        businessAuditLogger.logBusinessError(errorType, context, description, affectedUser);

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("BUSINESS_ERROR:VALIDATION_ERROR");
        assertThat(event.getFormattedMessage()).contains("CONTEXT:Formation Creation");
        assertThat(event.getFormattedMessage()).contains("USER:trainer.jane");
        assertThat(event.getFormattedMessage()).contains("DESC:La date de formation doit être dans le futur");

        Map<String, String> mdc = event.getMDCPropertyMap();
        assertThat(mdc.get("businessEventType")).isEqualTo("BUSINESS_ERROR");
        assertThat(mdc.get("errorType")).isEqualTo("VALIDATION_ERROR");
        assertThat(mdc.get("context")).isEqualTo("Formation Creation");
        assertThat(mdc.get("affectedUser")).isEqualTo("trainer.jane");
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

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        assertThat(event.getFormattedMessage()).contains("CUSTOM_EVENT:REPORT_GENERATED");
        assertThat(event.getFormattedMessage()).contains("reportType=MONTHLY_STATS");
        assertThat(event.getFormattedMessage()).contains("period=2024-12");
        assertThat(event.getFormattedMessage()).contains("generatedBy=analyst.bob");
        assertThat(event.getFormattedMessage()).contains("fileSize=2.5MB");
        assertThat(event.getFormattedMessage()).contains("recordCount=1250");

        Map<String, String> mdc = event.getMDCPropertyMap();
        assertThat(mdc.get("businessEventType")).isEqualTo("CUSTOM");
        assertThat(mdc.get("customEventType")).isEqualTo("REPORT_GENERATED");
        assertThat(mdc.get("custom_reportType")).isEqualTo("MONTHLY_STATS");
        assertThat(mdc.get("custom_period")).isEqualTo("2024-12");
        assertThat(mdc.get("custom_generatedBy")).isEqualTo("analyst.bob");
        assertThat(mdc.get("custom_fileSize")).isEqualTo("2.5MB");
        assertThat(mdc.get("custom_recordCount")).isEqualTo("1250");
    }

    @Test
    void shouldLogCustomBusinessEventWithNullContext() {
        businessAuditLogger.logCustomBusinessEvent("SYSTEM_STARTUP", null);

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("CUSTOM_EVENT:SYSTEM_STARTUP");
        assertThat(event.getFormattedMessage()).contains("CONTEXT:"); // Contexte vide
    }

    @Test
    void shouldLogCustomBusinessEventWithEmptyContext() {
        Map<String, Object> emptyContext = new HashMap<>();

        businessAuditLogger.logCustomBusinessEvent("MAINTENANCE_START", emptyContext);

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        assertThat(event.getFormattedMessage()).contains("CUSTOM_EVENT:MAINTENANCE_START");
        assertThat(event.getFormattedMessage()).contains("CONTEXT:"); // Contexte vide
    }

    @Test
    void shouldClearMDCAfterEachLog() {
        // Premier log
        businessAuditLogger.logBusinessAction("USER1", "user1", "CREATE", "Entity", "1", "Test");

        // Vérifier que le MDC est nettoyé
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();

        // Second log avec des valeurs différentes
        businessAuditLogger.logSecurityEvent("LOGIN", "user2", "127.0.0.1", true, "Success");

        // Vérifier que le MDC est à nouveau nettoyé
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();

        // Vérifier que les deux logs ont été enregistrés correctement
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(2);
        assertThat(logEvents.get(0).getFormattedMessage()).contains("user1");
        assertThat(logEvents.get(1).getFormattedMessage()).contains("user2");
    }

    @Test
    void shouldGenerateSessionIdWhenNotInMDC() {
        // MDC est vide au départ
        MDC.clear();

        businessAuditLogger.logBusinessAction("USER1", "test", "ACTION", "Entity", "1", "Test");

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        String sessionId = event.getMDCPropertyMap().get("sessionId");
        assertThat(sessionId).isNotNull();
        assertThat(sessionId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void shouldUseExistingSessionIdFromMDC() {
        String existingSessionId = "existing-session-123";
        MDC.put("sessionId", existingSessionId);

        businessAuditLogger.logBusinessAction("USER1", "test", "ACTION", "Entity", "1", "Test");

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(1);

        ILoggingEvent event = logEvents.get(0);
        String sessionId = event.getMDCPropertyMap().get("sessionId");
        assertThat(sessionId).isEqualTo(existingSessionId);
    }
}