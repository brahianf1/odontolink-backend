package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Cuerpo del PUT a {@code /v2/gen-ai/agents/{uuid}}. Solo se serializan los
 * campos no nulos ({@link JsonInclude.Include#NON_NULL}) para no enviar
 * sobre-escrituras de campos que no estamos editando explicitamente.
 *
 * <p>Todos los campos son opcionales segun la spec de DigitalOcean, pero
 * mandamos siempre todos los que el dominio gestiona (instruction,
 * temperatura, top_p, max_tokens, k, retrieval_method) para que la
 * operacion sea idempotente.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoUpdateAgentRequest(
        @JsonProperty("name") String name,
        @JsonProperty("instruction") String instruction,
        @JsonProperty("temperature") BigDecimal temperature,
        @JsonProperty("top_p") BigDecimal topP,
        @JsonProperty("max_tokens") Integer maxTokens,
        @JsonProperty("k") Integer k,
        @JsonProperty("retrieval_method") String retrievalMethod
) {
}
