package com.enterprise.app.infrastructure.configuration;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration Spring 6 pour l'observabilité et monitoring.
 * Active les fonctionnalités d'observation Micrometer et Spring Boot 3.
 */
@Configuration
@EnableAspectJAutoProxy
@EnableAsync
public class ObservabilityConfiguration {

    /**
     * Active l'aspect @Observed pour l'instrumentation automatique.
     * Nouvelle fonctionnalité Spring Boot 3 / Spring 6.
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}