package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Body de {@code POST /v2/gen-ai/agents/{agent_uuid}/guardrails} para
 * vincular uno o varios guardrails nativos al agente.
 *
 * <p>Segun el OpenAPI spec oficial de DO (operationId {@code
 * genai_attach_agent_guardrails}, body schema {@code
 * apiLinkAgentGuardrailsInputPublic}), la operacion es <b>batch</b>: se envia
 * un array {@code guardrails} con todos los attachments deseados, no uno por
 * llamada. La version anterior de este DTO mandaba el shape singular y DO
 * respondia 400 sin que el caller lo notara, dejando los guardrails sin
 * sincronizar tras un publish.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoAttachGuardrailsRequest(
        @JsonProperty("guardrails") List<GuardrailItem> guardrails) {

    public record GuardrailItem(
            @JsonProperty("guardrail_uuid") String guardrailUuid,
            @JsonProperty("priority") Integer priority) {
    }
}
