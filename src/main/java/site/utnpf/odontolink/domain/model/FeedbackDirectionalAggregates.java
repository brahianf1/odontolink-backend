package site.utnpf.odontolink.domain.model;

/**
 * Value Object inmutable que encapsula los agregados del Panel Docente de
 * Feedback (RF25) discriminados por dirección.
 *
 * <p>Reemplaza al esquema previo que devolvía un único {@code averageRating}
 * mezclando los dos sentidos del feedback bidireccional. La separación es
 * obligatoria porque el promedio "patient→practitioner" mide el desempeño
 * del estudiante (señal del panel docente), mientras que el
 * "practitioner→patient" mide al paciente — agregarlos en una sola media
 * contamina la métrica de evaluación.
 *
 * <p>Los counts viajan junto a los promedios para que el frontend pueda:
 * <ul>
 *   <li>Decidir si mostrar el indicador (count 0 → ocultar o "Sin datos").</li>
 *   <li>Recomputar combinaciones derivadas sin extra round-trips
 *       ({@code totalGlobal = countP2P + countPra2Pat}).</li>
 * </ul>
 *
 * <p>Cuando el universo filtrado no contiene feedbacks de una dirección, su
 * promedio se reporta como {@code 0.0} y el count como {@code 0L} — contrato
 * estable que evita tener que defender contra {@code null}/{@code NaN}.
 */
public final class FeedbackDirectionalAggregates {

    private final double averageRatingPatientToPractitioner;
    private final long totalPatientToPractitioner;
    private final double averageRatingPractitionerToPatient;
    private final long totalPractitionerToPatient;

    public FeedbackDirectionalAggregates(double averageRatingPatientToPractitioner,
                                         long totalPatientToPractitioner,
                                         double averageRatingPractitionerToPatient,
                                         long totalPractitionerToPatient) {
        this.averageRatingPatientToPractitioner = averageRatingPatientToPractitioner;
        this.totalPatientToPractitioner = totalPatientToPractitioner;
        this.averageRatingPractitionerToPatient = averageRatingPractitionerToPatient;
        this.totalPractitionerToPatient = totalPractitionerToPatient;
    }

    /**
     * Agregados con todos los valores en cero. Útil para el atajo "supervisor
     * sin practicantes a cargo" sin tener que tocar la base.
     */
    public static FeedbackDirectionalAggregates empty() {
        return new FeedbackDirectionalAggregates(0.0, 0L, 0.0, 0L);
    }

    public double getAverageRatingPatientToPractitioner() {
        return averageRatingPatientToPractitioner;
    }

    public long getTotalPatientToPractitioner() {
        return totalPatientToPractitioner;
    }

    public double getAverageRatingPractitionerToPatient() {
        return averageRatingPractitionerToPatient;
    }

    public long getTotalPractitionerToPatient() {
        return totalPractitionerToPatient;
    }
}
