package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.FeedbackResponseDTO;

/**
 * Mapper para convertir objetos de dominio Feedback a DTOs de respuesta.
 *
 * Responsabilidad:
 * - Conversión Dominio → DTO: Convierte Feedbacks del dominio a DTOs para respuestas HTTP
 *
 * Este mapper construye un DTO completo que incluye información del usuario que envió
 * el feedback y de la atención asociada.
 *
 * Implementado para responder a los CU-009, CU-016, CU-010 (RF21, RF22, RF24, RF25, RF40).
 *
 * @author OdontoLink Team
 */
public class FeedbackRestMapper {

    private FeedbackRestMapper() {
        // Utility class
    }

    /**
     * Convierte un Feedback del dominio a DTO de respuesta.
     *
     * Extrae información de:
     * - Feedback: id, rating, comment, createdAt
     * - SubmittedBy (User): id, nombre completo, rol
     * - Attention: id, nombre del tratamiento, nombres del paciente y practicante
     *
     * @param domain Objeto de dominio Feedback
     * @return DTO para respuesta HTTP con toda la información necesaria
     */
    public static FeedbackResponseDTO toResponse(Feedback domain) {
        if (domain == null) {
            return null;
        }

        FeedbackResponseDTO response = new FeedbackResponseDTO();
        response.setId(domain.getId());
        response.setRating(domain.getRating());
        response.setComment(domain.getComment());
        response.setCreatedAt(domain.getCreatedAt());

        // Información del usuario que envió el feedback
        if (domain.getSubmittedBy() != null) {
            response.setSubmittedById(domain.getSubmittedBy().getId());
            response.setSubmittedByName(
                    domain.getSubmittedBy().getFirstName() + " " +
                            domain.getSubmittedBy().getLastName()
            );
            if (domain.getSubmittedBy().getRole() != null) {
                response.setSubmittedByRole(domain.getSubmittedBy().getRole().toString());
            }
        }

        // Información de la atención
        if (domain.getAttention() != null) {
            response.setAttentionId(domain.getAttention().getId());

            // Información del tratamiento
            if (domain.getAttention().getTreatment() != null) {
                response.setTreatmentName(domain.getAttention().getTreatment().getName());
            }

            // Información del paciente
            if (domain.getAttention().getPatient() != null
                    && domain.getAttention().getPatient().getUser() != null) {
                var patientUser = domain.getAttention().getPatient().getUser();
                response.setPatientName(
                        patientUser.getFirstName() + " " + patientUser.getLastName()
                );
            }

            // Información del practicante
            if (domain.getAttention().getPractitioner() != null
                    && domain.getAttention().getPractitioner().getUser() != null) {
                var practitionerUser = domain.getAttention().getPractitioner().getUser();
                response.setPractitionerName(
                        practitionerUser.getFirstName() + " " + practitionerUser.getLastName()
                );
            }
        }

        return response;
    }
}
