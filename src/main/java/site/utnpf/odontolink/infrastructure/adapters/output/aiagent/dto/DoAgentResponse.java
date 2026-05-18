package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
            @JsonProperty("updated_at") Instant updatedAt,
            @JsonProperty("provide_citations") Boolean provideCitations,
            /**
             * Lista de guardrails reportados para este agente. Incluye tanto los
             * vinculados ({@code is_attached: true}) como los disponibles
             * ({@code is_attached: false}). Sirve como espejo del estado del
             * proveedor.
             */
            @JsonProperty("guardrails") List<AgentGuardrail> guardrails,
            /**
             * Bloque opcional con la URL de invocacion del agente. Solo
             * aparece cuando el agente esta deployado en el dashboard de DO.
             */
            @JsonProperty("deployment") AgentDeployment deployment
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentDeployment(
            @JsonProperty("url") String url,
            @JsonProperty("status") String status
    ) {
    }

    /**
     * Guardrail nativo del proveedor segun lo reporta el agente. Todos los
     * campos son opcionales en la respuesta porque la disponibilidad varia
     * segun el tipo de guardrail.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentGuardrail(
            @JsonProperty("guardrail_uuid") String guardrailUuid,
            @JsonProperty("uuid") String uuid,
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("description") String description,
            @JsonProperty("default_response") String defaultResponse,
            @JsonProperty("priority") Integer priority,
            @JsonProperty("is_attached") Boolean isAttached,
            @JsonProperty("is_default") Boolean isDefault
    ) {
    }
}
