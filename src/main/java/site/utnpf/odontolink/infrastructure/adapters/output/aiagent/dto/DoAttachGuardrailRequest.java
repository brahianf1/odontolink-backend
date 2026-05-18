package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de {@code POST /v2/gen-ai/agents/{agent_uuid}/guardrails} para
 * vincular un guardrail nativo al agente.
 *
 * <p>Segun el OpenAPI spec de DO (apiAgentGuardrailInput): solo dos campos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoAttachGuardrailRequest(
        @JsonProperty("guardrail_uuid") String guardrailUuid,
        @JsonProperty("priority") Integer priority
) {
}
