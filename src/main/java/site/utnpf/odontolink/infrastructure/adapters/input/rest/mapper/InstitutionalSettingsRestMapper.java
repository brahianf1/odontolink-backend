package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.InstitutionalSettings;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.InstitutionalSettingsResponseDTO;

/**
 * Mapper estático entre el agregado {@link InstitutionalSettings} y su DTO
 * de respuesta REST (RF07).
 */
public final class InstitutionalSettingsRestMapper {

    private InstitutionalSettingsRestMapper() {
        // Clase de utilidades: no se instancia.
    }

    public static InstitutionalSettingsResponseDTO toDTO(InstitutionalSettings settings) {
        if (settings == null) {
            return null;
        }
        InstitutionalSettingsResponseDTO dto = new InstitutionalSettingsResponseDTO();
        dto.setInstitutionName(settings.getInstitutionName());
        dto.setOpeningHours(settings.getOpeningHours());
        dto.setUsagePolicies(settings.getUsagePolicies());
        dto.setContactEmail(settings.getContactEmail());
        dto.setContactPhone(settings.getContactPhone());
        dto.setContactAddress(settings.getContactAddress());
        dto.setUpdatedAt(settings.getUpdatedAt());
        return dto;
    }
}
