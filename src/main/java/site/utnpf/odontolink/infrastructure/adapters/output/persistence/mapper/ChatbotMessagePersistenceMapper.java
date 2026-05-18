package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.ChatbotMessage;
import site.utnpf.odontolink.domain.model.ChatbotMessageRole;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatbotMessageEntity;

public final class ChatbotMessagePersistenceMapper {

    private ChatbotMessagePersistenceMapper() {
    }

    public static ChatbotMessage toDomain(ChatbotMessageEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ChatbotMessage(
                entity.getId(),
                entity.getSessionId(),
                ChatbotMessageRole.valueOf(entity.getRole()),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    public static ChatbotMessageEntity toEntity(ChatbotMessage domain) {
        if (domain == null) {
            return null;
        }
        ChatbotMessageEntity entity = new ChatbotMessageEntity();
        entity.setId(domain.getId());
        entity.setSessionId(domain.getSessionId());
        entity.setRole(domain.getRole().name());
        entity.setContent(domain.getContent());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
