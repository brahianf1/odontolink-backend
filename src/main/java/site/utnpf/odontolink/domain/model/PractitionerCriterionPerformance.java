package site.utnpf.odontolink.domain.model;

/**
 * Value Object inmutable usado por el chart "top practicantes por criterio"
 * (RF25 ext.): representa la performance agregada de UN practicante sobre
 * UN único criterio dentro del universo filtrado.
 *
 * <p>{@link #rankPosition} es 1-based y lo asigna el caller al construir la
 * lista ordenada (no es responsabilidad del repositorio).
 */
public final class PractitionerCriterionPerformance {

    private final Long practitionerId;
    private final String practitionerName;
    private final String criterionCode;
    private final String criterionDisplayName;
    private final double averageScore;
    private final long feedbackCount;
    private final int rankPosition;

    public PractitionerCriterionPerformance(Long practitionerId,
                                            String practitionerName,
                                            String criterionCode,
                                            String criterionDisplayName,
                                            double averageScore,
                                            long feedbackCount,
                                            int rankPosition) {
        this.practitionerId = practitionerId;
        this.practitionerName = practitionerName;
        this.criterionCode = criterionCode;
        this.criterionDisplayName = criterionDisplayName;
        this.averageScore = averageScore;
        this.feedbackCount = feedbackCount;
        this.rankPosition = rankPosition;
    }

    public Long getPractitionerId() {
        return practitionerId;
    }

    public String getPractitionerName() {
        return practitionerName;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public String getCriterionDisplayName() {
        return criterionDisplayName;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public long getFeedbackCount() {
        return feedbackCount;
    }

    public int getRankPosition() {
        return rankPosition;
    }
}
