package site.utnpf.odontolink.application.port.in;

import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Comando inmutable que transporta los datos de actualización del perfil
 * desde la capa de adaptadores hacia el caso de uso (RF06).
 *
 * <p>Semántica PATCH:
 * <ul>
 *   <li>Los campos requeridos del modelo ({@code email}, {@code firstName},
 *       {@code lastName}) viajan como tipos planos: el adaptador REST
 *       garantiza que estén presentes via {@code @NotBlank}.</li>
 *   <li>Los opcionales viajan envueltos en {@link JsonNullable} para que el
 *       caso de uso pueda distinguir tres estados:
 *       <ul>
 *         <li>{@code JsonNullable.undefined()} → no tocar.</li>
 *         <li>{@code JsonNullable.of(null)} o {@code of("")} → limpiar.</li>
 *         <li>{@code JsonNullable.of(value)} → sobreescribir.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>El uso de {@code JsonNullable} en la capa de aplicación es un trade-off
 * deliberado: la librería es un wrapper utility (no framework runtime) y
 * preserva exactamente la semántica que necesitamos. Crear un wrapper propio
 * sería duplicar trabajo sin ganancia sustantiva.
 */
public final class UpdateProfileCommand {

    private final String email;
    private final String firstName;
    private final String lastName;
    private final JsonNullable<String> phone;
    private final JsonNullable<LocalDate> birthDate;
    private final JsonNullable<String> address;
    private final JsonNullable<String> profilePictureUrl;

    public UpdateProfileCommand(String email,
                                String firstName,
                                String lastName,
                                JsonNullable<String> phone,
                                JsonNullable<LocalDate> birthDate,
                                JsonNullable<String> address,
                                JsonNullable<String> profilePictureUrl) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone != null ? phone : JsonNullable.undefined();
        this.birthDate = birthDate != null ? birthDate : JsonNullable.undefined();
        this.address = address != null ? address : JsonNullable.undefined();
        this.profilePictureUrl = profilePictureUrl != null ? profilePictureUrl : JsonNullable.undefined();
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public JsonNullable<String> getPhone() {
        return phone;
    }

    public JsonNullable<LocalDate> getBirthDate() {
        return birthDate;
    }

    public JsonNullable<String> getAddress() {
        return address;
    }

    public JsonNullable<String> getProfilePictureUrl() {
        return profilePictureUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateProfileCommand)) return false;
        UpdateProfileCommand that = (UpdateProfileCommand) o;
        return Objects.equals(email, that.email)
                && Objects.equals(firstName, that.firstName)
                && Objects.equals(lastName, that.lastName)
                && Objects.equals(phone, that.phone)
                && Objects.equals(birthDate, that.birthDate)
                && Objects.equals(address, that.address)
                && Objects.equals(profilePictureUrl, that.profilePictureUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, firstName, lastName, phone, birthDate, address, profilePictureUrl);
    }
}
