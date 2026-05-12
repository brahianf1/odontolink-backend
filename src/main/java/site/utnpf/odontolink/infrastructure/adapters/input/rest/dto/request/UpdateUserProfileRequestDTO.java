package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/**
 * DTO para que el administrador modifique los datos de perfil de un
 * usuario existente (RF05).
 *
 * Sólo se exponen los campos cuya modificación no compromete la unicidad
 * ni la trazabilidad: el email y el DNI permanecen inmutables desde esta
 * API porque son identificadores funcionales del sistema, y la contraseña
 * tiene su propio flujo de recuperación (RF04).
 */
@Schema(description = "Campos modificables del perfil de un usuario por parte del administrador (RF05)")
public class UpdateUserProfileRequestDTO {

    @Schema(description = "Nombre", example = "Carlos", required = true)
    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @Schema(description = "Apellido", example = "Rodríguez", required = true)
    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    @Schema(description = "Teléfono de contacto", example = "3815234567")
    private String phone;

    @Schema(description = "Fecha de nacimiento", example = "1995-06-15")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate birthDate;

    public UpdateUserProfileRequestDTO() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
