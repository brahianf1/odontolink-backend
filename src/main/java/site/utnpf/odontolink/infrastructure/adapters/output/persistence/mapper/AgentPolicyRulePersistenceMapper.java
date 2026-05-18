package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.AgentPolicyRule;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AgentPolicyRuleEntity;

public final class AgentPolicyRulePersistenceMapper {

    private AgentPolicyRulePersistenceMapper() {
    }

    public static AgentPolicyRule toDomain(AgentPolicyRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AgentPolicyRule(
                entity.getId(),
                entity.getLabel(),
                entity.getText(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static AgentPolicyRuleEntity toEntity(AgentPolicyRule domain) {
        if (domain == null) {
            return null;
        }
        AgentPolicyRuleEntity entity = new AgentPolicyRuleEntity();
        entity.setId(domain.getId());
        entity.setLabel(domain.getLabel());
        entity.setText(domain.getText());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
