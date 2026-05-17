package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.AiGovernancePolicy;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiGovernancePolicyEntity;

public final class AiGovernancePolicyPersistenceMapper {

    private AiGovernancePolicyPersistenceMapper() {
    }

    public static AiGovernancePolicy toDomain(AiGovernancePolicyEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AiGovernancePolicy(
                entity.getId(),
                entity.isRequireGuardrails(),
                entity.getMinActiveGuardrails(),
                entity.isRequireSystemPrompt(),
                entity.isRequireWelcomeMessage(),
                entity.isRequireIndexedDocuments(),
                entity.isAllowOverride(),
                entity.getUpdatedAt()
        );
    }

    public static AiGovernancePolicyEntity toEntity(AiGovernancePolicy domain) {
        if (domain == null) {
            return null;
        }
        AiGovernancePolicyEntity entity = new AiGovernancePolicyEntity();
        entity.setId(domain.getId());
        entity.setRequireGuardrails(domain.isRequireGuardrails());
        entity.setMinActiveGuardrails(domain.getMinActiveGuardrails());
        entity.setRequireSystemPrompt(domain.isRequireSystemPrompt());
        entity.setRequireWelcomeMessage(domain.isRequireWelcomeMessage());
        entity.setRequireIndexedDocuments(domain.isRequireIndexedDocuments());
        entity.setAllowOverride(domain.isAllowOverride());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
