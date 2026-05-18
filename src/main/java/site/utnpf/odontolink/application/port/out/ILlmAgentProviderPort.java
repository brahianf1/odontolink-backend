package site.utnpf.odontolink.application.port.out;

import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.domain.model.ProviderGuardrailType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida para el proveedor concreto del LLM (RF31, RF32).
 *
 * <p>Disenado para ser agnostico al vendor: el dominio solo conoce este
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
     *
     * <p>{@code deploymentEndpoint} es la URL base del agente desplegado para
     * invocaciones de chat completions. {@code null} si el proveedor no lo
     * reporta o el agente no esta deployado. Se usa para descubrir
     * dinamicamente el endpoint cuando no esta forzado por ENV.
     *
     * <p>{@code guardrails} lista los guardrails nativos del proveedor con su
     * estado (vinculado o no), para reconciliar el espejo local.
     */
    record AgentSnapshot(
            String id,
            String instruction,
            BigDecimal temperature,
            BigDecimal topP,
            int maxTokens,
            int k,
            AiRetrievalMethod retrievalMethod,
            Instant updatedAt,
            String deploymentEndpoint,
            boolean provideCitations,
            List<ProviderGuardrailSnapshot> guardrails) {
    }

    /**
     * Metadata de un guardrail del proveedor tal como lo reporta el agente.
     * Esto es lo que el espejo local de {@code ProviderGuardrail} espeja
     * (con la salvedad de que {@code priority} se persiste como intencion del
     * admin, no del proveedor — DO no permite editarla via API).
     */
    record ProviderGuardrailSnapshot(
            String guardrailUuid,
            ProviderGuardrailType type,
            String name,
            String description,
            String defaultResponse,
            boolean isAttached,
            int priority) {
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
            AiRetrievalMethod retrievalMethod,
            boolean provideCitations) {
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

    /**
     * Vincula un guardrail al agente con la prioridad indicada. Idempotente
     * en el sentido de que si el guardrail ya esta vinculado, el proveedor
     * suele aceptar la llamada y actualizar; el caller debe asumir esa
     * semantica.
     */
    void attachGuardrail(String providerAgentId, String providerGuardrailUuid, int priority);

    /**
     * Desvincula un guardrail del agente. Idempotente: si ya no esta
     * vinculado, no se considera error.
     */
    void detachGuardrail(String providerAgentId, String providerGuardrailUuid);
}
