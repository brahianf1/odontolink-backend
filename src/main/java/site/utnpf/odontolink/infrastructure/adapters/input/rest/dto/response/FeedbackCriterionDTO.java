package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.FeedbackCriterion;

/**
 * Vista pública del catálogo de criterios para que el frontend renderice
 * la encuesta. NO expone {@code active}, {@code includeInRanking} ni metadata
 * técnica: el endpoint sólo devuelve criterios activos.
 */
@Schema(description = "Criterio del catálogo, listo para renderizar en la encuesta.")
public class FeedbackCriterionDTO {

    @Schema(example = "PUNCTUALITY", description = "Código machine-readable estable.")
    private String code;

    @Schema(example = "Puntualidad")
    private String displayName;

    @Schema(example = "Indica si el practicante respetó los horarios pactados.")
    private String description;

    @Schema(example = "1", description = "Orden visual sugerido (ascendente).")
    private int displayOrder;

    public FeedbackCriterionDTO() {
    }

    public FeedbackCriterionDTO(String code, String displayName, String description, int displayOrder) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public static FeedbackCriterionDTO fromDomain(FeedbackCriterion domain) {
        if (domain == null) {
            return null;
        }
        return new FeedbackCriterionDTO(
                domain.getCode(),
                domain.getDisplayName(),
                domain.getDescription(),
                domain.getDisplayOrder()
        );
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
