package com.enterprise.app.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration AOP pour activer les aspects Spring et Spring Retry.
 */
@Configuration
@EnableAspectJAutoProxy
@EnableRetry
public class AopConfig {
    // Configuration automatique des aspects via @EnableAspectJAutoProxy
    // Configuration automatique de Spring Retry via @EnableRetry
}