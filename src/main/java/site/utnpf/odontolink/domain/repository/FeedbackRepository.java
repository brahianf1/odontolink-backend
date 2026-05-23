package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.FeedbackDirectionalAggregates;
import site.utnpf.odontolink.domain.model.FeedbackSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.model.PractitionerCriterionPerformance;
import site.utnpf.odontolink.domain.model.PractitionerRankingEntry;
import site.utnpf.odontolink.domain.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * Calcula los agregados (promedio + total) del Panel Docente
     * discriminados por dirección del feedback bidireccional.
     *
     * <p>Reemplaza al obsoleto {@code averageRating(criteria)} que mezclaba
     * paciente→practicante con practicante→paciente y, por tanto, distorsionaba
     * la métrica de desempeño del estudiante. La separación se ejecuta dentro
     * de una sola consulta SQL agregada (con CASE WHEN o GROUP BY) para no
     * pagar dos round-trips por dirección.
     *
     * <p>El campo {@code direction} dentro de {@code criteria} NO se aplica
     * acá: este método siempre devuelve ambos sentidos para que el cliente
     * pueda mostrar los dos indicadores. El {@code direction} sólo afecta a
     * {@link #searchDashboard} (filtra la slice paginada).
     *
     * <p>Si el universo filtrado no contiene feedbacks de una dirección,
     * su promedio y count se reportan en 0.
     *
     * @param criteria Filtros del docente + cerco de practicantes permitidos
     * @return Agregados directionales (nunca null)
     */
    FeedbackDirectionalAggregates aggregateByDirection(FeedbackSearchCriteria criteria);

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

    /**
     * Top-N practicantes con mejor promedio sobre un criterio puntual,
     * dentro del cerco {@code allowedPractitionerIds} y opcionales filtros
     * de fecha y tratamiento.
     *
     * <p>Sólo considera practicantes con al menos {@code minFeedbackCount}
     * scores sobre ese criterio. Si el cerco viene vacío se devuelve lista
     * vacía sin tocar BD (corresponde al adaptador).
     *
     * @param criterionCode          code del {@link site.utnpf.odontolink.domain.model.FeedbackCriterion}
     * @param allowedPractitionerIds cerco silencioso supervisor→practicantes
     * @param startDate              filtro {@code createdAt >= startDate} (nullable)
     * @param endDate                filtro {@code createdAt <= endDate}   (nullable)
     * @param treatmentId            filtro por treatment (nullable)
     * @param minFeedbackCount       mínimo de scores para aparecer
     * @param topN                   tamaño máximo del ranking
     * @return entradas ordenadas DESC por averageScore con rankPosition 1-based
     */
    List<PractitionerCriterionPerformance> topPractitionersByCriterion(
            String criterionCode,
            Set<Long> allowedPractitionerIds,
            LocalDate startDate,
            LocalDate endDate,
            Long treatmentId,
            int minFeedbackCount,
            int topN);

    /**
     * Ranking de practicantes según el "promedio de promedios" sobre los
     * criterios con {@code includeInRanking=true} aplicables a la dirección
     * paciente→practicante.
     *
     * <p>Para cada practicante: para cada criterio computa AVG(score); luego
     * promedia esos AVGs (cada criterio pesa igual sin importar el sample
     * size por criterio). Sólo aparecen practicantes con al menos
     * {@code minFeedbackCount} feedbacks distintos contribuyendo.
     *
     * @param allowedPractitionerIds cerco silencioso
     * @param startDate              nullable
     * @param endDate                nullable
     * @param treatmentId            nullable
     * @param minFeedbackCount       mínimo de feedbacks distintos
     * @param topN                   tamaño máximo del ranking
     * @return entradas ordenadas DESC por combinedAverage con rankPosition 1-based
     */
    List<PractitionerRankingEntry> practitionerOverallRanking(
            Set<Long> allowedPractitionerIds,
            LocalDate startDate,
            LocalDate endDate,
            Long treatmentId,
            int minFeedbackCount,
            int topN);
}
