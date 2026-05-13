package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para la cancelación de un turno desde el lado del PRACTICANTE.
 *
 * El motivo es OBLIGATORIO: la cancelación de un practicante altera la
 * agenda del paciente y consume el cupo académico del estudiante, por lo
 * que se exige justificación tanto para mostrarla al paciente como para
 * dejar trazabilidad académica/supervisora.
 *
 * La validación se aplica en dos niveles para evitar mensajes inconsistentes:
 * - Bean Validation a nivel REST (mensajes claros para el cliente).
 * - El propio dominio re-valida en {@code Appointment.cancelByPractitioner},
 *   para que el contrato del agregado sea robusto incluso si se invoca
 *   desde otra capa.
 */
@Schema(description = "Cancelación de turno solicitada por el practicante; el motivo es obligatorio.")
public class CancelAppointmentByPractitionerRequestDTO {

    @Schema(description = "Motivo obligatorio de la cancelación informado por el practicante",
            example = "Inasistencia justificada del practicante por examen final.",
            required = true,
            maxLength = 1000)
    @NotBlank(message = "El motivo de cancelación es obligatorio cuando la cancela el practicante")
    @Size(max = 1000, message = "El motivo de cancelación no puede superar los 1000 caracteres")
    private String reason;

    public CancelAppointmentByPractitionerRequestDTO() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
