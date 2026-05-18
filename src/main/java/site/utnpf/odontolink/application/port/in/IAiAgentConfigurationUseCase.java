package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.application.port.in.dto.UpdateAiAgentConfigurationCommand;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de entrada del modulo de administracion del agente IA (RF31, RF32).
 */
public interface IAiAgentConfigurationUseCase {

    /**
     * Devuelve la configuracion vigente si existe. {@link Optional#empty()}
     * cuando la fila singleton todavia no fue creada (lifecycle virtual
     * {@link AiAgentLifecycle#UNCONFIGURED}). El controller traduce eso a
     * un payload especial sin valores.
     */
    Optional<AiAgentConfiguration> findConfiguration();

    /**
     * Crea o actualiza la configuracion con el contenido provisto por el
     * admin. Cualquier exito deja al agregado en {@code DRAFT}: las
     * ediciones no afectan al paciente hasta el proximo publish.
     */
    AiAgentConfiguration saveConfiguration(UpdateAiAgentConfigurationCommand command);

    /**
     * Reverte la configuracion a {@code DRAFT} sin sincronizar con el
     * proveedor. Util si el admin quiere "despublicar" temporalmente.
     */
    AiAgentConfiguration revertToDraft();

    /**
     * Publica la configuracion al proveedor.
     *
     * @param override si {@code true}, intenta saltar los checks de
     *                 gobernanza. Solo tiene efecto si la policy tiene
     *                 {@code allowOverride=true}.
     * @return la configuracion en estado {@code PUBLISHED} si todo fue ok.
     * @throws site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException
     *         si fallan los checks de gobernanza y no se autorizo override.
     * @throws site.utnpf.odontolink.domain.exception.LlmProviderException si
     *         el proveedor responde con error.
     */
    AiAgentConfiguration publish(boolean override);

    /**
     * Resultado del preview: contiene el texto final que viajaria al
     * proveedor sin tocarlo.
     */
    record PreviewResult(String composedInstruction, List<String> activeGuardrailLabels) {
    }

    PreviewResult preview();

    /**
     * Resultado del health-check: lifecycle vigente + lista de requisitos
     * faltantes para publicar segun la policy actual + reachability separada
     * para management API y endpoint del agente.
     *
     * <p>{@code agentInvocationReachable} es {@code null} cuando no se pudo
     * probar (URL del agente desconocida porque la config esta vacia o el
     * agente no esta deployado). Cuando es {@code false},
     * {@code agentInvocationErrorDetail} explica la causa (tipicamente "401"
     * cuando la access key no es la correcta).
     */
    record HealthResult(
            AiAgentLifecycle lifecycle,
            List<String> missingRequirements,
            boolean providerReachable,
            String providerErrorDetail,
            Boolean agentInvocationReachable,
            String agentInvocationErrorDetail) {
    }

    HealthResult health();
}
