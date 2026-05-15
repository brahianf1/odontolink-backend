package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Payload PATCH para que un PATIENT actualice sus datos clinicos
 * rol-especificos.
 *
 * <p>Semantica PATCH (ver {@code UpdateMyProfileRequestDTO}): campo ausente
 * = no tocar; presente con valor null o vacio = limpiar; presente con valor
 * = sobreescribir.
 */
@Schema(description = "Payload PATCH para datos clinicos del paciente autenticado")
public class UpdatePatientDetailsRequestDTO {

    @Schema(description = "Obra social o cobertura medica (omitir para no modificar; vacio para limpiar)",
            example = "OSDE")
    private JsonNullable<@Size(max = 100, message = "La obra social no puede superar los 100 caracteres") String>
            healthInsurance = JsonNullable.undefined();

    @Schema(description = "Grupo sanguineo segun ABO/Rh (omitir para no modificar; vacio para limpiar)",
            example = "O+",
            allowableValues = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"})
    private JsonNullable<@Pattern(regexp = "^$|^(A|B|AB|O)[+-]$",
                                  message = "El grupo sanguineo debe seguir el formato ABO/Rh (A+, A-, B+, B-, AB+, AB-, O+, O-)") String>
            bloodType = JsonNullable.undefined();

    public UpdatePatientDetailsRequestDTO() {
    }

    public JsonNullable<String> getHealthInsurance() { return healthInsurance; }
    public void setHealthInsurance(JsonNullable<String> v) { this.healthInsurance = v; }

    public JsonNullable<String> getBloodType() { return bloodType; }
    public void setBloodType(JsonNullable<String> v) { this.bloodType = v; }
}
