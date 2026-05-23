package site.utnpf.odontolink.application.port.in.dto;

import java.util.Objects;

/**
 * Comando inmutable que representa la puntuación entrante a un criterio
 * dentro de una encuesta de feedback. Lo construye el adaptador REST a
 * partir del DTO de request; el use case lo consume sin acoplarse al
 * mundo HTTP.
 */
public final class CriterionScoreInput {

    private final String criterionCode;
    private final int score;

    public CriterionScoreInput(String criterionCode, int score) {
        this.criterionCode = Objects.requireNonNull(criterionCode, "criterionCode");
        this.score = score;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public int getScore() {
        return score;
    }
}
