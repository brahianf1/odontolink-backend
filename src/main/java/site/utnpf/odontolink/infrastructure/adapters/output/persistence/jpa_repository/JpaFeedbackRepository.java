package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para operaciones de base de datos de FeedbackEntity.
 * Extiende JpaRepository para obtener operaciones CRUD básicas.
 *
 * Spring Data JPA generará automáticamente la implementación en tiempo de ejecución.
 *
 * @author OdontoLink Team
 */
public interface JpaFeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    /**
     * Busca todos los feedbacks de una atención específica.
     *
     * @param attentionId ID de la atención
     * @return Lista de feedbacks de la atención
     */
    List<FeedbackEntity> findByAttention_Id(Long attentionId);

    /**
     * Verifica si existe un feedback de un usuario específico para una atención.
     * Implementa la validación de RF23: Evitar calificaciones duplicadas.
     *
     * @param attentionId ID de la atención
     * @param submittedById ID del usuario que envió el feedback
     * @return true si existe un feedback, false en caso contrario
     */
    boolean existsByAttention_IdAndSubmittedBy_Id(Long attentionId, Long submittedById);

    /**
     * Busca todos los feedbacks de las atenciones de un practicante específico.
     * Implementa RF25, RF40: Panel docente de supervisión de feedback.
     *
     * @param practitionerId ID del practicante
     * @return Lista de feedbacks de todas las atenciones del practicante
     */
    @Query("SELECT f FROM FeedbackEntity f " +
           "JOIN f.attention a " +
           "WHERE a.practitioner.id = :practitionerId " +
           "ORDER BY f.createdAt DESC")
    List<FeedbackEntity> findByPractitionerId(@Param("practitionerId") Long practitionerId);

    /**
     * Busca todos los feedbacks enviados por un usuario específico.
     *
     * @param submittedById ID del usuario
     * @return Lista de feedbacks enviados por el usuario
     */
    List<FeedbackEntity> findBySubmittedBy_Id(Long submittedById);

    /**
     * Busca el feedback específico de un usuario para una atención.
     *
     * @param attentionId ID de la atención
     * @param submittedById ID del usuario
     * @return Optional conteniendo el feedback si existe
     */
    Optional<FeedbackEntity> findByAttention_IdAndSubmittedBy_Id(Long attentionId, Long submittedById);
}
