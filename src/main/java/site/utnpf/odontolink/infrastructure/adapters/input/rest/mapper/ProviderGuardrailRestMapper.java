package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.ProviderGuardrail;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ProviderGuardrailResponseDTO;

import java.util.List;

public final class ProviderGuardrailRestMapper {

    private ProviderGuardrailRestMapper() {
    }

    public static ProviderGuardrailResponseDTO toResponse(ProviderGuardrail domain) {
        if (domain == null) {
            return null;
        }
        ProviderGuardrailResponseDTO dto = new ProviderGuardrailResponseDTO();
        dto.setId(domain.getId());
        dto.setProviderGuardrailUuid(domain.getProviderGuardrailUuid());
        dto.setType(domain.getType());
        dto.setDisplayName(domain.getDisplayName());
        dto.setDescription(domain.getDescription());
        dto.setAttached(domain.isAttached());
        dto.setPriority(domain.getPriority());
        dto.setDefaultResponse(domain.getDefaultResponse());
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setUpdatedAt(domain.getUpdatedAt());
        return dto;
    }

    public static List<ProviderGuardrailResponseDTO> toResponseList(List<ProviderGuardrail> domain) {
        return domain.stream().map(ProviderGuardrailRestMapper::toResponse).toList();
    }
}
