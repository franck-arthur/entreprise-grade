package com.enterprise.app.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration AOP pour activer les aspects Spring.
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
    // Configuration automatique des aspects via @EnableAspectJAutoProxy
}