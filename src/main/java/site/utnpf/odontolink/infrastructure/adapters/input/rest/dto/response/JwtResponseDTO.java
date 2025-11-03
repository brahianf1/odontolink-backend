package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para operaciones de autenticación.
 * Contiene el token JWT y la información básica del usuario.
 */
@Schema(description = "Respuesta de autenticación exitosa con token JWT y datos del usuario")
public class JwtResponseDTO {

    @Schema(description = "Token JWT para autenticación en endpoints protegidos", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Tipo de token", example = "Bearer", defaultValue = "Bearer")
    private String type = "Bearer";

    @Schema(description = "Identificador único del usuario", example = "1")
    private Long userId;

    @Schema(description = "Correo electrónico del usuario", example = "juan.perez@email.com")
    private String email;

    @Schema(description = "Rol del usuario en el sistema", example = "PATIENT")
    private String role;

    @Schema(description = "Nombre del usuario", example = "Juan")
    private String firstName;

    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String lastName;

    // Constructores
    public JwtResponseDTO() {
    }

    public JwtResponseDTO(String token, Long userId, String email, String role, String firstName, String lastName) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters y Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
}
