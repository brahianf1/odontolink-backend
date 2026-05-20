package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import site.utnpf.odontolink.infrastructure.config.validation.MinimumAge;

import java.time.LocalDate;

/**
 * DTO para que el administrador dé de alta un supervisor/docente (RF05).
 *
 * Las validaciones replican las del auto-registro de supervisores para
 * mantener una sola fuente de verdad sobre las reglas de alta.
 */
@Schema(description = "Datos requeridos por el administrador para crear un supervisor/docente (RF05)")
public class AdminCreateSupervisorRequestDTO {

    @Schema(description = "Email institucional del docente", example = "horacio.gomez@fodo.unt.edu.ar", required = true)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    @Schema(description = "Contraseña inicial (mínimo 6 caracteres)", example = "Docente789!", required = true)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;

    @Schema(description = "Nombre", example = "Horacio", required = true)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String firstName;

    @Schema(description = "Apellido", example = "Gómez", required = true)
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    private String lastName;

    @Schema(description = "DNI (7 u 8 dígitos)", example = "23456789", required = true)
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{7,8}$", message = "El DNI debe contener entre 7 y 8 dígitos")
    private String dni;

    @Schema(description = "Teléfono de contacto", example = "3815678901")
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String phone;

    @Schema(description = "Fecha de nacimiento. Debe acreditar mayoría de edad (18+).",
            example = "1980-04-10")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    @MinimumAge(18)
    private LocalDate birthDate;

    @Schema(description = "Especialidad del docente", example = "Ortodoncia", required = true)
    @NotBlank(message = "La especialidad es obligatoria")
    @Size(max = 100, message = "La especialidad no puede superar los 100 caracteres")
    private String specialty;

    @Schema(description = "Legajo docente", example = "DOC-2024-001", required = true)
    @NotBlank(message = "El legajo docente es obligatorio")
    @Size(max = 50, message = "El legajo docente no puede superar los 50 caracteres")
    private String employeeId;

    public AdminCreateSupervisorRequestDTO() {
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

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
}
