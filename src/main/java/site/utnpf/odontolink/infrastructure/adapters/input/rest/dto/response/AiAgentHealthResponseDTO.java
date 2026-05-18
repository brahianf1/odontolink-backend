package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;

import java.util.List;

/**
 * Respuesta del health-check del modulo IA. {@code missingRequirements}
 * es una lista de codigos estables (REQUIRES_GUARDRAILS, ...) que el FE
 * traduce visualmente.
 *
 * <p>El health distingue dos planos de conectividad con DigitalOcean Gradient:
 * <ul>
 *   <li>{@code providerReachable} / {@code providerErrorDetail}: management
 *       API ({@code api.digitalocean.com/v2/gen-ai}). Autoriza con el PAT.</li>
 *   <li>{@code agentInvocationReachable} /
 *       {@code agentInvocationErrorDetail}: endpoint del agente
 *       ({@code <id>.agents.do-ai.run/api/v1/chat/completions}). Autoriza
 *       con la access key del agente. Si la key esta mal configurada,
 *       management funciona pero el chatbot devuelve siempre fallback.</li>
 * </ul>
 *
 * <p>Los campos de invocacion son opcionales en el JSON: solo aparecen si se
 * intento la prueba (la URL del agente debe estar resuelta).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiAgentHealthResponseDTO(
        AiAgentLifecycle lifecycle,
        List<String> missingRequirements,
        boolean providerReachable,
        String providerErrorDetail,
        Boolean agentInvocationReachable,
        String agentInvocationErrorDetail
) {
}
