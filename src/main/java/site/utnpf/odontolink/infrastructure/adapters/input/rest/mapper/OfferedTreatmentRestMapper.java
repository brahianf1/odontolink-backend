package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.OfferedTreatmentResponseDTO;

/**
 * Mapper para convertir objetos de dominio OfferedTreatment a DTOs de respuesta.
 *
 * Responsabilidad:
 * - Conversión Dominio → DTO: Convierte objetos de dominio a DTOs para respuestas HTTP
 *
 * Para conversiones de AvailabilitySlot, se utiliza AvailabilitySlotInputMapper que maneja
 * tanto conversiones DTO → Dominio como Dominio → DTO de manera bidireccional.
 *
 * @author OdontoLink Team
 */
public class OfferedTreatmentRestMapper {

    private OfferedTreatmentRestMapper() {
        // Utility class
    }

    /**
     * Convierte un OfferedTreatment del dominio a DTO de respuesta.
     * El progreso actual de atenciones completadas, activas y canceladas se establece en 0.
     *
     * @param domain Objeto de dominio OfferedTreatment
     * @return DTO para respuesta HTTP
     */
    public static OfferedTreatmentResponseDTO toResponse(OfferedTreatment domain) {
        return toResponse(domain, 0, 0, 0);
    }

    /**
     * Convierte un OfferedTreatment del dominio a DTO de respuesta,
     * incluyendo el progreso detallado (completadas, activas y canceladas).
     *
     * Este método permite enriquecer el DTO con información calculada dinámicamente
     * para eliminar la ambigüedad entre "Meta Académica", "Carga de Trabajo" y "Estadísticas".
     *
     * @param domain Objeto de dominio OfferedTreatment
     * @param currentCompletedAttentions Número actual de atenciones completadas (Meta)
     * @param currentActiveAttentions Número actual de atenciones activas (Carga)
     * @param currentCancelledAttentions Número histórico de atenciones canceladas (Estadística)
     * @return DTO para respuesta HTTP enriquecido con progreso detallado
     */
    public static OfferedTreatmentResponseDTO toResponse(OfferedTreatment domain,
                                                         int currentCompletedAttentions,
                                                         int currentActiveAttentions,
                                                         int currentCancelledAttentions) {
        if (domain == null) {
            return null;
        }

        OfferedTreatmentResponseDTO response = new OfferedTreatmentResponseDTO();
        response.setId(domain.getId());

        if (domain.getPractitioner() != null) {
            response.setPractitionerId(domain.getPractitioner().getId());
            if (domain.getPractitioner().getUser() != null) {
                response.setPractitionerName(
                    domain.getPractitioner().getUser().getFirstName() + " " +
                    domain.getPractitioner().getUser().getLastName()
                );
            }
        }

        if (domain.getTreatment() != null) {
            response.setTreatment(TreatmentRestMapper.toResponse(domain.getTreatment()));
        }

        response.setRequirements(domain.getRequirements());
        response.setDurationInMinutes(domain.getDurationInMinutes());
        response.setOfferStartDate(domain.getOfferStartDate());
        response.setOfferEndDate(domain.getOfferEndDate());
        response.setMaxCompletedAttentions(domain.getMaxCompletedAttentions());
        
        // Nuevos campos detallados
        response.setCurrentCompletedAttentions(currentCompletedAttentions);
        response.setCurrentActiveAttentions(currentActiveAttentions);
        response.setCurrentCancelledAttentions(currentCancelledAttentions);

        // Cálculo de bloqueo (Lógica de Negocio replicada para visualización)
        boolean isBlocked = false;
        if (domain.getMaxCompletedAttentions() != null) {
            int totalConsumed = currentCompletedAttentions + currentActiveAttentions;
            isBlocked = totalConsumed >= domain.getMaxCompletedAttentions();
        }
        response.setAvailabilityBlocked(isBlocked);

        if (domain.getAvailabilitySlots() != null) {
            response.setAvailabilitySlots(
                AvailabilitySlotInputMapper.toDTOSet(domain.getAvailabilitySlots())
            );
        }

        return response;
    }
}
