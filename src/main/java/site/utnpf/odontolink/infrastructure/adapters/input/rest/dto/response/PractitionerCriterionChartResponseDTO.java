package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.application.port.in.IPractitionerPerformanceUseCase.PractitionerCriterionChartResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "Chart top-N de practicantes por un criterio puntual.")
public class PractitionerCriterionChartResponseDTO {

    @Schema(description = "Criterio sobre el que se computa el top-N.")
    private CriterionLabelDTO criterion;

    @Schema(description = "Umbral mínimo de feedbacks para aparecer en el chart.",
            example = "3")
    private int minSamplesThreshold;

    @Schema(description = "Entradas ordenadas DESC por average.")
    private List<EntryDTO> entries;

    public PractitionerCriterionChartResponseDTO() {
    }

    public static PractitionerCriterionChartResponseDTO fromResult(PractitionerCriterionChartResult result) {
        PractitionerCriterionChartResponseDTO dto = new PractitionerCriterionChartResponseDTO();
        dto.setCriterion(new CriterionLabelDTO(result.getCriterionCode(), result.getCriterionDisplayName()));
        dto.setMinSamplesThreshold(result.getMinSamplesThreshold());
        dto.setEntries(result.getEntries() == null
                ? Collections.emptyList()
                : result.getEntries().stream()
                        .map(e -> new EntryDTO(
                                e.getPractitionerId(),
                                e.getPractitionerName(),
                                round2(e.getAverageScore()),
                                e.getFeedbackCount(),
                                e.getRankPosition()))
                        .collect(Collectors.toList()));
        return dto;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public CriterionLabelDTO getCriterion() {
        return criterion;
    }

    public void setCriterion(CriterionLabelDTO criterion) {
        this.criterion = criterion;
    }

    public int getMinSamplesThreshold() {
        return minSamplesThreshold;
    }

    public void setMinSamplesThreshold(int minSamplesThreshold) {
        this.minSamplesThreshold = minSamplesThreshold;
    }

    public List<EntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryDTO> entries) {
        this.entries = entries;
    }

    @Schema(description = "Entrada individual de un practicante en el chart.")
    public static class EntryDTO {
        @Schema(example = "8")
        private Long practitionerId;
        @Schema(example = "Ana Martínez")
        private String practitionerName;
        @Schema(example = "4.83")
        private double average;
        @Schema(example = "12")
        private long feedbackCount;
        @Schema(example = "1")
        private int rankPosition;

        public EntryDTO() {
        }

        public EntryDTO(Long practitionerId, String practitionerName, double average,
                        long feedbackCount, int rankPosition) {
            this.practitionerId = practitionerId;
            this.practitionerName = practitionerName;
            this.average = average;
            this.feedbackCount = feedbackCount;
            this.rankPosition = rankPosition;
        }

        public Long getPractitionerId() {
            return practitionerId;
        }

        public void setPractitionerId(Long practitionerId) {
            this.practitionerId = practitionerId;
        }

        public String getPractitionerName() {
            return practitionerName;
        }

        public void setPractitionerName(String practitionerName) {
            this.practitionerName = practitionerName;
        }

        public double getAverage() {
            return average;
        }

        public void setAverage(double average) {
            this.average = average;
        }

        public long getFeedbackCount() {
            return feedbackCount;
        }

        public void setFeedbackCount(long feedbackCount) {
            this.feedbackCount = feedbackCount;
        }

        public int getRankPosition() {
            return rankPosition;
        }

        public void setRankPosition(int rankPosition) {
            this.rankPosition = rankPosition;
        }
    }
}
