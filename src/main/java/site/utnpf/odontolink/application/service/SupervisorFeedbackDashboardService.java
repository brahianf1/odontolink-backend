package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.ISupervisorFeedbackDashboardUseCase;
import site.utnpf.odontolink.application.port.in.dto.SupervisorFeedbackDashboardQuery;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.FeedbackDashboardResult;
import site.utnpf.odontolink.domain.model.FeedbackSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación EXCLUSIVO del Panel Docente de Supervisión de
 * Feedback (RF25). Implementa {@link ISupervisorFeedbackDashboardUseCase}.
 *
 * Decisión arquitectónica - SEPARACIÓN DE SOMBREROS DEL DOCENTE:
 *  - El sombrero CLÍNICO (micro-contexto) lo atiende {@code SupervisorAttentionService}
 *    + {@code FeedbackService} (RF39 / RF24).
 *  - El sombrero EVALUADOR DE DESEMPEÑO (macro-contexto, RF25) lo atiende
 *    este servicio. Las dos rutas comparten la base de datos pero NO
 *    comparten flujo, validaciones ni reglas de negocio. Mezclar ambos
 *    sombreros en un mismo servicio violaría Responsabilidad Única y, peor,
 *    contaminaría el flujo clínico con preocupaciones de paginación y
 *    agregación analítica.
 *
 * Cerco de seguridad (RF25/RF40 - CRÍTICO):
 *  - Antes de delegar al repositorio, el servicio resuelve el conjunto de
 *    practicantes vinculados al supervisor autenticado y lo INYECTA en los
 *    criterios. El cerco se aplica SIEMPRE, sea o no que el docente
 *    especifique un practitionerId en los filtros.
 *  - Si el docente sí especifica practitionerId, validamos previamente que
 *    pertenezca al conjunto vinculado: cualquier intento de acceder al
 *    feedback de un alumno ajeno se rechaza con 403, no se silencia con un
 *    resultado vacío (los silenciosos son los filtros opcionales, no las
 *    violaciones de scope).
 *
 * @Transactional(readOnly = true): operación analítica idempotente.
 */
@Transactional(readOnly = true)
public class SupervisorFeedbackDashboardService implements ISupervisorFeedbackDashboardUseCase {

    private final FeedbackRepository feedbackRepository;
    private final SupervisorRepository supervisorRepository;

    public SupervisorFeedbackDashboardService(FeedbackRepository feedbackRepository,
                                              SupervisorRepository supervisorRepository) {
        this.feedbackRepository = feedbackRepository;
        this.supervisorRepository = supervisorRepository;
    }

    @Override
    public FeedbackDashboardResult getDashboard(SupervisorFeedbackDashboardQuery query,
                                                PageQuery pageQuery,
                                                User supervisorUser) {
        // 1) Cerco de seguridad por rol. Defensa adicional al @PreAuthorize:
        //    si en un futuro alguien cambia la anotación del controlador, el
        //    dominio sigue rechazando a quien no sea supervisor.
        if (supervisorUser == null || supervisorUser.getRole() != Role.ROLE_SUPERVISOR) {
            throw new UnauthorizedOperationException(
                    "Solo los supervisores pueden acceder al Panel de Supervisión de Feedback."
            );
        }

        // 2) Resolución del perfil de supervisor + conjunto de practicantes vinculados.
        //    Reutilizamos el repositorio existente; el N-a-N ya está mapeado y
        //    fue probado en RF22/RF37/RF39.
        Supervisor supervisor = supervisorRepository.findByUserId(supervisorUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supervisor", "userId", supervisorUser.getId().toString()));

        Set<Long> allowedPractitionerIds = extractSupervisedPractitionerIds(supervisor);

        // 3) Si el docente intenta apuntar a un practicante fuera de su cerco,
        //    rechazamos explícitamente (no silenciamos). Esto evita que un
        //    docente "explore" practicantes ajenos probando IDs hasta encontrar
        //    uno con data, y deja en logs un 403 claro para auditoría.
        if (query.getPractitionerId() != null
                && !allowedPractitionerIds.contains(query.getPractitionerId())) {
            throw new UnauthorizedOperationException(
                    "No puede consultar feedback de un practicante que no supervisa."
            );
        }

        // 4) Atajo: docente sin practicantes a cargo => panel vacío sin tocar la
        //    base. No es un error de negocio: es un docente recién dado de alta.
        if (allowedPractitionerIds.isEmpty()) {
            PageResult<Feedback> emptyPage = new PageResult<>(
                    Collections.emptyList(),
                    pageQuery.getPage(),
                    pageQuery.getSize(),
                    0L,
                    0
            );
            return new FeedbackDashboardResult(emptyPage, 0.0, 0L);
        }

        // 5) Armamos los criterios de dominio inyectando el cerco como
        //    filtro silencioso. El controlador NUNCA aportó este campo;
        //    sólo el servicio lo conoce.
        FeedbackSearchCriteria criteria = new FeedbackSearchCriteria(
                query.getPractitionerId(),
                query.getPatientId(),
                query.getTreatmentId(),
                query.getStartDate(),
                query.getEndDate(),
                allowedPractitionerIds
        );

        // 6) Delegamos al repositorio: una llamada paginada para el contenido,
        //    una llamada agregada para el promedio. Ambas usan EXACTAMENTE las
        //    mismas Specifications, garantizando que el promedio refleja el
        //    universo total filtrado (no sólo la página actual).
        PageResult<Feedback> page = feedbackRepository.searchDashboard(criteria, pageQuery);
        double averageRating = feedbackRepository.averageRating(criteria);

        // 7) totalFeedbacksCount lo tomamos del propio PageResult: es el COUNT
        //    de la misma query, así evitamos una tercera consulta.
        return new FeedbackDashboardResult(page, averageRating, page.getTotalElements());
    }

    /**
     * Extrae los IDs de los practicantes que el supervisor tiene vinculados.
     *
     * Robustez ante datos parciales: si la colección viene null (supervisor
     * sin grabar la relación todavía) o algún practicante carece de ID,
     * devolvemos un conjunto vacío en lugar de un NPE: el caller traduce
     * eso en un panel vacío seguro.
     */
    private Set<Long> extractSupervisedPractitionerIds(Supervisor supervisor) {
        if (supervisor.getSupervisedPractitioners() == null) {
            return Collections.emptySet();
        }
        return supervisor.getSupervisedPractitioners().stream()
                .map(Practitioner::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }
}
