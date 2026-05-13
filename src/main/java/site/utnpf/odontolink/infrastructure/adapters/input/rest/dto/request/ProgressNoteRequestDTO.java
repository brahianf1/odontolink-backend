package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de registro de una nota de progreso (evolución).
 * Implementa RF11 - CU 4.2: Registrar Evolución.
 *
 * Este DTO captura la "intención" del practicante de añadir una evolución al caso clínico.
 * Solo contiene el contenido de la nota, ya que el autor se obtiene del usuario autenticado.
 *
 * @author OdontoLink Team
 */
@Schema(description = "Solicitud para registrar una nota de evolución (RF11). El autor se infiere del " +
        "JWT y no se envía en el body.")
public class ProgressNoteRequestDTO {

    /**
     * Contenido de la nota de progreso (evolución).
     * Describe el progreso del tratamiento o cualquier observación relevante.
     */
    @Schema(description = "Texto libre de la evolución clínica. Mínimo 10 y máximo 5000 caracteres.",
            example = "Se realiza profilaxis completa. Paciente tolera bien el procedimiento. Se indica " +
                    "control en 6 meses y refuerzo de técnica de cepillado.",
            minLength = 10,
            maxLength = 5000,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El contenido de la nota es obligatorio")
    @Size(min = 10, max = 5000, message = "La nota debe tener entre 10 y 5000 caracteres")
    private String content;

    // Constructores
    public ProgressNoteRequestDTO() {
    }

    public ProgressNoteRequestDTO(String content) {
        this.content = content;
    }

    // Getters y Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
