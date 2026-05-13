package site.utnpf.odontolink.domain.model;

/**
 * Value Object inmutable que encapsula el resultado del Panel Docente de
 * Feedback (RF25).
 *
 * Decisión analítica: un panel de supervisión NO es un array de filas; es
 * un objeto contenedor que entrega la slice paginada de feedbacks junto
 * con los agregados (promedio + total) calculados sobre el MISMO universo
 * de filtros aplicados. De esta forma, mientras el docente recorre páginas,
 * los indicadores macro siguen siendo coherentes con su búsqueda.
 *
 * El total de feedbacks se expone como campo explícito (además de existir
 * dentro de {@link PageResult#getTotalElements()}) para que el contrato
 * con el frontend sea autoexplicativo y no haya que documentar la regla
 * "totalFeedbacksCount = page.totalElements".
 */
public final class FeedbackDashboardResult {

    private final PageResult<Feedback> page;
    private final double averageRating;
    private final long totalFeedbacksCount;

    public FeedbackDashboardResult(PageResult<Feedback> page,
                                   double averageRating,
                                   long totalFeedbacksCount) {
        this.page = page;
        this.averageRating = averageRating;
        this.totalFeedbacksCount = totalFeedbacksCount;
    }

    public PageResult<Feedback> getPage() {
        return page;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public long getTotalFeedbacksCount() {
        return totalFeedbacksCount;
    }
}
