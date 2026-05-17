package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAdminAuditEventResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiAgentConfigurationVersionResponseDTO;

import java.util.List;

public final class AiAgentVersioningRestMapper {

    private AiAgentVersioningRestMapper() {
    }

    public static AiAgentConfigurationVersionResponseDTO toResponse(AiAgentConfigurationVersion domain) {
        if (domain == null) {
            return null;
        }
        AiAgentConfigurationVersionResponseDTO dto = new AiAgentConfigurationVersionResponseDTO();
        dto.setVersionNumber(domain.getVersionNumber());
        dto.setDisplayName(domain.getDisplayName());
        dto.setSystemPromptCore(domain.getSystemPromptCore());
        dto.setWelcomeMessage(domain.getWelcomeMessage());
        dto.setTemperature(domain.getTemperature());
        dto.setTopP(domain.getTopP());
        dto.setMaxTokens(domain.getMaxTokens());
        dto.setK(domain.getK());
        dto.setRetrievalMethod(domain.getRetrievalMethod());
        dto.setComposedInstruction(domain.getComposedInstruction());
        dto.setGuardrailsLabelsSnapshot(domain.getGuardrailsLabelsSnapshot());
        dto.setPublishedByUserId(domain.getPublishedByUserId());
        dto.setPublishedWithOverride(domain.isPublishedWithOverride());
        dto.setMissingRequirementsAtPublish(domain.getMissingRequirementsAtPublish());
        dto.setPublishedAt(domain.getPublishedAt());
        return dto;
    }

    public static List<AiAgentConfigurationVersionResponseDTO> toVersionResponseList(List<AiAgentConfigurationVersion> versions) {
        return versions.stream()
                .map(AiAgentVersioningRestMapper::toResponse)
                .toList();
    }

    public static AiAdminAuditEventResponseDTO toResponse(AiAdminAuditEvent event) {
        if (event == null) {
            return null;
        }
        AiAdminAuditEventResponseDTO dto = new AiAdminAuditEventResponseDTO();
        dto.setId(event.getId());
        dto.setType(event.getType());
        dto.setActorUserId(event.getActorUserId());
        dto.setRelatedVersionNumber(event.getRelatedVersionNumber());
        dto.setWithOverride(event.isWithOverride());
        dto.setDetails(event.getDetails());
        dto.setOccurredAt(event.getOccurredAt());
        return dto;
    }

    public static List<AiAdminAuditEventResponseDTO> toAuditResponseList(List<AiAdminAuditEvent> events) {
        return events.stream()
                .map(AiAgentVersioningRestMapper::toResponse)
                .toList();
    }
}
