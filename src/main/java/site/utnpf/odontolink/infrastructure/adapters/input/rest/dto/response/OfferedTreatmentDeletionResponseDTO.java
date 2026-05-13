package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.OfferedTreatmentDeletionResult;

/**
 * DTO de respuesta para el endpoint DELETE del catálogo personal del practicante.
 *
 * Informa al frontend qué decisión tomó el Dominio respecto a la eliminación
 * (Baja Lógica vs Baja Física) y la razón asociada, para que el usuario reciba
 * un feedback accionable en lugar de un genérico {@code 204 No Content}.
 *
 * Esto es clave para la UX de RF16: el practicante necesita saber que su
 * oferta ya no está visible pero permanece como histórico ligado a turnos.
 */
@Schema(description = "Resultado de la eliminación de una oferta del catálogo personal")
public class OfferedTreatmentDeletionResponseDTO {

    @Schema(description = "Tipo de eliminación aplicada por el Dominio",
            example = "SOFT_DELETED",
            allowableValues = {"SOFT_DELETED", "HARD_DELETED"})
    private String outcome;

    @Schema(description = "Explicación del Dominio sobre la decisión tomada",
            example = "La oferta se desactivó: existen turnos agendados a futuro. Se conserva la integridad referencial de las citas ya otorgadas. Ya no aparecerá en el catálogo público.")
    private String message;

    public OfferedTreatmentDeletionResponseDTO() {
    }

    public OfferedTreatmentDeletionResponseDTO(String outcome, String message) {
        this.outcome = outcome;
        this.message = message;
    }

    public static OfferedTreatmentDeletionResponseDTO from(OfferedTreatmentDeletionResult result) {
        return new OfferedTreatmentDeletionResponseDTO(
                result.getOutcome().name(),
                result.getReason()
        );
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
