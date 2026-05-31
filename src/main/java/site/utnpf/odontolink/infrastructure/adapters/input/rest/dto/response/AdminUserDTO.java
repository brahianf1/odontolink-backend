package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO de respuesta para el panel de administración (RF05).
 *
 * Expone los datos comunes del usuario y su estado de activación, sin
 * incluir información sensible como la contraseña. El rol se expone como
 * string para que el frontend pueda renderizar etiquetas sin acoplarse a
 * los nombres internos del enum.
 */
@Schema(description = "Vista de usuario destinada al panel administrativo")
public class AdminUserDTO {

    @Schema(description = "Identificador del usuario", example = "15")
    private Long id;

    @Schema(description = "Email", example = "carlos.rodriguez@gmail.com")
    private String email;

    @Schema(description = "Rol", example = "ROLE_PATIENT")
    private String role;

    @Schema(description = "Indica si el usuario está activo", example = "true")
    private boolean isActive;

    @Schema(description = "Nombre", example = "Carlos")
    private String firstName;

    @Schema(description = "Apellido", example = "Rodríguez")
    private String lastName;

    @Schema(description = "DNI", example = "35789456")
    private String dni;

    @Schema(description = "Teléfono", example = "3815234567")
    private String phone;

    @Schema(description = "Fecha de nacimiento", example = "1995-06-15")
    private LocalDate birthDate;

    @Schema(description = "URL pública de la foto de perfil. Puede ser null si el usuario no tiene foto.",
            example = "https://cdn.odontolink/u/15/avatar.jpg", nullable = true)
    private String profilePictureUrl;

    @Schema(description = "Fecha de creación de la cuenta", example = "2025-08-12T19:34:21Z")
    private Instant createdAt;

    public AdminUserDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
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

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
