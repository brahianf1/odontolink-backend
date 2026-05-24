package site.utnpf.odontolink.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Value Object inmutable usado por el chart "ranking combinado de practicantes"
 * (RF25 ext.).
 *
 * <p>{@link #combinedAverage} es el promedio de los promedios por criterio
 * (cada criterio pesa igual, sin importar el sample size por criterio). El
 * mapa {@link #perCriterionAverages} (clave = code del criterio, valor =
 * promedio) acompaña al combinado para que el frontend pueda mostrar el
 * desglose sin nuevos round-trips.
 *
 * <p>{@link #feedbackCount} reporta el número de feedbacks distintos que el
 * practicante recibió y que contribuyen al cálculo: sirve para el umbral
 * mínimo de muestras y para que la UI muestre "n encuestas".
 */
public final class PractitionerRankingEntry {

    private final Long practitionerId;
    private final String practitionerName;
    private final double combinedAverage;
    private final Map<String, Double> perCriterionAverages;
    private final long feedbackCount;
    private final int rankPosition;

    public PractitionerRankingEntry(Long practitionerId,
                                    String practitionerName,
                                    double combinedAverage,
                                    Map<String, Double> perCriterionAverages,
                                    long feedbackCount,
                                    int rankPosition) {
        this.practitionerId = practitionerId;
        this.practitionerName = practitionerName;
        this.combinedAverage = combinedAverage;
        this.perCriterionAverages = perCriterionAverages == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(perCriterionAverages));
        this.feedbackCount = feedbackCount;
        this.rankPosition = rankPosition;
    }

    public Long getPractitionerId() {
        return practitionerId;
    }

    public String getPractitionerName() {
        return practitionerName;
    }

    public double getCombinedAverage() {
        return combinedAverage;
    }

    public Map<String, Double> getPerCriterionAverages() {
        return perCriterionAverages;
    }

    public long getFeedbackCount() {
        return feedbackCount;
    }

    public int getRankPosition() {
        return rankPosition;
    }
}
