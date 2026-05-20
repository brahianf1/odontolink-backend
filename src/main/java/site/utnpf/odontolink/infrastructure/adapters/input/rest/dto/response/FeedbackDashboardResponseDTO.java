package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.FeedbackDashboardResult;
import site.utnpf.odontolink.domain.model.FeedbackDirectionalAggregates;

/**
 * "DashboardWrapper" del Panel Docente de Supervisión de Feedback (RF25).
 *
 * <p>Un panel analítico no es un array de filas: es un objeto contenedor que
 * agrupa el listado paginado y los indicadores macro calculados sobre el
 * mismo universo de filtros. Este DTO formaliza ese contrato hacia el
 * frontend.
 *
 * <p><b>Migración v2:</b> el campo único {@code averageRating} fue
 * reemplazado por dos pares (promedio + total) discriminados por la
 * dirección del feedback bidireccional. La métrica que evalúa al
 * practicante (P→Pr) y la que evalúa al paciente (Pra→Pat) ahora viajan
 * por separado para que el frontend pueda elegir cuál mostrar según el
 * contexto. La media global anterior mezclaba ambos sentidos y
 * distorsionaba el desempeño del estudiante; se eliminó deliberadamente.
 *
 * <p>Campos:
 *  - {@code feedbacks}: slice paginada (contenido + metadata de paginación).
 *    Si el cliente envió {@code direction=...}, la lista viene filtrada por
 *    esa dirección; los agregados de abajo se calculan SIEMPRE sobre los dos
 *    sentidos para que el dashboard pueda mostrar ambas tarjetas.
 *  - {@code averageRatingPatientToPractitioner} / {@code totalPatientToPractitioner}:
 *    promedio y total de feedbacks emitidos por pacientes calificando al
 *    practicante (la métrica de soft-skills/RF21 para el panel docente).
 *  - {@code averageRatingPractitionerToPatient} / {@code totalPractitionerToPatient}:
 *    promedio y total de feedbacks emitidos por practicantes calificando al
 *    paciente (RF22).
 */
@Schema(description = "Contenedor del Panel Docente de Supervisión de Feedback (RF25) v2. " +
        "Incluye la lista paginada y los agregados discriminados por dirección del feedback bidireccional.")
public class FeedbackDashboardResponseDTO {

    @Schema(description = "Página de feedbacks (contenido + metadata de paginación). " +
            "Si se envió `direction`, viene filtrada por esa dirección.")
    private PageResponseDTO<FeedbackResponseDTO> feedbacks;

    @Schema(description = "Promedio de calificación de feedbacks paciente→practicante sobre el universo filtrado",
            example = "4.27")
    private double averageRatingPatientToPractitioner;

    @Schema(description = "Cantidad total de feedbacks paciente→practicante en el universo filtrado",
            example = "92")
    private long totalPatientToPractitioner;

    @Schema(description = "Promedio de calificación de feedbacks practicante→paciente sobre el universo filtrado",
            example = "4.05")
    private double averageRatingPractitionerToPatient;

    @Schema(description = "Cantidad total de feedbacks practicante→paciente en el universo filtrado",
            example = "45")
    private long totalPractitionerToPatient;

    public FeedbackDashboardResponseDTO() {
    }

    public FeedbackDashboardResponseDTO(PageResponseDTO<FeedbackResponseDTO> feedbacks,
                                        double averageRatingPatientToPractitioner,
                                        long totalPatientToPractitioner,
                                        double averageRatingPractitionerToPatient,
                                        long totalPractitionerToPatient) {
        this.feedbacks = feedbacks;
        this.averageRatingPatientToPractitioner = averageRatingPatientToPractitioner;
        this.totalPatientToPractitioner = totalPatientToPractitioner;
        this.averageRatingPractitionerToPatient = averageRatingPractitionerToPatient;
        this.totalPractitionerToPatient = totalPractitionerToPatient;
    }

    /**
     * Factory de conversión: convierte el resultado del dominio
     * ({@link FeedbackDashboardResult}) al wrapper expuesto al cliente.
     * Centraliza el armado del DTO para que los controladores no se llenen
     * de boilerplate.
     */
    public static FeedbackDashboardResponseDTO of(FeedbackDashboardResult result,
                                                  java.util.function.Function<site.utnpf.odontolink.domain.model.Feedback, FeedbackResponseDTO> mapper) {
        PageResponseDTO<FeedbackResponseDTO> page = PageResponseDTO.of(result.getPage(), mapper);
        FeedbackDirectionalAggregates aggregates = result.getAggregates();
        return new FeedbackDashboardResponseDTO(
                page,
                aggregates.getAverageRatingPatientToPractitioner(),
                aggregates.getTotalPatientToPractitioner(),
                aggregates.getAverageRatingPractitionerToPatient(),
                aggregates.getTotalPractitionerToPatient()
        );
    }

    public PageResponseDTO<FeedbackResponseDTO> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(PageResponseDTO<FeedbackResponseDTO> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public double getAverageRatingPatientToPractitioner() {
        return averageRatingPatientToPractitioner;
    }

    public void setAverageRatingPatientToPractitioner(double averageRatingPatientToPractitioner) {
        this.averageRatingPatientToPractitioner = averageRatingPatientToPractitioner;
    }

    public long getTotalPatientToPractitioner() {
        return totalPatientToPractitioner;
    }

    public void setTotalPatientToPractitioner(long totalPatientToPractitioner) {
        this.totalPatientToPractitioner = totalPatientToPractitioner;
    }

    public double getAverageRatingPractitionerToPatient() {
        return averageRatingPractitionerToPatient;
    }

    public void setAverageRatingPractitionerToPatient(double averageRatingPractitionerToPatient) {
        this.averageRatingPractitionerToPatient = averageRatingPractitionerToPatient;
    }

    public long getTotalPractitionerToPatient() {
        return totalPractitionerToPatient;
    }

    public void setTotalPractitionerToPatient(long totalPractitionerToPatient) {
        this.totalPractitionerToPatient = totalPractitionerToPatient;
    }
}
