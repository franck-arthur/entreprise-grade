package com.enterprise.app.infrastructure.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration hybride des loggers pour l'application.
 *
 * Cette configuration configure :
 * - Logger technique automatique via AOP (@TechnicalLogging)
 * - Logger métier explicite (BusinessAuditLogger) pour la traçabilité
 */
@Configuration
public class LoggingConfig {

    /**
     * Bean pour le logger de traçabilité métier.
     * Injection via @Autowired ou @Qualifier("businessAuditLogger").
     */
    @Bean
    public BusinessAuditLogger businessAuditLogger() {
        return new BusinessAuditLogger();
    }
}