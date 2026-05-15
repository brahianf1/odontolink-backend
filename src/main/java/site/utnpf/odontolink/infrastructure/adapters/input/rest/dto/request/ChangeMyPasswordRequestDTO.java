package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import site.utnpf.odontolink.infrastructure.config.validation.StrongPassword;

/**
 * DTO de entrada para que el usuario autenticado cambie su propia contraseña
 * desde el autoservicio del perfil (RF06).
 *
 * Es un flujo distinto del de recuperación pública (RF04): aquí el usuario
 * ya está autenticado y debe demostrar conocimiento de la contraseña actual
 * antes de poder rotarla. Esto mitiga ataques de tipo session-hijacking en
 * los que el atacante obtiene un token válido temporalmente y trata de
 * pivotar a una toma persistente de la cuenta.
 *
 * El tamaño mínimo de 6 caracteres se mantiene alineado con los DTOs de
 * registro y restablecimiento para no introducir asimetrías en la política
 * de contraseñas.
 */
@Schema(description = "Datos requeridos para que el usuario autenticado rote su contraseña (RF06)")
public class ChangeMyPasswordRequestDTO {

    @Schema(description = "Contraseña actual del usuario", example = "MiPass123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;

    @Schema(description = "Nueva contraseña a establecer (mínimo 8 caracteres; no puede ser una contraseña común)",
            example = "MiNuevaClave2026",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @StrongPassword
    private String newPassword;

    public ChangeMyPasswordRequestDTO() {
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
