package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request multi-criterio para crear feedback sobre una atención finalizada
 * (RF21/RF22/RF23). El paciente puntúa al practicante y viceversa con el
 * set de criterios definido por el catálogo (ver
 * {@code GET /api/feedback/criteria}).
 */
@Schema(description = "Encuesta multi-criterio sobre una atención finalizada.")
public class CreateFeedbackRequestDTO {

    @Schema(description = "ID de la atención", example = "23")
    @NotNull(message = "El ID de la atención es obligatorio")
    @Positive(message = "El ID de la atención debe ser positivo")
    private Long attentionId;

    @Schema(description = "Lista de scores. Debe cubrir exactamente los criterios activos para la dirección.")
    @NotEmpty(message = "Debe enviar al menos un score")
    @Size(max = 10, message = "Demasiados scores")
    @Valid
    private List<CriterionScoreInputDTO> scores;

    @Schema(description = "Comentario libre opcional", example = "Excelente atención")
    @Size(max = 1000, message = "El comentario no puede exceder los 1000 caracteres")
    private String comment;

    public CreateFeedbackRequestDTO() {
    }

    public Long getAttentionId() {
        return attentionId;
    }

    public void setAttentionId(Long attentionId) {
        this.attentionId = attentionId;
    }

    public List<CriterionScoreInputDTO> getScores() {
        return scores;
    }

    public void setScores(List<CriterionScoreInputDTO> scores) {
        this.scores = scores;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
