package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Respuesta del POST que registra un data source en la KB. DigitalOcean
 * envuelve el data source recien creado bajo {@code knowledge_base_data_source}.
 *
 * <p>Tolerante a campos nuevos: el proveedor agrega sub-objetos con frecuencia
 * (per type), nosotros solo consumimos {@code uuid} y {@code created_at}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DoRegisterDataSourceResponse(
        @JsonProperty("knowledge_base_data_source") DataSourceBody knowledgeBaseDataSource
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DataSourceBody(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("created_at") Instant createdAt
    ) {
    }
}
