package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;

import java.util.List;

/**
 * Respuesta del health-check del modulo IA. {@code missingRequirements}
 * es una lista de codigos estables (REQUIRES_GUARDRAILS, ...) que el FE
 * traduce visualmente.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiAgentHealthResponseDTO(
        AiAgentLifecycle lifecycle,
        List<String> missingRequirements,
        boolean providerReachable,
        String providerErrorDetail
) {
}
