package com.tucv.ticketflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI ticketflowOpenAPI() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Token JWT (HS256) emitido por auth-service-jwt");

        return new OpenAPI()
                .info(new Info()
                        .title("Ticketflow API")
                        .description("API REST de mesa de ayuda (helpdesk): tickets, comentarios, "
                                + "SLA por prioridad, máquina de estados y dashboard.")
                        .version("1.0.0")
                        .contact(new Contact().name("TuCV").email("soporte@tucv.com")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, bearerAuth))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
