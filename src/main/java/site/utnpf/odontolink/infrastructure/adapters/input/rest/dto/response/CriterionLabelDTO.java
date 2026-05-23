package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Identificador semántico de un criterio (code + displayName).")
public class CriterionLabelDTO {

    @Schema(example = "PUNCTUALITY")
    private String code;

    @Schema(example = "Puntualidad")
    private String displayName;

    public CriterionLabelDTO() {
    }

    public CriterionLabelDTO(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
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
}
