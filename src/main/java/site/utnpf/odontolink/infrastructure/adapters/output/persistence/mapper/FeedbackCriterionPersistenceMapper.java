package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.FeedbackCriterion;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.FeedbackCriterionEntity;

/**
 * Mapeo bidireccional entre {@link FeedbackCriterion} (dominio) y
 * {@link FeedbackCriterionEntity} (persistencia). Master data simple: sin
 * recursividad ni relaciones bidireccionales que romper.
 */
public final class FeedbackCriterionPersistenceMapper {

    private FeedbackCriterionPersistenceMapper() {
    }

    public static FeedbackCriterion toDomain(FeedbackCriterionEntity entity) {
        if (entity == null) {
            return null;
        }
        FeedbackCriterion domain = new FeedbackCriterion(
                entity.getCode(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getApplicableDirection(),
                entity.isIncludeInRanking(),
                entity.getDisplayOrder(),
                entity.isActive()
        );
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setDeactivatedAt(entity.getDeactivatedAt());
        return domain;
    }

    public static FeedbackCriterionEntity toEntity(FeedbackCriterion domain) {
        if (domain == null) {
            return null;
        }
        FeedbackCriterionEntity entity = new FeedbackCriterionEntity();
        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setDisplayName(domain.getDisplayName());
        entity.setDescription(domain.getDescription());
        entity.setApplicableDirection(domain.getApplicableDirection());
        entity.setIncludeInRanking(domain.isIncludeInRanking());
        entity.setDisplayOrder(domain.getDisplayOrder());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDeactivatedAt(domain.getDeactivatedAt());
        return entity;
    }
}
