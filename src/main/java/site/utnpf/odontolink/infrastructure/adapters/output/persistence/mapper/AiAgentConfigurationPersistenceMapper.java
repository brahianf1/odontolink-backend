package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.AiAgentConfiguration;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAgentConfigurationEntity;

/**
 * Mapper estatico entre {@link AiAgentConfiguration} (dominio) y
 * {@link AiAgentConfigurationEntity} (persistencia).
 */
public final class AiAgentConfigurationPersistenceMapper {

    private AiAgentConfigurationPersistenceMapper() {
    }

    public static AiAgentConfiguration toDomain(AiAgentConfigurationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AiAgentConfiguration(
                entity.getId(),
                entity.getDisplayName(),
                entity.getSystemPromptCore(),
                entity.getWelcomeMessage(),
                entity.getTemperature(),
                entity.getTopP(),
                entity.getMaxTokens(),
                entity.getK(),
                AiRetrievalMethod.valueOf(entity.getRetrievalMethod()),
                AiAgentLifecycle.valueOf(entity.getLifecycle()),
                entity.getProviderAgentId(),
                entity.getProviderSyncedAt(),
                entity.getLastSyncError(),
                entity.getUpdatedAt()
        );
    }

    public static AiAgentConfigurationEntity toEntity(AiAgentConfiguration domain) {
        if (domain == null) {
            return null;
        }
        AiAgentConfigurationEntity entity = new AiAgentConfigurationEntity();
        entity.setId(domain.getId());
        entity.setDisplayName(domain.getDisplayName());
        entity.setSystemPromptCore(domain.getSystemPromptCore());
        entity.setWelcomeMessage(domain.getWelcomeMessage());
        entity.setTemperature(domain.getTemperature());
        entity.setTopP(domain.getTopP());
        entity.setMaxTokens(domain.getMaxTokens());
        entity.setK(domain.getK());
        entity.setRetrievalMethod(domain.getRetrievalMethod().name());
        entity.setLifecycle(domain.getLifecycle().name());
        entity.setProviderAgentId(domain.getProviderAgentId());
        entity.setProviderSyncedAt(domain.getProviderSyncedAt());
        entity.setLastSyncError(domain.getLastSyncError());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
