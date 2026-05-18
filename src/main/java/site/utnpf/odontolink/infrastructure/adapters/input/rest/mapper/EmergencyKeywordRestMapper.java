package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.EmergencyKeyword;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.EmergencyKeywordResponseDTO;

public final class EmergencyKeywordRestMapper {

    private EmergencyKeywordRestMapper() {
    }

    public static EmergencyKeywordResponseDTO toResponse(EmergencyKeyword domain) {
        EmergencyKeywordResponseDTO dto = new EmergencyKeywordResponseDTO();
        dto.setId(domain.getId());
        dto.setTerm(domain.getTerm());
        dto.setActive(domain.isActive());
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setUpdatedAt(domain.getUpdatedAt());
        return dto;
    }
}
