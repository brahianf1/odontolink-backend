package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.Guardrail;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.GuardrailResponseDTO;

import java.util.List;

public final class GuardrailRestMapper {

    private GuardrailRestMapper() {
    }

    public static GuardrailResponseDTO toResponse(Guardrail domain) {
        if (domain == null) {
            return null;
        }
        GuardrailResponseDTO dto = new GuardrailResponseDTO();
        dto.setId(domain.getId());
        dto.setLabel(domain.getLabel());
        dto.setText(domain.getText());
        dto.setActive(domain.isActive());
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setUpdatedAt(domain.getUpdatedAt());
        return dto;
    }

    public static List<GuardrailResponseDTO> toResponseList(List<Guardrail> domain) {
        return domain.stream().map(GuardrailRestMapper::toResponse).toList();
    }
}
