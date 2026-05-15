package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para la cancelación manual de un caso clínico por parte del practicante
 * responsable. Cubre el escenario en que el caso quedó IN_PROGRESS sin
 * próximos turnos pero con trabajo clínico previo (al menos un COMPLETED),
 * por lo que el cierre por abandono no aplica.
 *
 * El motivo es OBLIGATORIO: queda registrado como ProgressNote en el
 * expediente clínico del caso para trazabilidad académica y supervisora.
 *
 * La validación se aplica en dos niveles:
 * - Bean Validation a nivel REST (mensajes claros para el cliente).
 * - El POJO {@code Attention.cancelByPractitioner} re-valida para que el
 *   contrato del agregado sea robusto incluso desde otras capas.
 */
@Schema(description = "Cancelación manual de un caso clínico por el practicante; el motivo es obligatorio.")
public class CancelAttentionByPractitionerRequestDTO {

    @Schema(description = "Motivo obligatorio de la cancelación del caso clínico.",
            example = "El paciente no continúa el tratamiento tras la primera consulta.",
            required = true,
            maxLength = 1000)
    @NotBlank(message = "El motivo de cancelación del caso es obligatorio.")
    @Size(max = 1000, message = "El motivo de cancelación no puede superar los 1000 caracteres.")
    private String reason;

    public CancelAttentionByPractitionerRequestDTO() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
