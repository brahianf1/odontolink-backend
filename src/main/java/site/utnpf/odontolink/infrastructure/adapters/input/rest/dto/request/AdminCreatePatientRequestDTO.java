package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import site.utnpf.odontolink.infrastructure.config.validation.MinimumAge;

import java.time.LocalDate;

/**
 * DTO para que el administrador dé de alta un paciente (RF05).
 *
 * Las validaciones son idénticas a las del auto-registro: reutilizamos las
 * mismas reglas Bean Validation para que el alta administrativa no pueda
 * crear registros inconsistentes que se cuelen por una validación más laxa.
 */
@Schema(description = "Datos requeridos por el administrador para crear un paciente (RF05)")
public class AdminCreatePatientRequestDTO {

    @Schema(description = "Email del paciente", example = "carlos.rodriguez@gmail.com", required = true)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    @Schema(description = "Contraseña inicial (mínimo 6 caracteres)", example = "MiPass123!", required = true)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;

    @Schema(description = "Nombre", example = "Carlos", required = true)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String firstName;

    @Schema(description = "Apellido", example = "Rodríguez", required = true)
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    private String lastName;

    @Schema(description = "DNI (7 u 8 dígitos)", example = "35789456", required = true)
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{7,8}$", message = "El DNI debe contener entre 7 y 8 dígitos")
    private String dni;

    @Schema(description = "Teléfono de contacto", example = "3815234567")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String phone;

    @Schema(description = "Fecha de nacimiento. Debe acreditar mayoría de edad (18+).",
            example = "1995-06-15")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    @MinimumAge(18)
    private LocalDate birthDate;

    @Schema(description = "Obra social", example = "OSDE")
    @Size(max = 100, message = "La obra social no puede superar los 100 caracteres")
    private String healthInsurance;

    @Schema(
            description = "Grupo sanguineo segun el sistema ABO/Rh. Valores aceptados: A+, A-, B+, B-, AB+, AB-, O+, O-",
            example = "O+",
            allowableValues = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}
    )
    @Pattern(
            regexp = "^(A|B|AB|O)[+-]$",
            message = "El grupo sanguineo debe seguir el formato ABO/Rh (A+, A-, B+, B-, AB+, AB-, O+, O-)"
    )
    private String bloodType;

    public AdminCreatePatientRequestDTO() {
    }

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

    public String getHealthInsurance() {
        return healthInsurance;
    }

    public void setHealthInsurance(String healthInsurance) {
        this.healthInsurance = healthInsurance;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }
}
