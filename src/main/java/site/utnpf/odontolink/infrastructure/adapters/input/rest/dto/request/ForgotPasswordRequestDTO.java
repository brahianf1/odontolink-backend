package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para iniciar el flujo de recuperación de contraseña (RF04).
 *
 * Sólo recibe el email: el servicio responderá con la misma representación
 * independientemente de si el email existe o no, para evitar enumeración
 * de cuentas.
 */
@Schema(description = "Datos requeridos para solicitar el inicio del flujo de recuperación de contraseña")
public class ForgotPasswordRequestDTO {

    @Schema(description = "Email asociado a la cuenta cuyo acceso se desea recuperar",
            example = "juan.perez@email.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    public ForgotPasswordRequestDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
