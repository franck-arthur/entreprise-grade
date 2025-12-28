package com.enterprise.app.infrastructure.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Spring Batch pour l'application
 *
 * Active le traitement par lots (batch processing) dans l'application Spring Boot.
 * Spring Batch 5+ utilise une configuration automatique, cette classe sert principalement
 * à documenter l'activation explicite du batch processing.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    // Spring Batch 5+ avec Spring Boot 3+ utilise la configuration automatique
    // Cette classe active explicitement le batch processing et peut être étendue
    // avec des beans de configuration personnalisés si nécessaire
}