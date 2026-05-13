package site.utnpf.odontolink.application.port.in;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Comando inmutable que transporta los datos de actualización del perfil
 * desde la capa de adaptadores hacia el caso de uso (RF06).
 *
 * Se modela como tipo dedicado, en vez de pasar siete parámetros sueltos al
 * método de la interfaz, por dos razones:
 * <ul>
 *   <li>Legibilidad y evolución: agregar un nuevo campo del perfil no
 *       cambia la firma del puerto, sólo el contenido del comando.</li>
 *   <li>Inmutabilidad: el caso de uso recibe un snapshot estable de la
 *       intención del usuario y no puede mutarla durante la transacción.</li>
 * </ul>
 *
 * Las validaciones sintácticas (formato de email, longitud, presencia)
 * viven en el DTO REST con Bean Validation. Aquí sólo viajan los valores
 * ya saneados.
 */
public final class UpdateProfileCommand {

    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final LocalDate birthDate;
    private final String address;
    private final String profilePictureUrl;

    public UpdateProfileCommand(String email,
                                String firstName,
                                String lastName,
                                String phone,
                                LocalDate birthDate,
                                String address,
                                String profilePictureUrl) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.birthDate = birthDate;
        this.address = address;
        this.profilePictureUrl = profilePictureUrl;
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

    public String getPhone() {
        return phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }

    public String getProfilePictureUrl() {
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
