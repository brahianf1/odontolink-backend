package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.domain.model.ProviderGuardrailType;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoAgentResponse;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoAttachGuardrailRequest;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoUpdateAgentRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador del puerto {@link ILlmAgentProviderPort} contra la API de
 * DigitalOcean Gradient AI (RF31, RF32).
 *
 * <p>Aislamos la API de DO en este adaptador para mantener el dominio
 * agnostico al LLM provider. La traduccion entre el enum
 * {@link AiRetrievalMethod} del dominio y el formato {@code RETRIEVAL_METHOD_*}
 * del proveedor vive aqui.
 *
 * <p>Implementa tambien el manejo de guardrails nativos:
 * {@link #attachGuardrail} y {@link #detachGuardrail} mapean a los endpoints
 * {@code POST /agents/{uuid}/guardrails} y
 * {@code DELETE /agents/{uuid}/guardrails/{guardrail_uuid}}. La lista de
 * guardrails vinculados/disponibles viaja como parte del agent body en
 * {@code getAgent()}.
 */
public class DigitalOceanLlmAgentAdapter implements ILlmAgentProviderPort {

    private static final String AGENT_PATH = "/v2/gen-ai/agents/";
    private static final String GUARDRAILS_SEGMENT = "/guardrails";

    private final DigitalOceanGradientClient client;

    public DigitalOceanLlmAgentAdapter(DigitalOceanGradientClient client) {
        this.client = client;
    }

    @Override
    public AgentSnapshot getAgent(String providerAgentId) {
        DoAgentResponse response = client.get(AGENT_PATH + providerAgentId, DoAgentResponse.class);
        return toSnapshot(response);
    }

    @Override
    public AgentSnapshot updateAgent(String providerAgentId, AgentUpdateSpec spec) {
        DoUpdateAgentRequest body = new DoUpdateAgentRequest(
                spec.displayName(),
                spec.instruction(),
                spec.temperature(),
                spec.topP(),
                spec.maxTokens(),
                spec.k(),
                toProviderRetrievalMethod(spec.retrievalMethod()),
                spec.provideCitations()
        );
        DoAgentResponse response = client.put(AGENT_PATH + providerAgentId, body, DoAgentResponse.class);
        return toSnapshot(response);
    }

    @Override
    public void attachGuardrail(String providerAgentId, String providerGuardrailUuid, int priority) {
        DoAttachGuardrailRequest body = new DoAttachGuardrailRequest(providerGuardrailUuid, priority);
        // La respuesta no nos importa: lo unico que necesitamos saber es si fue 2xx.
        // El client convierte 4xx/5xx en LlmProviderException.
        client.post(AGENT_PATH + providerAgentId + GUARDRAILS_SEGMENT, body, Object.class);
    }

    @Override
    public void detachGuardrail(String providerAgentId, String providerGuardrailUuid) {
        client.delete(AGENT_PATH + providerAgentId + GUARDRAILS_SEGMENT + "/" + providerGuardrailUuid);
    }

    private AgentSnapshot toSnapshot(DoAgentResponse response) {
        if (response == null || response.agent() == null) {
            // Si el proveedor responde 200 con body vacio (poco probable),
            // devolvemos un snapshot con valores neutros para que el caller
            // pueda decidir sin romperse.
            return new AgentSnapshot(null, null, null, null, 0, 0, AiRetrievalMethod.NONE,
                    null, null, false, List.of());
        }
        DoAgentResponse.AgentBody body = response.agent();
        String deploymentUrl = body.deployment() == null ? null : body.deployment().url();
        List<ProviderGuardrailSnapshot> guardrails = mapGuardrails(body.guardrails());
        return new AgentSnapshot(
                body.uuid(),
                body.instruction(),
                body.temperature(),
                body.topP(),
                body.maxTokens() == null ? 0 : body.maxTokens(),
                body.k() == null ? 0 : body.k(),
                fromProviderRetrievalMethod(body.retrievalMethod()),
                body.updatedAt(),
                deploymentUrl,
                Boolean.TRUE.equals(body.provideCitations()),
                guardrails
        );
    }

    private static List<ProviderGuardrailSnapshot> mapGuardrails(List<DoAgentResponse.AgentGuardrail> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<ProviderGuardrailSnapshot> out = new ArrayList<>(raw.size());
        for (DoAgentResponse.AgentGuardrail g : raw) {
            if (g == null || g.guardrailUuid() == null) {
                continue;
            }
            out.add(new ProviderGuardrailSnapshot(
                    g.guardrailUuid(),
                    ProviderGuardrailType.fromProviderString(g.type()),
                    g.name(),
                    g.description(),
                    g.defaultResponse(),
                    Boolean.TRUE.equals(g.isAttached()),
                    g.priority() == null ? 100 : g.priority()
            ));
        }
        return out;
    }

    /**
     * Convierte el enum del dominio al string que espera DO. La API usa el
     * prefijo {@code RETRIEVAL_METHOD_*} en todos los valores.
     */
    private String toProviderRetrievalMethod(AiRetrievalMethod method) {
        if (method == null) {
            return null;
        }
        return "RETRIEVAL_METHOD_" + method.name();
    }

    /**
     * Convierte el string crudo del proveedor al enum del dominio. Si el
     * valor no coincide con ninguna constante conocida (por ejemplo el
     * proveedor agrega uno nuevo), devolvemos {@link AiRetrievalMethod#NONE}
     * para evitar lanzar al deserializar un GET de salud.
     */
    private AiRetrievalMethod fromProviderRetrievalMethod(String raw) {
        if (raw == null) {
            return AiRetrievalMethod.NONE;
        }
        String stripped = raw.startsWith("RETRIEVAL_METHOD_") ? raw.substring("RETRIEVAL_METHOD_".length()) : raw;
        try {
            return AiRetrievalMethod.valueOf(stripped);
        } catch (IllegalArgumentException ex) {
            return AiRetrievalMethod.NONE;
        }
    }
}
