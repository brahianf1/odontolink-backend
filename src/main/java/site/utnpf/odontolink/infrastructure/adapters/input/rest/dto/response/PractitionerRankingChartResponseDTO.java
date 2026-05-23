package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.application.port.in.IPractitionerPerformanceUseCase.PractitionerRankingChartResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Schema(description = "Ranking combinado de practicantes (promedio de promedios sobre criterios de ranking).")
public class PractitionerRankingChartResponseDTO {

    @Schema(description = "Criterios que entran en la fórmula del ranking.")
    private List<CriterionLabelDTO> criteriaUsed;

    @Schema(description = "Umbral mínimo de feedbacks para aparecer en el ranking.", example = "3")
    private int minSamplesThreshold;

    @Schema(description = "Entradas ordenadas DESC por combinedAverage.")
    private List<EntryDTO> entries;

    public PractitionerRankingChartResponseDTO() {
    }

    public static PractitionerRankingChartResponseDTO fromResult(PractitionerRankingChartResult result) {
        PractitionerRankingChartResponseDTO dto = new PractitionerRankingChartResponseDTO();
        dto.setCriteriaUsed(result.getCriteriaUsed() == null
                ? Collections.emptyList()
                : result.getCriteriaUsed().stream()
                        .map(c -> new CriterionLabelDTO(c.getCode(), c.getDisplayName()))
                        .collect(Collectors.toList()));
        dto.setMinSamplesThreshold(result.getMinSamplesThreshold());
        dto.setEntries(result.getEntries() == null
                ? Collections.emptyList()
                : result.getEntries().stream()
                        .map(e -> {
                            Map<String, Double> rounded = new LinkedHashMap<>();
                            e.getPerCriterionAverages().forEach((k, v) -> rounded.put(k, round2(v)));
                            return new EntryDTO(
                                    e.getPractitionerId(),
                                    e.getPractitionerName(),
                                    round2(e.getCombinedAverage()),
                                    rounded,
                                    e.getFeedbackCount(),
                                    e.getRankPosition());
                        })
                        .collect(Collectors.toList()));
        return dto;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public List<CriterionLabelDTO> getCriteriaUsed() {
        return criteriaUsed;
    }

    public void setCriteriaUsed(List<CriterionLabelDTO> criteriaUsed) {
        this.criteriaUsed = criteriaUsed;
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

    @Schema(description = "Entrada individual del ranking.")
    public static class EntryDTO {
        @Schema(example = "8")
        private Long practitionerId;
        @Schema(example = "Ana Martínez")
        private String practitionerName;
        @Schema(description = "Promedio de promedios sobre los criterios usados.",
                example = "4.65")
        private double combinedAverage;
        @Schema(description = "Promedios por criterio (clave = code).",
                example = "{\"PUNCTUALITY\": 4.83, \"CARE_QUALITY\": 4.5, \"COMMUNICATION_CLARITY\": 4.62}")
        private Map<String, Double> perCriterionAverages;
        @Schema(example = "12")
        private long feedbackCount;
        @Schema(example = "1")
        private int rankPosition;

        public EntryDTO() {
        }

        public EntryDTO(Long practitionerId, String practitionerName, double combinedAverage,
                        Map<String, Double> perCriterionAverages, long feedbackCount, int rankPosition) {
            this.practitionerId = practitionerId;
            this.practitionerName = practitionerName;
            this.combinedAverage = combinedAverage;
            this.perCriterionAverages = perCriterionAverages;
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

        public double getCombinedAverage() {
            return combinedAverage;
        }

        public void setCombinedAverage(double combinedAverage) {
            this.combinedAverage = combinedAverage;
        }

        public Map<String, Double> getPerCriterionAverages() {
            return perCriterionAverages;
        }

        public void setPerCriterionAverages(Map<String, Double> perCriterionAverages) {
            this.perCriterionAverages = perCriterionAverages;
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
