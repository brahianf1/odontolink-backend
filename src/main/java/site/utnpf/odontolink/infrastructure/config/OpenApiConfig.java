package site.utnpf.odontolink.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger para documentación automática de la API.
 * Define la información general de la API y la configuración de seguridad JWT.
 * Desacoplada de la lógica de negocio, reside en la capa de infraestructura.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private String serverPort;

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    /**
     * Configura el objeto OpenAPI con información general de la API y seguridad JWT.
     * La documentación estará disponible en:
     * - Swagger UI: /swagger-ui.html
     * - OpenAPI JSON: /v3/api-docs
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(localServer()))
                .components(securityComponents())
                .addSecurityItem(securityRequirement());
    }

    /**
     * Información general de la API.
     */
    private Info apiInfo() {
        return new Info()
                .title("OdontoLink API")
                .description("API REST para la gestión de citas y tratamientos odontológicos. "
                        + "Sistema de información para clínicas universitarias que conecta pacientes, "
                        + "practicantes y supervisores.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("Equipo OdontoLink")
                        .email("soporte@odontolink.com"))
                .license(new License()
                        .name("Propietario")
                        .url("https://odontolink.com/license"));
    }

    /**
     * Configuración del servidor local.
     */
    private Server localServer() {
        return new Server()
                .url("http://localhost:" + serverPort)
                .description("Servidor de desarrollo local");
    }

    /**
     * Configuración de componentes de seguridad JWT.
     * Define el esquema de autenticación Bearer para todos los endpoints protegidos.
     */
    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresar el token JWT obtenido del endpoint de login"));
    }

    /**
     * Requisito de seguridad aplicado globalmente a todos los endpoints.
     * Los endpoints públicos pueden sobrescribir esto con @SecurityRequirement(name = "").
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList(SECURITY_SCHEME_NAME);
    }
}
