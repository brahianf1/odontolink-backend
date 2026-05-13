package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO para que el usuario autenticado actualice su propio perfil (RF06).
 *
 * Sólo se exponen los campos cubiertos por el autoservicio. El DNI y el rol
 * permanecen fuera del payload porque son identificadores funcionales y
 * cambiarlos comprometería trazabilidad e identidad. La contraseña tiene su
 * propio endpoint ({@code PUT /api/users/me/password}) con verificación de la
 * contraseña actual.
 *
 * Decisión de validación: {@code firstName}, {@code lastName} y {@code email}
 * son obligatorios porque ya son obligatorios en el dominio (NOT NULL en la
 * tabla {@code users}). El resto son opcionales, con validaciones de formato
 * cuando estén presentes.
 */
@Schema(description = "Payload del autoservicio de actualización de perfil (RF06)")
public class UpdateMyProfileRequestDTO {

    @Schema(description = "Email de contacto y de login", example = "carlos.rodriguez@gmail.com", required = true)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    @Schema(description = "Nombre", example = "Carlos", required = true)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String firstName;

    @Schema(description = "Apellido", example = "Rodríguez", required = true)
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    private String lastName;

    /**
     * Teléfono opcional. Se valida con regex laxa (sólo dígitos, espacios y
     * '+') para no rechazar formatos internacionales legítimos: la validación
     * estricta corresponde al servicio de notificaciones, no a este endpoint.
     */
    @Schema(description = "Teléfono de contacto", example = "3815234567")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    @Pattern(regexp = "^[0-9 +()-]*$", message = "El teléfono sólo puede contener dígitos, espacios y los símbolos + ( ) -")
    private String phone;

    @Schema(description = "Fecha de nacimiento", example = "1995-06-15")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate birthDate;

    @Schema(description = "Dirección postal", example = "Av. Independencia 1234, San Miguel de Tucumán")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String address;

    @Schema(description = "URL pública de la foto de perfil", example = "https://cdn.odontolink/u/15/avatar.png")
    @Size(max = 512, message = "La URL de la foto no puede superar los 512 caracteres")
    private String profilePictureUrl;

    public UpdateMyProfileRequestDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
