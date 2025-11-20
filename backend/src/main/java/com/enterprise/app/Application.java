package com.enterprise.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application class.
 *
 * Features enabled:
 * - JPA Auditing for automatic createdAt/updatedAt timestamps
 * - Caching with Redis
 * - Async processing for improved performance
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
