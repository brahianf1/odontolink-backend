package site.utnpf.odontolink.domain.model;

/**
 * Value Object inmutable que encapsula el resultado del Panel Docente de
 * Feedback (RF25).
 *
 * <p>Decisión analítica: un panel de supervisión NO es un array de filas; es
 * un objeto contenedor que entrega la slice paginada de feedbacks junto
 * con los agregados (promedio + total) calculados sobre el MISMO universo
 * de filtros aplicados. De esta forma, mientras el docente recorre páginas,
 * los indicadores macro siguen siendo coherentes con su búsqueda.
 *
 * <p>Migración v2 (feedback bidireccional discriminado): los agregados ya
 * no son una única media global — se reportan separados por dirección
 * ({@link FeedbackDirection}) dentro de {@link FeedbackDirectionalAggregates}.
 * El "promedio global" que mezclaba paciente→practicante con practicante→
 * paciente era una métrica contaminada y se eliminó deliberadamente para
 * que el panel docente refleje el desempeño real del estudiante (sólo el
 * P→Pr). Si en el futuro se necesita una combinación, se calcula a partir
 * de los counts expuestos en {@link FeedbackDirectionalAggregates}.
 */
public final class FeedbackDashboardResult {

    private final PageResult<Feedback> page;
    private final FeedbackDirectionalAggregates aggregates;

    public FeedbackDashboardResult(PageResult<Feedback> page,
                                   FeedbackDirectionalAggregates aggregates) {
        this.page = page;
        this.aggregates = aggregates;
    }

    public PageResult<Feedback> getPage() {
        return page;
    }

    public FeedbackDirectionalAggregates getAggregates() {
        return aggregates;
    }
}
