package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Estructura de respuesta de {@code GET /v2/gen-ai/agents/{uuid}} y
 * {@code PUT /v2/gen-ai/agents/{uuid}}. DigitalOcean envuelve la informacion
 * del agente en una propiedad {@code agent} dentro del root del payload.
 *
 * <p>Anotamos {@code @JsonIgnoreProperties(ignoreUnknown = true)} porque el
 * proveedor agrega campos con frecuencia y no queremos que un campo nuevo
 * rompa la deserializacion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DoAgentResponse(@JsonProperty("agent") AgentBody agent) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentBody(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("name") String name,
            @JsonProperty("instruction") String instruction,
            @JsonProperty("temperature") BigDecimal temperature,
            @JsonProperty("top_p") BigDecimal topP,
            @JsonProperty("max_tokens") Integer maxTokens,
            @JsonProperty("k") Integer k,
            @JsonProperty("retrieval_method") String retrievalMethod,
            @JsonProperty("updated_at") Instant updatedAt
    ) {
    }
}
