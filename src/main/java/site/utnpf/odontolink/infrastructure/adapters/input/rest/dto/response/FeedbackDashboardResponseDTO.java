package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import site.utnpf.odontolink.domain.model.FeedbackDashboardResult;

/**
 * "DashboardWrapper" del Panel Docente de Supervisión de Feedback (RF25).
 *
 * Un panel analítico no es un array de filas: es un objeto contenedor que
 * agrupa el listado paginado y los indicadores macro calculados sobre el
 * mismo universo de filtros. Este DTO formaliza ese contrato hacia el
 * frontend para que la lectura sea autoexplicativa y los indicadores
 * sigan coherentes mientras el docente navega entre páginas.
 *
 * Campos:
 *  - {@code feedbacks}: slice paginada (contenido + metadata de paginación).
 *  - {@code averageRating}: promedio de calificación sobre el universo
 *    filtrado completo (no sólo la página actual). Coincide con {@code
 *    SUM(rating)/COUNT(*)} en BD.
 *  - {@code totalFeedbacksCount}: cantidad total de feedbacks en el
 *    universo filtrado. Es el mismo valor que {@code feedbacks.totalElements},
 *    expuesto como campo de primer nivel para que el frontend no tenga que
 *    razonar "el total real está dentro de la página".
 */
@Schema(description = "Contenedor del Panel Docente de Supervisión de Feedback (RF25). Incluye la lista paginada de feedbacks y los indicadores agregados sobre el mismo universo de filtros.")
public class FeedbackDashboardResponseDTO {

    @Schema(description = "Página de feedbacks (contenido + metadata de paginación)")
    private PageResponseDTO<FeedbackResponseDTO> feedbacks;

    @Schema(description = "Promedio de la calificación sobre el universo filtrado completo", example = "4.27")
    private double averageRating;

    @Schema(description = "Cantidad total de feedbacks en el universo filtrado completo", example = "137")
    private long totalFeedbacksCount;

    public FeedbackDashboardResponseDTO() {
    }

    public FeedbackDashboardResponseDTO(PageResponseDTO<FeedbackResponseDTO> feedbacks,
                                        double averageRating,
                                        long totalFeedbacksCount) {
        this.feedbacks = feedbacks;
        this.averageRating = averageRating;
        this.totalFeedbacksCount = totalFeedbacksCount;
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
        return new FeedbackDashboardResponseDTO(
                page,
                result.getAverageRating(),
                result.getTotalFeedbacksCount()
        );
    }

    public PageResponseDTO<FeedbackResponseDTO> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(PageResponseDTO<FeedbackResponseDTO> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public long getTotalFeedbacksCount() {
        return totalFeedbacksCount;
    }

    public void setTotalFeedbacksCount(long totalFeedbacksCount) {
        this.totalFeedbacksCount = totalFeedbacksCount;
    }
}
