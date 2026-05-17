package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.AiGovernancePolicy;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AiGovernancePolicyResponseDTO;

public final class AiGovernancePolicyRestMapper {

    private AiGovernancePolicyRestMapper() {
    }

    public static AiGovernancePolicyResponseDTO toResponse(AiGovernancePolicy domain) {
        if (domain == null) {
            return null;
        }
        AiGovernancePolicyResponseDTO dto = new AiGovernancePolicyResponseDTO();
        dto.setRequireGuardrails(domain.isRequireGuardrails());
        dto.setMinActiveGuardrails(domain.getMinActiveGuardrails());
        dto.setRequireSystemPrompt(domain.isRequireSystemPrompt());
        dto.setRequireWelcomeMessage(domain.isRequireWelcomeMessage());
        dto.setRequireIndexedDocuments(domain.isRequireIndexedDocuments());
        dto.setAllowOverride(domain.isAllowOverride());
        dto.setUpdatedAt(domain.getUpdatedAt());
        return dto;
    }
}
