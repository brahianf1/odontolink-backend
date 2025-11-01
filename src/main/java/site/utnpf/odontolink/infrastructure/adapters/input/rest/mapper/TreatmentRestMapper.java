package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.TreatmentResponseDTO;

/**
 * Mapper para convertir entre Treatment (dominio) y TreatmentResponseDTO (REST).
 */
public class TreatmentRestMapper {

    /**
     * Convierte de dominio a DTO de respuesta.
     */
    public static TreatmentResponseDTO toResponse(Treatment domain) {
        if (domain == null) {
            return null;
        }

        return new TreatmentResponseDTO(
                domain.getId(),
                domain.getName(),
                domain.getDescription(),
                domain.getArea()
        );
    }
}
