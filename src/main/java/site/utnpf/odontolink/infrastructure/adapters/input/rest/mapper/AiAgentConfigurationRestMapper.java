package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.application.port.in.dto.UpdateAiAgentConfigurationCommand;
import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.UpdateAiAgentConfigurationRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAgentConfigurationResponseDTO;

import java.util.List;

/**
 * Mapper estatico entre los DTOs REST y el dominio del modulo IA. No
 * incluye guardrails (viven en su propio mapper) ni los compone con el
 * prompt: eso lo hace el dominio cuando el caller le pasa los activos.
 */
public final class AiAgentConfigurationRestMapper {

    private AiAgentConfigurationRestMapper() {
    }

    public static UpdateAiAgentConfigurationCommand toCommand(UpdateAiAgentConfigurationRequestDTO dto) {
        return new UpdateAiAgentConfigurationCommand(
                dto.getDisplayName(),
                dto.getSystemPromptCore(),
                dto.getWelcomeMessage(),
                dto.getTemperature(),
                dto.getTopP(),
                dto.getMaxTokens(),
                dto.getK(),
                dto.getRetrievalMethod()
        );
    }

    /**
     * Compone el response con la configuracion vigente y la instruccion
     * preview (concatenacion de guardrails activos + systemPromptCore).
     * El servicio carga los guardrails y los pasa explicitamente para
     * mantener el mapper sin dependencias de infraestructura.
     */
    public static AiAgentConfigurationResponseDTO toResponse(AiAgentConfiguration domain,
                                                             List<site.utnpf.odontolink.domain.model.Guardrail> activeGuardrails) {
        AiAgentConfigurationResponseDTO dto = new AiAgentConfigurationResponseDTO();
        dto.setDisplayName(domain.getDisplayName());
        dto.setSystemPromptCore(domain.getSystemPromptCore());
        dto.setWelcomeMessage(domain.getWelcomeMessage());
        dto.setTemperature(domain.getTemperature());
        dto.setTopP(domain.getTopP());
        dto.setMaxTokens(domain.getMaxTokens());
        dto.setK(domain.getK());
        dto.setRetrievalMethod(domain.getRetrievalMethod());
        dto.setLifecycle(domain.getLifecycle());
        dto.setFinalInstructionPreview(domain.composeInstruction(activeGuardrails));
        dto.setProviderAgentId(domain.getProviderAgentId());
        dto.setProviderSyncedAt(domain.getProviderSyncedAt());
        dto.setLastSyncError(domain.getLastSyncError());
        dto.setUpdatedAt(domain.getUpdatedAt());
        return dto;
    }
}
