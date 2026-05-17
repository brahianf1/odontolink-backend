package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoAgentResponse;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoUpdateAgentRequest;

/**
 * Adaptador del puerto {@link ILlmAgentProviderPort} contra la API de
 * DigitalOcean Gradient AI (RF31, RF32).
 *
 * <p>Aislamos la API de DO en este adaptador para mantener el dominio
 * agnostico al LLM provider. La traduccion entre el enum
 * {@link AiRetrievalMethod} del dominio y el formato {@code RETRIEVAL_METHOD_*}
 * del proveedor vive aqui.
 */
public class DigitalOceanLlmAgentAdapter implements ILlmAgentProviderPort {

    private static final String AGENT_PATH = "/v2/gen-ai/agents/";

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
                toProviderRetrievalMethod(spec.retrievalMethod())
        );
        DoAgentResponse response = client.put(AGENT_PATH + providerAgentId, body, DoAgentResponse.class);
        return toSnapshot(response);
    }

    private AgentSnapshot toSnapshot(DoAgentResponse response) {
        if (response == null || response.agent() == null) {
            // Si el proveedor responde 200 con body vacio (poco probable),
            // devolvemos un snapshot con valores neutros para que el caller
            // pueda decidir sin romperse.
            return new AgentSnapshot(null, null, null, null, 0, 0, AiRetrievalMethod.NONE, null);
        }
        DoAgentResponse.AgentBody body = response.agent();
        return new AgentSnapshot(
                body.uuid(),
                body.instruction(),
                body.temperature(),
                body.topP(),
                body.maxTokens() == null ? 0 : body.maxTokens(),
                body.k() == null ? 0 : body.k(),
                fromProviderRetrievalMethod(body.retrievalMethod()),
                body.updatedAt()
        );
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
