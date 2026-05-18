package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.EmergencyKeyword;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.EmergencyKeywordEntity;

public final class EmergencyKeywordPersistenceMapper {

    private EmergencyKeywordPersistenceMapper() {
    }

    public static EmergencyKeyword toDomain(EmergencyKeywordEntity entity) {
        if (entity == null) {
            return null;
        }
        return new EmergencyKeyword(
                entity.getId(),
                entity.getTerm(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static EmergencyKeywordEntity toEntity(EmergencyKeyword domain) {
        if (domain == null) {
            return null;
        }
        EmergencyKeywordEntity entity = new EmergencyKeywordEntity();
        entity.setId(domain.getId());
        entity.setTerm(domain.getTerm());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
