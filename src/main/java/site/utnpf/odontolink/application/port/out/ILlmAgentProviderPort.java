package site.utnpf.odontolink.application.port.out;

import site.utnpf.odontolink.domain.model.AiRetrievalMethod;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Puerto de salida para el proveedor concreto del LLM (RF31, RF32).
 *
 * <p>Diseñado para ser agnostico al vendor: el dominio solo conoce este
 * contrato. El adapter actual lo implementa contra DigitalOcean Gradient,
 * pero podria reemplazarse por otro (Bedrock, Azure OpenAI, etc.) sin
 * tocar la capa de aplicacion ni el dominio.
 *
 * <p>Las operaciones de provisioning (crear / borrar agente) quedan fuera
 * del puerto a proposito: el provisioning es manual segun decision de
 * arquitectura, por lo que la app solo lee y actualiza un agente existente.
 */
public interface ILlmAgentProviderPort {

    /**
     * Snapshot inmutable del estado del agente segun lo reporta el proveedor.
     * Se usa principalmente en {@code forceResync} para reconciliar el
     * estado local con el remoto y al cablear health checks futuros.
     */
    record AgentSnapshot(
            String id,
            String instruction,
            BigDecimal temperature,
            BigDecimal topP,
            int maxTokens,
            int k,
            AiRetrievalMethod retrievalMethod,
            Instant updatedAt) {
    }

    /**
     * Bloque de actualizacion atomico que viaja al proveedor. Mantiene todos
     * los campos relevantes para que el PUT del adapter sea idempotente
     * y no haga merge ambiguo.
     */
    record AgentUpdateSpec(
            String displayName,
            String instruction,
            BigDecimal temperature,
            BigDecimal topP,
            int maxTokens,
            int k,
            AiRetrievalMethod retrievalMethod) {
    }

    /**
     * Obtiene la configuracion actual del agente. Se usa para validar la
     * disponibilidad del proveedor y para reconciliar el estado local.
     *
     * @throws site.utnpf.odontolink.domain.exception.LlmProviderException si
     *         el proveedor responde con error o la comunicacion falla.
     */
    AgentSnapshot getAgent(String providerAgentId);

    /**
     * Actualiza la configuracion del agente. El adapter es responsable de
     * traducir los campos del dominio a la representacion del proveedor.
     */
    AgentSnapshot updateAgent(String providerAgentId, AgentUpdateSpec spec);
}
