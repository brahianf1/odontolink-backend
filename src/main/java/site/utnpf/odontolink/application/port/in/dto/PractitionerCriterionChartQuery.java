package site.utnpf.odontolink.application.port.in.dto;

import java.time.LocalDate;

/**
 * Comando inmutable que parametriza la query "top practicantes por criterio"
 * del panel docente.
 */
public final class PractitionerCriterionChartQuery {

    private final String criterionCode;
    private final Integer topN;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Long treatmentId;

    public PractitionerCriterionChartQuery(String criterionCode,
                                           Integer topN,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           Long treatmentId) {
        this.criterionCode = criterionCode;
        this.topN = topN;
        this.startDate = startDate;
        this.endDate = endDate;
        this.treatmentId = treatmentId;
    }

    public String getCriterionCode() {
        return criterionCode;
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
