package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.Feedback;
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
     * Obtiene todos los feedbacks de las atenciones de un practicante específico.
     * Implementa RF25, RF40: Panel docente de supervisión de feedback.
     *
     * @param practitionerId El ID del practicante
     * @return Lista de feedbacks de todas las atenciones del practicante
     */
    List<Feedback> findByPractitionerId(Long practitionerId);

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
