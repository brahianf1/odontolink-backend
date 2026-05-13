package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * DTO para la cancelación de un turno desde el lado del PACIENTE.
 *
 * El motivo es deliberadamente OPCIONAL: el modelo de negocio no obliga al
 * paciente a justificarse para cancelar. Si lo informa, se persiste como
 * señal cualitativa para el funnel de deserción; si no, el turno queda
 * cancelado sin texto y la falta de motivo es información en sí misma.
 *
 * Se acota el tamaño máximo para evitar payloads abusivamente largos sin
 * empujar al usuario a respuestas formales.
 */
@Schema(description = "Cancelación de turno solicitada por el paciente; el motivo es opcional.")
public class CancelAppointmentByPatientRequestDTO {

    @Schema(description = "Motivo opcional de la cancelación informado por el paciente",
            example = "Me surgió un imprevisto y no puedo asistir.",
            maxLength = 1000)
    @Size(max = 1000, message = "El motivo de cancelación no puede superar los 1000 caracteres")
    private String reason;

    public CancelAppointmentByPatientRequestDTO() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
