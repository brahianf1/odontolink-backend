package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackEntity;

/**
 * Mapper para convertir entre Feedback (dominio) y FeedbackEntity (persistencia).
 *
 * Este mapper sigue el patrón de evitar ciclos infinitos al mapear relaciones bidireccionales.
 * El Feedback contiene una referencia a Attention, por lo que se mapea con versión "shallow"
 * para evitar recursión infinita.
 *
 * @author OdontoLink Team
 */
public class FeedbackPersistenceMapper {

    private FeedbackPersistenceMapper() {
        // Utility class
    }

    /**
     * Convierte de entidad JPA a modelo de dominio.
     *
     * @param entity Entidad JPA FeedbackEntity
     * @return Modelo de dominio Feedback
     */
    public static Feedback toDomain(FeedbackEntity entity) {
        if (entity == null) {
            return null;
        }

        Feedback feedback = new Feedback();
        feedback.setId(entity.getId());
        feedback.setRating(entity.getRating());
        feedback.setComment(entity.getComment());
        feedback.setCreatedAt(entity.getCreatedAt());

        // Mapear la Attention con versión "shallow" para evitar recursión infinita
        if (entity.getAttention() != null) {
            feedback.setAttention(AttentionPersistenceMapper.toDomainShallow(entity.getAttention()));
        }

        // Mapear el User que envió el feedback
        if (entity.getSubmittedBy() != null) {
            feedback.setSubmittedBy(UserPersistenceMapper.toDomain(entity.getSubmittedBy()));
        }

        return feedback;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     *
     * @param feedback Modelo de dominio Feedback
     * @return Entidad JPA FeedbackEntity
     */
    public static FeedbackEntity toEntity(Feedback feedback) {
        if (feedback == null) {
            return null;
        }

        FeedbackEntity entity = new FeedbackEntity();
        entity.setId(feedback.getId());
        entity.setRating(feedback.getRating());
        entity.setComment(feedback.getComment());
        entity.setCreatedAt(feedback.getCreatedAt());

        // Mapear la Attention
        if (feedback.getAttention() != null) {
            entity.setAttention(AttentionPersistenceMapper.toEntityShallow(feedback.getAttention()));
        }

        // Mapear el User
        if (feedback.getSubmittedBy() != null) {
            entity.setSubmittedBy(UserPersistenceMapper.toEntity(feedback.getSubmittedBy()));
        }

        return entity;
    }
}
