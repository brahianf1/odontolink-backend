package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.application.port.in.dto.PractitionerCriterionChartQuery;
import site.utnpf.odontolink.application.port.in.dto.PractitionerRankingChartQuery;
import site.utnpf.odontolink.domain.model.PractitionerCriterionPerformance;
import site.utnpf.odontolink.domain.model.PractitionerRankingEntry;
import site.utnpf.odontolink.domain.model.User;

import java.util.List;

/**
 * Caso de uso: charts de performance de practicantes (panel docente, RF25 ext.).
 *
 * <p>Aplica el cerco silencioso supervisor→practicantes sobre el universo,
 * el umbral mínimo de muestras y el {@code topN} configurado o por default.
 */
public interface IPractitionerPerformanceUseCase {

    /**
     * Top-N practicantes por criterio puntual. Si {@code query.topN} viene
     * null se aplica el default del sistema.
     */
    PractitionerCriterionChartResult getTopByCriterion(PractitionerCriterionChartQuery query,
                                                       User supervisorUser);

    /**
     * Ranking combinado: promedio de promedios sobre criterios
     * {@code includeInRanking=true} para la dirección P→Pr.
     */
    PractitionerRankingChartResult getOverallRanking(PractitionerRankingChartQuery query,
                                                     User supervisorUser);

    /**
     * Resultado del chart top-N por criterio. Encapsula la lista junto al
     * threshold aplicado y el criterio mismo para que el adaptador REST no
     * tenga que reconsultar el catálogo.
     */
    final class PractitionerCriterionChartResult {
        private final String criterionCode;
        private final String criterionDisplayName;
        private final int minSamplesThreshold;
        private final List<PractitionerCriterionPerformance> entries;

        public PractitionerCriterionChartResult(String criterionCode,
                                                String criterionDisplayName,
                                                int minSamplesThreshold,
                                                List<PractitionerCriterionPerformance> entries) {
            this.criterionCode = criterionCode;
            this.criterionDisplayName = criterionDisplayName;
            this.minSamplesThreshold = minSamplesThreshold;
            this.entries = entries;
        }

        public String getCriterionCode() {
            return criterionCode;
        }

        public String getCriterionDisplayName() {
            return criterionDisplayName;
        }

        public int getMinSamplesThreshold() {
            return minSamplesThreshold;
        }

        public List<PractitionerCriterionPerformance> getEntries() {
            return entries;
        }
    }

    /**
     * Resultado del chart de ranking combinado. Trae la lista de criterios
     * usados para que el frontend pueda etiquetar la fórmula sin tener que
     * deducirla.
     */
    final class PractitionerRankingChartResult {
        private final List<CriterionLabel> criteriaUsed;
        private final int minSamplesThreshold;
        private final List<PractitionerRankingEntry> entries;

        public PractitionerRankingChartResult(List<CriterionLabel> criteriaUsed,
                                              int minSamplesThreshold,
                                              List<PractitionerRankingEntry> entries) {
            this.criteriaUsed = criteriaUsed;
            this.minSamplesThreshold = minSamplesThreshold;
            this.entries = entries;
        }

        public List<CriterionLabel> getCriteriaUsed() {
            return criteriaUsed;
        }

        public int getMinSamplesThreshold() {
            return minSamplesThreshold;
        }

        public List<PractitionerRankingEntry> getEntries() {
            return entries;
        }
    }

    /**
     * Identificador semántico de un criterio (code + displayName) para que
     * el adaptador REST no tenga que reconsultar el catálogo al armar el
     * payload del ranking.
     */
    final class CriterionLabel {
        private final String code;
        private final String displayName;

        public CriterionLabel(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
