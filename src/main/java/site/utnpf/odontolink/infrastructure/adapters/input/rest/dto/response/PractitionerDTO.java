package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para información básica de un Practicante.
 * Utilizado en las APIs de supervisión académica.
 */
@Schema(description = "Información básica de un practicante")
public class PractitionerDTO {

    @Schema(description = "ID del practicante", example = "1")
    private Long id;

    @Schema(description = "Legajo del practicante", example = "48123")
    private String studentId;

    @Schema(description = "Año de cursado", example = "4")
    private Integer studyYear;

    @Schema(description = "Información del usuario asociado")
    private UserBasicDTO user;

    // Constructores
    public PractitionerDTO() {
    }

    public PractitionerDTO(Long id, String studentId, Integer studyYear, UserBasicDTO user) {
        this.id = id;
        this.studentId = studentId;
        this.studyYear = studyYear;
        this.user = user;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public UserBasicDTO getUser() {
        return user;
    }

    public void setUser(UserBasicDTO user) {
        this.user = user;
    }
}
