package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Respuesta de los endpoints de indexing jobs:
 * {@code POST /v2/gen-ai/indexing_jobs} (al disparar)
 * y {@code GET /v2/gen-ai/indexing_jobs/{uuid}} (al consultar estado).
 *
 * <p>El body real incluye conteos detallados (tokens, files, etc.); nosotros
 * solo consumimos los campos necesarios para reflejar el estado en el dominio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DoIndexingJobResponse(@JsonProperty("job") JobBody job) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JobBody(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("status") String status,
            @JsonProperty("updated_at") Instant updatedAt,
            @JsonProperty("error_message") String errorMessage
    ) {
    }
}
