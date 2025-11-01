package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AttentionResponseDTO;

import java.util.stream.Collectors;

/**
 * Mapper para convertir objetos de dominio Attention a DTOs de respuesta.
 *
 * Responsabilidad:
 * - Conversión Dominio → DTO: Convierte Attentions del dominio a DTOs para respuestas HTTP
 *
 * Este mapper construye un DTO completo que incluye la lista de Appointments asociados.
 * Utiliza AppointmentRestMapper para convertir cada Appointment hijo.
 *
 * Implementado para responder al CU-008: "Reservar Turno".
 * Cuando un paciente reserva un turno, la respuesta incluye la Attention completa con sus turnos.
 *
 * @author OdontoLink Team
 */
public class AttentionRestMapper {

    private AttentionRestMapper() {
        // Utility class
    }

    /**
     * Convierte una Attention del dominio a DTO de respuesta.
     *
     * Extrae información de:
     * - Attention: id, status, startDate
     * - Patient: id, nombre completo
     * - Practitioner: id, nombre completo
     * - Treatment: id, nombre
     * - Appointments: lista completa de turnos asociados
     *
     * @param domain Objeto de dominio Attention (caso clínico)
     * @return DTO para respuesta HTTP con toda la información necesaria
     */
    public static AttentionResponseDTO toResponse(Attention domain) {
        if (domain == null) {
            return null;
        }

        AttentionResponseDTO response = new AttentionResponseDTO();
        response.setId(domain.getId());
        response.setStatus(domain.getStatus());
        response.setStartDate(domain.getStartDate());

        // Información del paciente
        if (domain.getPatient() != null) {
            response.setPatientId(domain.getPatient().getId());
            if (domain.getPatient().getUser() != null) {
                response.setPatientName(
                        domain.getPatient().getUser().getFirstName() + " " +
                                domain.getPatient().getUser().getLastName()
                );
            }
        }

        // Información del practicante
        if (domain.getPractitioner() != null) {
            response.setPractitionerId(domain.getPractitioner().getId());
            if (domain.getPractitioner().getUser() != null) {
                response.setPractitionerName(
                        domain.getPractitioner().getUser().getFirstName() + " " +
                                domain.getPractitioner().getUser().getLastName()
                );
            }
        }

        // Información del tratamiento
        if (domain.getTreatment() != null) {
            response.setTreatmentId(domain.getTreatment().getId());
            response.setTreatmentName(domain.getTreatment().getName());
        }

        // Mapear la lista de Appointments
        if (domain.getAppointments() != null) {
            response.setAppointments(
                    domain.getAppointments().stream()
                            .map(AppointmentRestMapper::toResponse)
                            .collect(Collectors.toList())
            );
        }

        return response;
    }
}
