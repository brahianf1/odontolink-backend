package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatMessageEntity;

/**
 * Mapper entre ChatMessage (dominio) y ChatMessageEntity (persistencia).
 *
 * La variante "shallow" inserta una ChatSession mínima (solo ID) para evitar la recursión
 * entre sesión↔mensajes y permite a los REST mappers acceder al chatSessionId.
 *
 * @author OdontoLink Team
 */
public class ChatMessagePersistenceMapper {

    private ChatMessagePersistenceMapper() {
    }

    public static ChatMessage toDomain(ChatMessageEntity entity) {
        if (entity == null) {
            return null;
        }
        ChatMessage chatMessage = mapBaseToDomain(entity);
        if (entity.getChatSession() != null) {
            chatMessage.setChatSession(ChatSessionPersistenceMapper.toDomainShallow(entity.getChatSession()));
        }
        return chatMessage;
    }

    public static ChatMessage toDomainShallow(ChatMessageEntity entity) {
        if (entity == null) {
            return null;
        }
        ChatMessage chatMessage = mapBaseToDomain(entity);

        // Referencia mínima a la sesión (solo ID) para evitar ciclos y permitir el REST mapper.
        if (entity.getChatSession() != null) {
            ChatSession minimalSession = new ChatSession();
            minimalSession.setId(entity.getChatSession().getId());
            chatMessage.setChatSession(minimalSession);
        }
        return chatMessage;
    }

    public static ChatMessageEntity toEntity(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setId(chatMessage.getId());
        entity.setContent(chatMessage.getContent());
        entity.setSentAt(chatMessage.getSentAt());
        entity.setReadAt(chatMessage.getReadAt());

        if (chatMessage.getChatSession() != null) {
            entity.setChatSession(ChatSessionPersistenceMapper.toEntityShallow(chatMessage.getChatSession()));
        }
        if (chatMessage.getSender() != null) {
            entity.setSender(UserPersistenceMapper.toEntity(chatMessage.getSender()));
        }
        return entity;
    }

    private static ChatMessage mapBaseToDomain(ChatMessageEntity entity) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(entity.getId());
        chatMessage.setContent(entity.getContent());
        chatMessage.setSentAt(entity.getSentAt());
        chatMessage.setReadAt(entity.getReadAt());

        if (entity.getSender() != null) {
            chatMessage.setSender(UserPersistenceMapper.toDomain(entity.getSender()));
        }
        return chatMessage;
    }
}
