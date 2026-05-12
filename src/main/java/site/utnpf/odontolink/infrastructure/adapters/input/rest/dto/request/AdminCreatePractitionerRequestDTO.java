package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * DTO para que el administrador dé de alta un practicante (RF05).
 *
 * Las validaciones son idénticas a las del auto-registro de practicantes
 * para evitar discrepancias entre los caminos público y administrativo.
 */
@Schema(description = "Datos requeridos por el administrador para crear un practicante (RF05)")
public class AdminCreatePractitionerRequestDTO {

    @Schema(description = "Email institucional del practicante", example = "ana.martinez@fodo.unt.edu.ar", required = true)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @Schema(description = "Contraseña inicial (mínimo 6 caracteres)", example = "Segura456!", required = true)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @Schema(description = "Nombre", example = "Ana", required = true)
    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @Schema(description = "Apellido", example = "Martínez", required = true)
    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    @Schema(description = "DNI (7 u 8 dígitos)", example = "38456123", required = true)
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{7,8}$", message = "El DNI debe contener entre 7 y 8 dígitos")
    private String dni;

    @Schema(description = "Teléfono de contacto", example = "3816789012")
    private String phone;

    @Schema(description = "Fecha de nacimiento", example = "1998-03-20")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate birthDate;

    @Schema(description = "Legajo universitario", example = "48765", required = true)
    @NotBlank(message = "El legajo es obligatorio")
    private String studentId;

    @Schema(description = "Año de cursado (1 a 6)", example = "4", required = true, minimum = "1", maximum = "6")
    @NotNull(message = "El año cursado es obligatorio")
    @Min(value = 1, message = "El año cursado debe ser mayor o igual a 1")
    @Max(value = 6, message = "El año cursado debe ser menor o igual a 6")
    private Integer studyYear;

    public AdminCreatePractitionerRequestDTO() {
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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Integer getStudyYear() {
        return studyYear;
    }

    public void setStudyYear(Integer studyYear) {
        this.studyYear = studyYear;
    }
}
