package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AiAgentConfigurationVersionEntity;

public final class AiAgentConfigurationVersionPersistenceMapper {

    private AiAgentConfigurationVersionPersistenceMapper() {
    }

    public static AiAgentConfigurationVersion toDomain(AiAgentConfigurationVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AiAgentConfigurationVersion(
                entity.getId(),
                entity.getVersionNumber(),
                entity.getDisplayName(),
                entity.getSystemPromptCore(),
                entity.getWelcomeMessage(),
                entity.getTemperature(),
                entity.getTopP(),
                entity.getMaxTokens(),
                entity.getK(),
                AiRetrievalMethod.valueOf(entity.getRetrievalMethod()),
                entity.getComposedInstruction(),
                entity.getGuardrailsLabelsSnapshot(),
                entity.getPublishedByUserId(),
                entity.isPublishedWithOverride(),
                entity.getMissingRequirementsAtPublish(),
                entity.getPublishedAt()
        );
    }

    public static AiAgentConfigurationVersionEntity toEntity(AiAgentConfigurationVersion domain) {
        if (domain == null) {
            return null;
        }
        AiAgentConfigurationVersionEntity entity = new AiAgentConfigurationVersionEntity();
        entity.setId(domain.getId());
        entity.setVersionNumber(domain.getVersionNumber());
        entity.setDisplayName(domain.getDisplayName());
        entity.setSystemPromptCore(domain.getSystemPromptCore());
        entity.setWelcomeMessage(domain.getWelcomeMessage());
        entity.setTemperature(domain.getTemperature());
        entity.setTopP(domain.getTopP());
        entity.setMaxTokens(domain.getMaxTokens());
        entity.setK(domain.getK());
        entity.setRetrievalMethod(domain.getRetrievalMethod().name());
        entity.setComposedInstruction(domain.getComposedInstruction());
        entity.setGuardrailsLabelsSnapshot(domain.getGuardrailsLabelsSnapshot());
        entity.setPublishedByUserId(domain.getPublishedByUserId());
        entity.setPublishedWithOverride(domain.isPublishedWithOverride());
        entity.setMissingRequirementsAtPublish(domain.getMissingRequirementsAtPublish());
        entity.setPublishedAt(domain.getPublishedAt());
        return entity;
    }
}
