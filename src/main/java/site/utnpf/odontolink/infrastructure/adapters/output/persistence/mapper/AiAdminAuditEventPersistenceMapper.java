package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAdminAuditEventEntity;

public final class AiAdminAuditEventPersistenceMapper {

    private AiAdminAuditEventPersistenceMapper() {
    }

    public static AiAdminAuditEvent toDomain(AiAdminAuditEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AiAdminAuditEvent(
                entity.getId(),
                entity.getType(),
                entity.getActorUserId(),
                entity.getRelatedVersionNumber(),
                entity.isWithOverride(),
                entity.getDetails(),
                entity.getOccurredAt()
        );
    }

    public static AiAdminAuditEventEntity toEntity(AiAdminAuditEvent domain) {
        if (domain == null) {
            return null;
        }
        AiAdminAuditEventEntity entity = new AiAdminAuditEventEntity();
        entity.setId(domain.getId());
        entity.setType(domain.getType());
        entity.setActorUserId(domain.getActorUserId());
        entity.setRelatedVersionNumber(domain.getRelatedVersionNumber());
        entity.setWithOverride(domain.isWithOverride());
        entity.setDetails(domain.getDetails());
        entity.setOccurredAt(domain.getOccurredAt());
        return entity;
    }
}
