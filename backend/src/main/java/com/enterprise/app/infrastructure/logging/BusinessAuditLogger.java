package com.enterprise.app.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Logger dédié à la traçabilité métier et audit.
 *
 * Ce logger est séparé du logger technique pour :
 * - Respecter les exigences de conformité et audit
 * - Permettre une rétention différenciée des logs
 * - Faciliter l'analyse des actions métier
 * - Séparer les préoccupations techniques et fonctionnelles
 */
@Component
@Slf4j
public class BusinessAuditLogger {

    private static final Logger BUSINESS_AUDIT_LOGGER = LoggerFactory.getLogger("BUSINESS_AUDIT");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Enregistre une action métier avec contexte utilisateur.
     */
    public void logBusinessAction(String userId, String username, String action,
                                String entityType, String entityId, String details) {
        try {
            // Enrichir le MDC pour les logs structurés
            MDC.put("businessEventType", "USER_ACTION");
            MDC.put("userId", userId);
            MDC.put("username", username);
            MDC.put("entityType", entityType);
            MDC.put("entityId", entityId);
            MDC.put("sessionId", getSessionId());

            BUSINESS_AUDIT_LOGGER.info("ACTION:{} | USER:{} | ENTITY:{}#{} | DETAILS:{}",
                action, username, entityType, entityId, details);

        } finally {
            MDC.clear();
        }
    }

    /**
     * Enregistre un événement de sécurité (authentification, autorisation).
     */
    public void logSecurityEvent(String eventType, String username, String ipAddress,
                               boolean success, String details) {
        try {
            MDC.put("businessEventType", "SECURITY");
            MDC.put("username", username);
            MDC.put("ipAddress", ipAddress);
            MDC.put("success", String.valueOf(success));

            String level = success ? "INFO" : "WARN";
            BUSINESS_AUDIT_LOGGER.info("SECURITY:{} | USER:{} | IP:{} | SUCCESS:{} | DETAILS:{}",
                eventType, username, ipAddress, success, details);

        } finally {
            MDC.clear();
        }
    }

    /**
     * Enregistre un événement de processus métier (formation, import batch).
     */
    public void logBusinessProcess(String processType, String processId, String status,
                                 String initiatedBy, Map<String, Object> metadata) {
        try {
            MDC.put("businessEventType", "PROCESS");
            MDC.put("processType", processType);
            MDC.put("processId", processId);
            MDC.put("status", status);
            MDC.put("initiatedBy", initiatedBy);

            StringBuilder metadataStr = new StringBuilder();
            if (metadata != null) {
                metadata.forEach((key, value) ->
                    metadataStr.append(key).append("=").append(value).append(" "));
            }

            BUSINESS_AUDIT_LOGGER.info("PROCESS:{} | ID:{} | STATUS:{} | BY:{} | META:{}",
                processType, processId, status, initiatedBy, metadataStr.toString().trim());

        } finally {
            MDC.clear();
        }
    }

    /**
     * Enregistre une erreur métier (non technique).
     */
    public void logBusinessError(String errorType, String context, String description,
                               String affectedUser) {
        try {
            MDC.put("businessEventType", "BUSINESS_ERROR");
            MDC.put("errorType", errorType);
            MDC.put("context", context);
            MDC.put("affectedUser", affectedUser);

            BUSINESS_AUDIT_LOGGER.error("BUSINESS_ERROR:{} | CONTEXT:{} | USER:{} | DESC:{}",
                errorType, context, affectedUser, description);

        } finally {
            MDC.clear();
        }
    }

    /**
     * Log générique pour événements métier personnalisés.
     */
    public void logCustomBusinessEvent(String eventType, Map<String, Object> context) {
        try {
            MDC.put("businessEventType", "CUSTOM");
            MDC.put("customEventType", eventType);

            // Ajouter tous les éléments du contexte au MDC
            if (context != null) {
                context.forEach((key, value) ->
                    MDC.put("custom_" + key, String.valueOf(value)));
            }

            StringBuilder contextStr = new StringBuilder();
            if (context != null) {
                context.forEach((key, value) ->
                    contextStr.append(key).append("=").append(value).append(" "));
            }

            BUSINESS_AUDIT_LOGGER.info("CUSTOM_EVENT:{} | CONTEXT:{}",
                eventType, contextStr.toString().trim());

        } finally {
            MDC.clear();
        }
    }

    /**
     * Récupère l'ID de session depuis le contexte de sécurité ou génère un UUID.
     */
    private String getSessionId() {
        // En production, récupérer depuis SecurityContextHolder ou session HTTP
        // Pour l'instant, génération d'un UUID temporaire
        return MDC.get("sessionId") != null ? MDC.get("sessionId") : UUID.randomUUID().toString();
    }
}