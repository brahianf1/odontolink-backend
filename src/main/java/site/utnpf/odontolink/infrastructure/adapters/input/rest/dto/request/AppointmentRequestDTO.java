package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO para la solicitud de reserva de un turno.
 * Implementa el CU-008: "Reservar Turno".
 *
 * Este DTO captura la "intención" del paciente de reservar un turno:
 * - Qué tratamiento ofrecido quiere (offeredTreatmentId)
 * - Cuándo lo quiere (appointmentTime)
 *
 * El patientId NO se incluye aquí porque se obtiene del usuario autenticado
 * mediante AuthenticationFacade.getAuthenticatedPatientId().
 */
public class AppointmentRequestDTO {

    /**
     * ID del tratamiento ofrecido (OfferedTreatment) seleccionado del catálogo.
     * Este ID identifica tanto el tratamiento como el practicante que lo ofrece.
     */
    @NotNull(message = "El ID del tratamiento ofrecido es obligatorio")
    private Long offeredTreatmentId;

    /**
     * Fecha y hora exactas del turno solicitado.
     * Debe ser una fecha futura y debe estar dentro de la disponibilidad del practicante.
     */
    @NotNull(message = "La fecha y hora del turno son obligatorias")
    @Future(message = "La fecha del turno debe ser futura")
    private LocalDateTime appointmentTime;

    // Constructores
    public AppointmentRequestDTO() {
    }

    public AppointmentRequestDTO(Long offeredTreatmentId, LocalDateTime appointmentTime) {
        this.offeredTreatmentId = offeredTreatmentId;
        this.appointmentTime = appointmentTime;
    }

    // Getters y Setters
    public Long getOfferedTreatmentId() {
        return offeredTreatmentId;
    }

    public void setOfferedTreatmentId(Long offeredTreatmentId) {
        this.offeredTreatmentId = offeredTreatmentId;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
}
