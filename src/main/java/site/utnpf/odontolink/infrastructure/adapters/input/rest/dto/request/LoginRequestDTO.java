package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para la solicitud de inicio de sesión.
 */
@Schema(description = "Datos requeridos para iniciar sesión en el sistema")
public class LoginRequestDTO {

    @Schema(description = "Dirección de correo electrónico del usuario", example = "juan.perez@email.com", required = true)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @Schema(description = "Contraseña de la cuenta", example = "miPassword123", required = true)
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // Constructores
    public LoginRequestDTO() {
    }

    // Getters y Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
