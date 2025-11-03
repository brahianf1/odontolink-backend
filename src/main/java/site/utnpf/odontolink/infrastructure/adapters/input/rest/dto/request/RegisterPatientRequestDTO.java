package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * DTO para la solicitud de registro de un paciente.
 * Contiene todos los campos necesarios para crear un usuario y su perfil de paciente.
 */
@Schema(description = "Datos requeridos para registrar un nuevo paciente en el sistema")
public class RegisterPatientRequestDTO {

    @Schema(description = "Dirección de correo electrónico del paciente", example = "juan.perez@email.com", required = true)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @Schema(description = "Contraseña para la cuenta (mínimo 6 caracteres)", example = "miPassword123", required = true)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @Schema(description = "Nombre del paciente", example = "Juan", required = true)
    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @Schema(description = "Apellido del paciente", example = "Pérez", required = true)
    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    @Schema(description = "Documento Nacional de Identidad (7 u 8 dígitos)", example = "12345678", required = true)
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{7,8}$", message = "El DNI debe contener entre 7 y 8 dígitos")
    private String dni;

    @Schema(description = "Número de teléfono de contacto", example = "+54 9 11 1234-5678")
    private String phone;

    @Schema(description = "Fecha de nacimiento del paciente", example = "1990-05-15")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate birthDate;

    @Schema(description = "Obra social o cobertura médica del paciente", example = "OSDE")
    private String healthInsurance;

    @Schema(description = "Grupo sanguíneo del paciente", example = "O+")
    private String bloodType;

    // Constructores
    public RegisterPatientRequestDTO() {
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
