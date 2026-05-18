package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.ProviderGuardrail;
import site.utnpf.odontolink.domain.model.ProviderGuardrailType;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ProviderGuardrailEntity;

public final class ProviderGuardrailPersistenceMapper {

    private ProviderGuardrailPersistenceMapper() {
    }

    public static ProviderGuardrail toDomain(ProviderGuardrailEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProviderGuardrail(
                entity.getId(),
                entity.getProviderGuardrailUuid(),
                parseType(entity.getType()),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.isAttached(),
                entity.getPriority(),
                entity.getDefaultResponse(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ProviderGuardrailEntity toEntity(ProviderGuardrail domain) {
        if (domain == null) {
            return null;
        }
        ProviderGuardrailEntity entity = new ProviderGuardrailEntity();
        entity.setId(domain.getId());
        entity.setProviderGuardrailUuid(domain.getProviderGuardrailUuid());
        entity.setType(domain.getType() == null ? ProviderGuardrailType.OTHER.name() : domain.getType().name());
        entity.setDisplayName(domain.getDisplayName());
        entity.setDescription(domain.getDescription());
        entity.setAttached(domain.isAttached());
        entity.setPriority(domain.getPriority());
        entity.setDefaultResponse(domain.getDefaultResponse());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private static ProviderGuardrailType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProviderGuardrailType.OTHER;
        }
        try {
            return ProviderGuardrailType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return ProviderGuardrailType.OTHER;
        }
    }
}
