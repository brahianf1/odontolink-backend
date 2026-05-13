package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.application.port.in.dto.SupervisorFeedbackDashboardQuery;
import site.utnpf.odontolink.domain.model.FeedbackDashboardResult;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.User;

/**
 * Puerto de entrada para el Panel Docente de Supervisión de Feedback (RF25).
 *
 * Separado de {@link IFeedbackUseCase} y de {@link ISupervisorAttentionUseCase}
 * por Segregación de Interfaces (SOLID-I):
 *  - {@link IFeedbackUseCase} resuelve el contexto MICRO (calificar / consultar
 *    feedback de una atención puntual).
 *  - {@link ISupervisorAttentionUseCase} resuelve la auditoría clínica de un
 *    caso concreto (sombrero clínico del docente, RF39).
 *  - Este puerto resuelve el contexto MACRO: análisis agregado de la
 *    performance "soft skills" del alumnado (sombrero evaluador de
 *    desempeño, RF25). Comparte la base de datos pero NO comparte caso de
 *    uso ni reglas con los anteriores.
 *
 * Documentar la separación a nivel de puerto evita que un futuro mantenedor
 * la diluya agregando métodos analíticos al puerto clínico.
 */
public interface ISupervisorFeedbackDashboardUseCase {

    /**
     * Ejecuta una consulta paginada y agregada del Panel Docente de Feedback.
     *
     * Aplica el "cerco" silencioso: el resultado JAMÁS incluye feedback de
     * practicantes que el supervisor autenticado no tenga vinculados, sin
     * importar qué filtros mande el cliente.
     *
     * @param query Filtros opcionales del docente (practicante, paciente,
     *              tratamiento, ventana temporal).
     * @param pageQuery Paginación + ordenamiento solicitados.
     * @param supervisorUser Usuario autenticado (debe tener rol SUPERVISOR).
     * @return Contenedor con la página de feedbacks + promedio y total
     *         calculados sobre el universo exacto de los filtros aplicados.
     */
    FeedbackDashboardResult getDashboard(SupervisorFeedbackDashboardQuery query,
                                         PageQuery pageQuery,
                                         User supervisorUser);
}
