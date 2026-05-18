package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.ChatbotSession;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatbotSessionEntity;

public final class ChatbotSessionPersistenceMapper {

    private ChatbotSessionPersistenceMapper() {
    }

    public static ChatbotSession toDomain(ChatbotSessionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ChatbotSession(
                entity.getId(),
                entity.getOwnerUserId(),
                entity.getAnonymousToken(),
                entity.getStartedAt(),
                entity.getLastInteractionAt(),
                entity.getMessageCount()
        );
    }

    public static ChatbotSessionEntity toEntity(ChatbotSession domain) {
        if (domain == null) {
            return null;
        }
        ChatbotSessionEntity entity = new ChatbotSessionEntity();
        entity.setId(domain.getId());
        entity.setOwnerUserId(domain.getOwnerUserId());
        entity.setAnonymousToken(domain.getAnonymousToken());
        entity.setStartedAt(domain.getStartedAt());
        entity.setLastInteractionAt(domain.getLastInteractionAt());
        entity.setMessageCount(domain.getMessageCount());
        return entity;
    }
}
