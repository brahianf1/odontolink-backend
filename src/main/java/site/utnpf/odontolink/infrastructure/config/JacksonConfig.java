package site.utnpf.odontolink.infrastructure.config;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de Jackson para Odontolink.
 *
 * Registra explicitamente {@link JsonNullableModule} para que Jackson pueda
 * serializar/deserializar {@code JsonNullable<T>}. Aunque la libreria
 * auto-registra el modulo via Jackson SPI, lo declaramos como bean para que
 * la dependencia sea visible en codigo y resistente a cambios accidentales
 * en classpath.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonNullableModule jsonNullableModule() {
        return new JsonNullableModule();
    }
}
