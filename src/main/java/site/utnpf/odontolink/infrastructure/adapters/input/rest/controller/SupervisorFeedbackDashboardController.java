package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.utnpf.odontolink.application.port.in.ISupervisorFeedbackDashboardUseCase;
import site.utnpf.odontolink.application.port.in.dto.SupervisorFeedbackDashboardQuery;
import site.utnpf.odontolink.domain.model.FeedbackDashboardResult;
import site.utnpf.odontolink.domain.model.FeedbackDirection;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.FeedbackDashboardResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.FeedbackRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.time.LocalDate;

/**
 * Adaptador de entrada REST para el Panel Docente de Supervisión de
 * Feedback (RF25).
 *
 * Rutas bajo {@code /api/supervisors/feedbacks/...} para mantener la
 * jerarquía semántica iniciada por {@code SupervisorController} y
 * {@code SupervisorAttentionController}: el recurso raíz del supervisor
 * es "lo suyo" (sus practicantes, sus atenciones, su feedback agregado).
 *
 * Reemplaza al endpoint legacy
 * {@code GET /api/supervisor/feedback/practitioner/{practitionerId}}, que
 * fue auditado y eliminado: aquel devolvía una lista plana sin filtros ni
 * agregados, incompatible con los requerimientos analíticos de RF25.
 *
 * Capas de defensa:
 *  - Filtros de seguridad y JWT a cargo de la cadena de Spring Security.
 *  - {@code @PreAuthorize("hasRole('SUPERVISOR')")} en el endpoint.
 *  - El servicio de aplicación aplica el cerco docente-alumno: aunque la
 *    anotación @PreAuthorize fallara por configuración, el dominio sigue
 *    rechazando.
 */
@RestController
@RequestMapping("/api/supervisors/feedbacks")
@Tag(
        name = "Panel Docente de Feedback",
        description = "RF25 - Análisis agregado del feedback recibido por los practicantes a cargo (Soft Skills)"
)
public class SupervisorFeedbackDashboardController {

    private final ISupervisorFeedbackDashboardUseCase dashboardUseCase;
    private final AuthenticationFacade authenticationFacade;

    public SupervisorFeedbackDashboardController(ISupervisorFeedbackDashboardUseCase dashboardUseCase,
                                                 AuthenticationFacade authenticationFacade) {
        this.dashboardUseCase = dashboardUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Búsqueda dinámica + paginación + agregados del Panel Docente (RF25).
     *
     * Filtros opcionales y combinables (AND lógico):
     *  - practitionerId: limita el universo a un practicante puntual.
     *  - patientId: limita el universo a un paciente puntual.
     *  - treatmentId: limita el universo a un tipo de tratamiento.
     *  - startDate / endDate: ventana inclusiva sobre la fecha de creación
     *    del feedback (ISO {@code YYYY-MM-DD}).
     *
     * Paginación:
     *  - page: 0-based, default 0.
     *  - size: default {@link PageQuery#DEFAULT_PAGE_SIZE}, máx {@link PageQuery#MAX_PAGE_SIZE}.
     *  - sortBy: alias permitido (createdAt | rating | practitionerId | patientId | treatmentId | id).
     *  - sortDirection: ASC | DESC (default DESC sobre createdAt).
     *
     * Seguridad (RF25/RF40 - CRÍTICA):
     *  - El servicio fuerza siempre un IN sobre los practicantes vinculados.
     *  - Si el cliente especifica practitionerId fuera del cerco, responde 403.
     *  - Si el supervisor no tiene practicantes a cargo, responde una página
     *    vacía con agregados en 0 (no es un error de negocio).
     *
     * GET /api/supervisors/feedbacks/dashboard
     */
    @Operation(
            summary = "Panel Docente de Supervisión de Feedback (RF25)",
            description = "Devuelve la lista paginada de feedbacks de los practicantes a cargo " +
                    "junto con el promedio de calificación y el total — todos calculados sobre el " +
                    "MISMO universo de filtros aplicados. Todos los filtros son opcionales y se " +
                    "combinan con AND lógico. El cerco docente-alumno se aplica de forma silenciosa."
    )
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<FeedbackDashboardResponseDTO> getDashboard(
            @Parameter(description = "ID del practicante a evaluar (debe pertenecer al cerco de supervisión)")
            @RequestParam(required = false) Long practitionerId,
            @Parameter(description = "ID del paciente para cruzar feedback por experiencia del paciente")
            @RequestParam(required = false) Long patientId,
            @Parameter(description = "ID del tratamiento para comparar performance en un procedimiento puntual")
            @RequestParam(required = false) Long treatmentId,
            @Parameter(description = "Fecha de inicio (inclusiva) sobre la creación del feedback (ISO YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha de fin (inclusiva) sobre la creación del feedback (ISO YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Dirección del feedback bidireccional. Si se omite, la lista paginada incluye " +
                    "ambos sentidos. Los agregados se reportan SIEMPRE discriminados.")
            @RequestParam(required = false) FeedbackDirection direction,
            @Parameter(description = "Número de página (0-based)")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página (máx 100)")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "Campo de ordenamiento (createdAt | rating | practitionerId | patientId | treatmentId | id)")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Dirección de ordenamiento (ASC | DESC). Default DESC sobre createdAt si no se envía sortBy.")
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {

        User supervisorUser = authenticationFacade.getAuthenticatedUser();

        SupervisorFeedbackDashboardQuery query = new SupervisorFeedbackDashboardQuery(
                practitionerId, patientId, treatmentId, startDate, endDate, direction
        );
        PageQuery pageQuery = PageQuery.of(page, size, sortBy, sortDirection);

        FeedbackDashboardResult result = dashboardUseCase.getDashboard(query, pageQuery, supervisorUser);

        FeedbackDashboardResponseDTO response =
                FeedbackDashboardResponseDTO.of(result, FeedbackRestMapper::toResponse);

        return ResponseEntity.ok(response);
    }
}
