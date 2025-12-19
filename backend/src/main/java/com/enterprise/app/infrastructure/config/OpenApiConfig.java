package com.enterprise.app.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 (Swagger) configuration.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.name}")
    private String appName;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.description}")
    private String appDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(appName)
                .version(appVersion)
                .description(appDescription + "\n\n" +
                    "## API Versioning\n" +
                    "This API supports multiple versions:\n" +
                    "- **v1**: `/api/v1/` - Current stable version (deprecated)\n" +
                    "- **v2**: `/api/v2/` - New enhanced version with improved features\n\n" +
                    "### Migration Guide\n" +
                    "V2 improvements include:\n" +
                    "- Enhanced response format with metadata\n" +
                    "- Bulk operations support\n" +
                    "- Better error handling\n" +
                    "- PATCH support for partial updates\n" +
                    "- Advanced filtering capabilities")
                .contact(new Contact()
                    .name("Enterprise Team")
                    .email("tech@enterprise.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://enterprise.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development server"),
                new Server()
                    .url("https://api.enterprise.com")
                    .description("Production server")))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token from Keycloak")));
    }
}
