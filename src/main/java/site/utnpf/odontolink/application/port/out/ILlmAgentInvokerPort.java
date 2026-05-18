package site.utnpf.odontolink.application.port.out;

import java.util.List;

/**
 * Puerto de salida para INVOCAR el agente IA (chat completions) (RF29/RF34).
 *
 * <p>Separado de {@link ILlmAgentProviderPort} a proposito: management y
 * runtime invocation hablan endpoints distintos en DO Gradient. El management
 * usa {@code api.digitalocean.com/v2/gen-ai/...} (con PAT), mientras que cada
 * agente deployado tiene su propia URL ({@code https://xxx.agents.do-ai.run}).
 * Al separar los puertos:
 * <ul>
 *   <li>El adapter de management no necesita conocer URLs de invocacion.</li>
 *   <li>El adapter de invocacion puede tener su propio circuit breaker
 *       (sensible al SLA en linea critica del chatbot).</li>
 *   <li>Si manania el proveedor de invocacion cambia (otro endpoint, otro
 *       protocolo) solo cambia este puerto.</li>
 * </ul>
 */
public interface ILlmAgentInvokerPort {

    /** Mensaje del historial conversacional que viaja al proveedor. */
    record ChatMessage(String role, String content) {
    }

    /** Documento recuperado por RAG con su score de similitud (0..1). */
    record RetrievalDocument(String dataSourceId, double score) {
    }

    /**
     * Resultado de la invocacion: la respuesta generada + metadata del RAG
     * usada para computar el indicador de confianza (RF34).
     *
     * <p>{@code retrievedDocuments} viene vacia cuando el modelo no necesito
     * consultar la KB (respuesta general). El use case interpreta esto como
     * {@code basedOnKnowledgeBase=false} y {@code confidence=50}.
     */
    record AgentInvocationResult(
            String reply,
            List<RetrievalDocument> retrievedDocuments,
            int totalTokens
    ) {
    }

    /**
     * Llama al endpoint de chat completions del agente.
     *
     * @param agentInvocationUrl URL base del agente (sin path; el adapter
     *                           sufija {@code /api/v1/chat/completions}).
     * @param messages historial completo a enviar (incluye system + user +
     *                 assistant en orden cronologico).
     * @throws site.utnpf.odontolink.domain.exception.LlmProviderException si
     *         el proveedor responde con error o falla la red.
     */
    AgentInvocationResult invoke(String agentInvocationUrl, List<ChatMessage> messages);
}
