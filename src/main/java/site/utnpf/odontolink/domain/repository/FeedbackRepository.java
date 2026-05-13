package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.FeedbackSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Output Port) para operaciones de persistencia de Feedback.
 * Sigue los principios de Arquitectura Hexagonal (Ports and Adapters).
 *
 * Este repositorio proporciona métodos para:
 * - Crear y guardar feedback
 * - Consultar feedback por atención
 * - Validar existencia de feedback (para prevenir duplicados - RF23)
 * - Consultar feedback por practicante (para supervisores - RF25, RF40)
 *
 * @author OdontoLink Team
 */
public interface FeedbackRepository {

    /**
     * Guarda un nuevo feedback o actualiza uno existente.
     *
     * @param feedback El feedback a guardar
     * @return El feedback guardado con su ID asignado
     */
    Feedback save(Feedback feedback);

    /**
     * Busca un feedback por su ID.
     *
     * @param id El ID del feedback
     * @return Optional conteniendo el feedback si existe
     */
    Optional<Feedback> findById(Long id);

    /**
     * Obtiene todos los feedbacks asociados a una atención específica.
     * Implementa RF24: Visualización segmentada por rol.
     *
     * @param attention La atención cuyo feedback se quiere consultar
     * @return Lista de feedbacks de la atención (vacía si no hay ninguno)
     */
    List<Feedback> findByAttention(Attention attention);

    /**
     * Obtiene todos los feedbacks asociados a una atención por su ID.
     *
     * @param attentionId El ID de la atención
     * @return Lista de feedbacks de la atención
     */
    List<Feedback> findByAttentionId(Long attentionId);

    /**
     * Verifica si ya existe un feedback de un usuario específico para una atención.
     * Implementa RF23: Evitar calificaciones duplicadas.
     *
     * @param attention La atención a verificar
     * @param submittedBy El usuario que envió el feedback
     * @return true si existe un feedback, false en caso contrario
     */
    boolean existsByAttentionAndSubmittedBy(Attention attention, User submittedBy);

    /**
     * Búsqueda paginada del Panel Docente de Supervisión de Feedback (RF25).
     *
     * Aplica los criterios opcionales en AND lógico junto con el cerco
     * obligatorio {@code practitioner.id IN allowedPractitionerIds} para
     * impedir que un supervisor vea feedback de alumnos que no le están
     * vinculados. La traducción a SQL se realiza vía JPA Specifications en
     * el adaptador.
     *
     * Si {@code allowedPractitionerIds} viene vacío, el resultado debe ser
     * una página vacía: representa al supervisor recién creado sin
     * practicantes a cargo.
     *
     * @param criteria Filtros del docente + cerco de practicantes permitidos
     * @param pageQuery Página y ordenamiento solicitados
     * @return Página de feedbacks (puede ser vacía, nunca null)
     */
    PageResult<Feedback> searchDashboard(FeedbackSearchCriteria criteria, PageQuery pageQuery);

    /**
     * Calcula el promedio de la calificación (rating) sobre EXACTAMENTE el
     * mismo universo de filtros usado por {@link #searchDashboard}.
     *
     * Decisión: la agregación se ejecuta en el motor de base de datos, no
     * en memoria, para que el panel funcione bien aunque el supervisor
     * tenga miles de feedbacks. Por la misma razón se devuelve un
     * {@code double} sin requerir cargar las filas paginadas.
     *
     * Si el universo está vacío, se devuelve {@code 0.0} (no se lanza
     * excepción): el frontend interpreta este valor junto con
     * {@code totalFeedbacksCount} para decidir si mostrar el indicador.
     *
     * @param criteria Filtros del docente + cerco de practicantes permitidos
     * @return Promedio de rating sobre el universo filtrado, o 0.0 si está vacío
     */
    double averageRating(FeedbackSearchCriteria criteria);

    /**
     * Obtiene los feedbacks enviados por un usuario específico.
     *
     * @param userId El ID del usuario
     * @return Lista de feedbacks enviados por el usuario
     */
    List<Feedback> findBySubmittedById(Long userId);

    /**
     * Obtiene el feedback específico de un usuario para una atención.
     *
     * @param attentionId El ID de la atención
     * @param userId El ID del usuario
     * @return Optional conteniendo el feedback si existe
     */
    Optional<Feedback> findByAttentionIdAndSubmittedById(Long attentionId, Long userId);
}
