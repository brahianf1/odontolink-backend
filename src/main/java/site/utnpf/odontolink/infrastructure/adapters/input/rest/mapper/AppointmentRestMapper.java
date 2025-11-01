package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AppointmentResponseDTO;

/**
 * Mapper para convertir objetos de dominio Appointment a DTOs de respuesta.
 *
 * Responsabilidad:
 * - Conversión Dominio → DTO: Convierte Appointments del dominio a DTOs para respuestas HTTP
 *
 * Este mapper extrae información de las entidades relacionadas (Attention, Patient, Practitioner, Treatment)
 * para construir un DTO completo que el frontend pueda mostrar sin necesidad de múltiples peticiones.
 *
 * Implementado para CU-008: "Reservar Turno".
 *
 * @author OdontoLink Team
 */
public class AppointmentRestMapper {

    private AppointmentRestMapper() {
        // Utility class
    }

    /**
     * Convierte un Appointment del dominio a DTO de respuesta.
     *
     * Extrae información de:
     * - Appointment: id, appointmentTime, motive, status
     * - Attention: id (para navegación)
     * - Patient: id, nombre completo (para vista del practicante)
     * - Practitioner: id, nombre completo (para vista del paciente)
     * - Treatment: id, nombre
     *
     * @param domain Objeto de dominio Appointment
     * @return DTO para respuesta HTTP con toda la información necesaria
     */
    public static AppointmentResponseDTO toResponse(Appointment domain) {
        if (domain == null) {
            return null;
        }

        AppointmentResponseDTO response = new AppointmentResponseDTO();
        response.setId(domain.getId());
        response.setAppointmentTime(domain.getAppointmentTime());
        response.setMotive(domain.getMotive());
        response.setStatus(domain.getStatus());

        // Extraer información de la Attention relacionada
        if (domain.getAttention() != null) {
            response.setAttentionId(domain.getAttention().getId());

            // Información del paciente
            if (domain.getAttention().getPatient() != null) {
                response.setPatientId(domain.getAttention().getPatient().getId());
                if (domain.getAttention().getPatient().getUser() != null) {
                    response.setPatientName(
                            domain.getAttention().getPatient().getUser().getFirstName() + " " +
                                    domain.getAttention().getPatient().getUser().getLastName()
                    );
                }
            }

            // Información del practicante
            if (domain.getAttention().getPractitioner() != null) {
                response.setPractitionerId(domain.getAttention().getPractitioner().getId());
                if (domain.getAttention().getPractitioner().getUser() != null) {
                    response.setPractitionerName(
                            domain.getAttention().getPractitioner().getUser().getFirstName() + " " +
                                    domain.getAttention().getPractitioner().getUser().getLastName()
                    );
                }
            }

            // Información del tratamiento
            if (domain.getAttention().getTreatment() != null) {
                response.setTreatmentId(domain.getAttention().getTreatment().getId());
                response.setTreatmentName(domain.getAttention().getTreatment().getName());
            }
        }

        return response;
    }
}
