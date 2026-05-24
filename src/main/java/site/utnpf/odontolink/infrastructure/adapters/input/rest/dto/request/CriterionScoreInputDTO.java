package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Score entrante en un criterio de la encuesta. El frontend obtiene los
 * códigos desde {@code GET /api/feedback/criteria}.
 */
@Schema(description = "Puntuación 1–5 sobre un criterio del catálogo de feedback.")
public class CriterionScoreInputDTO {

    @Schema(description = "Código del criterio (catálogo). Ej: PUNCTUALITY", example = "PUNCTUALITY")
    @NotBlank(message = "criterionCode es obligatorio")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,39}$",
            message = "criterionCode debe ser MAYÚSCULAS con guiones bajos, 3–40 chars")
    private String criterionCode;

    @Schema(description = "Puntuación 1–5", example = "5")
    @NotNull(message = "score es obligatorio")
    @Min(value = 1, message = "score mínimo 1")
    @Max(value = 5, message = "score máximo 5")
    private Integer score;

    public CriterionScoreInputDTO() {
    }

    public CriterionScoreInputDTO(String criterionCode, Integer score) {
        this.criterionCode = criterionCode;
        this.score = score;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public void setCriterionCode(String criterionCode) {
        this.criterionCode = criterionCode;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
