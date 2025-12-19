package com.enterprise.app.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Configuration for S3 storage using Garage self-hosted S3-compatible storage.
 * Provides S3 client bean configured to work with Garage endpoint.
 */
@Configuration
public class S3Config {

    /**
     * S3 configuration properties from application.yml.
     */
    @Data
    @ConfigurationProperties(prefix = "app.storage.s3")
    public static class S3Properties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String region;
    }

    /**
     * Creates S3 client bean configured for Garage storage.
     *
     * @param s3Properties S3 configuration properties
     * @return Configured S3Client
     */
    @Bean
    public S3Client s3Client(S3Properties s3Properties) {
        return S3Client.builder()
            .endpointOverride(URI.create(s3Properties.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    s3Properties.getAccessKey(),
                    s3Properties.getSecretKey()
                )
            ))
            .region(Region.of(s3Properties.getRegion()))
            .forcePathStyle(true) // Required for Garage S3 compatibility
            .build();
    }

    /**
     * S3 properties configuration bean.
     *
     * @return S3Properties configuration
     */
    @Bean
    @ConfigurationProperties(prefix = "app.storage.s3")
    public S3Properties s3Properties() {
        return new S3Properties();
    }
}