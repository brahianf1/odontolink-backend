package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Vista de los datos rol-especificos del usuario autenticado.
 *
 * <p>Forma de "union DTO": un unico esquema que contiene todos los campos
 * de todos los roles. El campo {@code role} actua como discriminador y los
 * campos no aplicables al rol vienen como {@code null} (y se omiten del
 * JSON gracias a {@link JsonInclude}).
 *
 * <p>Ventajas vs. polimorfismo Jackson explicito:
 * <ul>
 *   <li>El frontend recibe un tipo unico predecible; narrowing por {@code role}
 *       en TypeScript es trivial.</li>
 *   <li>El OpenAPI generado expone un schema simple, sin discriminator complejo.</li>
 *   <li>Anadir un campo a un rol existente no requiere tocar la jerarquia.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos rol-especificos del usuario autenticado. Los campos no aplicables al rol se omiten.")
public class MyDetailsDTO {

    @Schema(description = "Identificador del usuario", example = "15")
    private Long userId;

    @Schema(description = "Rol del usuario", example = "ROLE_PATIENT")
    private String role;

    // ---- PATIENT ----

    @Schema(description = "Obra social o cobertura medica (solo PATIENT)", example = "OSDE")
    private String healthInsurance;

    @Schema(description = "Grupo sanguineo segun ABO/Rh (solo PATIENT)", example = "O+")
    private String bloodType;

    // ---- PRACTITIONER ----

    @Schema(description = "Numero de legajo universitario (solo PRACTITIONER, read-only)", example = "LEG-2024-001")
    private String studentId;

    @Schema(description = "Anio cursado (solo PRACTITIONER, read-only)", example = "4")
    private Integer studyYear;

    // ---- SUPERVISOR ----

    @Schema(description = "Especialidad (solo SUPERVISOR)", example = "Endodoncia")
    private String specialty;

    @Schema(description = "Legajo docente (solo SUPERVISOR, read-only)", example = "DOC-007")
    private String employeeId;

    public MyDetailsDTO() {
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getHealthInsurance() { return healthInsurance; }
    public void setHealthInsurance(String healthInsurance) { this.healthInsurance = healthInsurance; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Integer getStudyYear() { return studyYear; }
    public void setStudyYear(Integer studyYear) { this.studyYear = studyYear; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
}
