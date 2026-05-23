package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Feedback;
import site.utnpf.odontolink.domain.model.FeedbackCriterionScore;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackCriterionScoreEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeo bidireccional entre {@link Feedback} (dominio) y {@link FeedbackEntity}.
 *
 * <p>Convierte también la subcolección de {@link FeedbackCriterionScore}.
 * Para evitar recursión infinita sobre {@code Attention} se usa la versión
 * shallow del mapper de atenciones — mismo patrón que el resto del módulo.
 */
public final class FeedbackPersistenceMapper {

    private FeedbackPersistenceMapper() {
    }

    public static Feedback toDomain(FeedbackEntity entity) {
        if (entity == null) {
            return null;
        }
        Feedback feedback = new Feedback();
        feedback.setId(entity.getId());
        feedback.setComment(entity.getComment());
        feedback.setCreatedAt(entity.getCreatedAt());

        if (entity.getAttention() != null) {
            feedback.setAttention(AttentionPersistenceMapper.toDomainShallow(entity.getAttention()));
        }
        if (entity.getSubmittedBy() != null) {
            feedback.setSubmittedBy(UserPersistenceMapper.toDomain(entity.getSubmittedBy()));
        }

        if (entity.getScores() != null && !entity.getScores().isEmpty()) {
            List<FeedbackCriterionScore> domainScores = entity.getScores().stream()
                    .map(FeedbackPersistenceMapper::toDomainScore)
                    .collect(Collectors.toList());
            feedback.replaceScores(domainScores);
        }
        return feedback;
    }

    public static FeedbackEntity toEntity(Feedback feedback) {
        if (feedback == null) {
            return null;
        }
        FeedbackEntity entity = new FeedbackEntity();
        entity.setId(feedback.getId());
        entity.setComment(feedback.getComment());
        entity.setCreatedAt(feedback.getCreatedAt());

        if (feedback.getAttention() != null) {
            entity.setAttention(AttentionPersistenceMapper.toEntityShallow(feedback.getAttention()));
        }
        if (feedback.getSubmittedBy() != null) {
            entity.setSubmittedBy(UserPersistenceMapper.toEntity(feedback.getSubmittedBy()));
        }

        List<FeedbackCriterionScoreEntity> scoreEntities = new ArrayList<>();
        for (FeedbackCriterionScore s : feedback.getScores()) {
            FeedbackCriterionScoreEntity se = new FeedbackCriterionScoreEntity();
            se.setId(s.getId());
            se.setScore(s.getScore());
            se.setCriterion(FeedbackCriterionPersistenceMapper.toEntity(s.getCriterion()));
            se.setFeedback(entity);
            scoreEntities.add(se);
        }
        entity.setScores(scoreEntities);
        return entity;
    }

    private static FeedbackCriterionScore toDomainScore(FeedbackCriterionScoreEntity scoreEntity) {
        FeedbackCriterionScore score = new FeedbackCriterionScore();
        score.setId(scoreEntity.getId());
        score.setScore(scoreEntity.getScore());
        score.setCriterion(FeedbackCriterionPersistenceMapper.toDomain(scoreEntity.getCriterion()));
        return score;
    }
}
