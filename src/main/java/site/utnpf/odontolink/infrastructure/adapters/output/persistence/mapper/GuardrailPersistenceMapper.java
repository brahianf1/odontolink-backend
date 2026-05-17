package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.Guardrail;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.GuardrailEntity;

public final class GuardrailPersistenceMapper {

    private GuardrailPersistenceMapper() {
    }

    public static Guardrail toDomain(GuardrailEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Guardrail(
                entity.getId(),
                entity.getLabel(),
                entity.getText(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static GuardrailEntity toEntity(Guardrail domain) {
        if (domain == null) {
            return null;
        }
        GuardrailEntity entity = new GuardrailEntity();
        entity.setId(domain.getId());
        entity.setLabel(domain.getLabel());
        entity.setText(domain.getText());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
