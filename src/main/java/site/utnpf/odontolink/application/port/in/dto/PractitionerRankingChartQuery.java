package site.utnpf.odontolink.application.port.in.dto;

import java.time.LocalDate;

/**
 * Comando inmutable para la query "ranking combinado de practicantes" (panel
 * docente). El criterio de ranking se deriva server-side de los
 * {@code FeedbackCriterion} activos con {@code includeInRanking=true}.
 */
public final class PractitionerRankingChartQuery {

    private final Integer topN;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Long treatmentId;

    public PractitionerRankingChartQuery(Integer topN,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         Long treatmentId) {
        this.topN = topN;
        this.startDate = startDate;
        this.endDate = endDate;
        this.treatmentId = treatmentId;
    }

    public Integer getTopN() {
        return topN;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Long getTreatmentId() {
        return treatmentId;
    }
}
