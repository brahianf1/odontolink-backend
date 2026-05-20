package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;
import site.utnpf.odontolink.infrastructure.config.validation.MinimumAge;

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
 * <p>Semántica PATCH (RFC 5789): campos requeridos del modelo
 * ({@code email}, {@code firstName}, {@code lastName}) viajan como
 * {@code String} obligatorio para preservar las invariantes del dominio. Los
 * opcionales viajan envueltos en {@link JsonNullable}, de modo que el servicio
 * pueda distinguir tres estados distintos por campo:
 * <ul>
 *   <li>"undefined" (no presente en el JSON) → no tocar el valor existente;</li>
 *   <li>"present con null" → limpiar el campo;</li>
 *   <li>"present con valor" → sobreescribir.</li>
 * </ul>
 * Sin esta distinción, omitir un opcional en el payload borraría el dato
 * existente — bug observable por el usuario como "se me limpió el teléfono
 * al editar el nombre".
 */
@Schema(description = "Payload del autoservicio de actualización de perfil (RF06). Semántica PATCH.")
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
     * Teléfono opcional. Las anotaciones se aplican al tipo contenido vía
     * {@link site.utnpf.odontolink.infrastructure.config.validation.JsonNullableValueExtractor},
     * que permite a Bean Validation atravesar el wrapper.
     */
    @Schema(description = "Teléfono de contacto (omitir para no modificar; vacío para limpiar)",
            example = "3815234567")
    private JsonNullable<@Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
                        @Pattern(regexp = "^[0-9 +()-]*$",
                                 message = "El teléfono sólo puede contener dígitos, espacios y los símbolos + ( ) -")
                        String> phone = JsonNullable.undefined();

    @Schema(description = "Fecha de nacimiento (omitir para no modificar; null para limpiar). " +
            "Debe acreditar mayoría de edad (18+).",
            example = "1995-06-15")
    private JsonNullable<@Past(message = "La fecha de nacimiento debe ser una fecha pasada")
                        @MinimumAge(18)
                        LocalDate> birthDate = JsonNullable.undefined();

    @Schema(description = "Dirección postal (omitir para no modificar; vacío para limpiar)",
            example = "Av. Independencia 1234, San Miguel de Tucumán")
    private JsonNullable<@Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
                        String> address = JsonNullable.undefined();

    @Schema(description = "URL pública de la foto de perfil (omitir para no modificar; vacío para limpiar)",
            example = "https://cdn.odontolink/u/15/avatar.png")
    private JsonNullable<@Size(max = 512, message = "La URL de la foto no puede superar los 512 caracteres")
                        String> profilePictureUrl = JsonNullable.undefined();

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

    public JsonNullable<String> getPhone() {
        return phone;
    }

    public void setPhone(JsonNullable<String> phone) {
        this.phone = phone;
    }

    public JsonNullable<LocalDate> getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(JsonNullable<LocalDate> birthDate) {
        this.birthDate = birthDate;
    }

    public JsonNullable<String> getAddress() {
        return address;
    }

    public void setAddress(JsonNullable<String> address) {
        this.address = address;
    }

    public JsonNullable<String> getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(JsonNullable<String> profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
