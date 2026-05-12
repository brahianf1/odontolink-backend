package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para confirmar el restablecimiento de contraseña (RF04).
 *
 * Recibe el token emitido por el sistema y la nueva contraseña en claro. La
 * regla de longitud mínima se mantiene alineada con los DTOs de registro
 * existentes para no introducir asimetrías de política de contraseñas.
 */
@Schema(description = "Datos requeridos para confirmar el restablecimiento de contraseña")
public class ResetPasswordRequestDTO {

    @Schema(description = "Token de recuperación recibido por correo",
            example = "u3X9rL8b3MpsjyJSjGv3p1cFhX0xkX9Yr_eN1Iy1dwo",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El token es obligatorio")
    private String token;

    @Schema(description = "Nueva contraseña a establecer (mínimo 6 caracteres)",
            example = "MiNuevaPass123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    private String newPassword;

    public ResetPasswordRequestDTO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
