package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Payload PATCH para que un SUPERVISOR (docente) actualice sus datos
 * rol-especificos.
 *
 * <p>El {@code employeeId} (legajo docente) no aparece aqui porque es
 * inmutable desde autoservicio: identifica academicamente al docente y
 * cambiarlo desde un endpoint personal romperia trazabilidad.
 */
@Schema(description = "Payload PATCH para datos academicos del docente autenticado")
public class UpdateSupervisorDetailsRequestDTO {

    @Schema(description = "Especialidad odontologica (omitir para no modificar; vacio para limpiar)",
            example = "Endodoncia")
    private JsonNullable<@Size(max = 100, message = "La especialidad no puede superar los 100 caracteres") String>
            specialty = JsonNullable.undefined();

    public UpdateSupervisorDetailsRequestDTO() {
    }

    public JsonNullable<String> getSpecialty() { return specialty; }
    public void setSpecialty(JsonNullable<String> v) { this.specialty = v; }
}
