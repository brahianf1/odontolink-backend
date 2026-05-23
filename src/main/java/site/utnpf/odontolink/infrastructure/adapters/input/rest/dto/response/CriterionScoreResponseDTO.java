package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Score asignado a un criterio dentro del feedback.")
public class CriterionScoreResponseDTO {

    @Schema(description = "Código del criterio", example = "PUNCTUALITY")
    private String criterionCode;

    @Schema(description = "Nombre visible del criterio", example = "Puntualidad")
    private String criterionDisplayName;

    @Schema(description = "Puntuación 1–5", example = "5")
    private int score;

    public CriterionScoreResponseDTO() {
    }

    public CriterionScoreResponseDTO(String criterionCode, String criterionDisplayName, int score) {
        this.criterionCode = criterionCode;
        this.criterionDisplayName = criterionDisplayName;
        this.score = score;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public void setCriterionCode(String criterionCode) {
        this.criterionCode = criterionCode;
    }

    public String getCriterionDisplayName() {
        return criterionDisplayName;
    }

    public void setCriterionDisplayName(String criterionDisplayName) {
        this.criterionDisplayName = criterionDisplayName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
