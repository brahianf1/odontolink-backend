package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatMessageEntity;

/**
 * Mapper para convertir entre ChatMessage (dominio) y ChatMessageEntity (persistencia).
 *
 * Este mapper evita ciclos infinitos al mapear la relación con ChatSession.
 * La versión "shallow" no incluye la sesión completa para evitar recursión.
 *
 * @author OdontoLink Team
 */
public class ChatMessagePersistenceMapper {

    private ChatMessagePersistenceMapper() {
        // Utility class
    }

    /**
     * Convierte de entidad JPA a modelo de dominio (versión completa).
     *
     * @param entity Entidad JPA ChatMessageEntity
     * @return Modelo de dominio ChatMessage
     */
    public static ChatMessage toDomain(ChatMessageEntity entity) {
        if (entity == null) {
            return null;
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(entity.getId());
        chatMessage.setContent(entity.getContent());
        chatMessage.setSentAt(entity.getSentAt());

        // Mapear ChatSession con versión shallow para evitar recursión infinita
        if (entity.getChatSession() != null) {
            chatMessage.setChatSession(ChatSessionPersistenceMapper.toDomainShallow(entity.getChatSession()));
        }

        // Mapear User (sender)
        if (entity.getSender() != null) {
            chatMessage.setSender(UserPersistenceMapper.toDomain(entity.getSender()));
        }

        return chatMessage;
    }

    /**
     * Convierte de entidad JPA a modelo de dominio (versión shallow).
     * Esta versión incluye una referencia básica a la sesión (solo con ID) para evitar
     * ciclos infinitos mientras permite acceder al chatSessionId en las respuestas REST.
     *
     * @param entity Entidad JPA ChatMessageEntity
     * @return Modelo de dominio ChatMessage con sesión shallow
     */
    public static ChatMessage toDomainShallow(ChatMessageEntity entity) {
        if (entity == null) {
            return null;
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(entity.getId());
        chatMessage.setContent(entity.getContent());
        chatMessage.setSentAt(entity.getSentAt());

        // Mapear ChatSession de forma mínima (solo ID) para evitar ciclos
        // pero permitiendo acceso al chatSessionId en el REST mapper
        if (entity.getChatSession() != null) {
            ChatSession minimalSession = new ChatSession();
            minimalSession.setId(entity.getChatSession().getId());
            chatMessage.setChatSession(minimalSession);
        }

        // Mapear User (sender)
        if (entity.getSender() != null) {
            chatMessage.setSender(UserPersistenceMapper.toDomain(entity.getSender()));
        }

        return chatMessage;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     *
     * @param chatMessage Modelo de dominio ChatMessage
     * @return Entidad JPA ChatMessageEntity
     */
    public static ChatMessageEntity toEntity(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }

        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setId(chatMessage.getId());
        entity.setContent(chatMessage.getContent());
        entity.setSentAt(chatMessage.getSentAt());

        // Mapear ChatSession con versión shallow
        if (chatMessage.getChatSession() != null) {
            entity.setChatSession(ChatSessionPersistenceMapper.toEntityShallow(chatMessage.getChatSession()));
        }

        // Mapear User (sender)
        if (chatMessage.getSender() != null) {
            entity.setSender(UserPersistenceMapper.toEntity(chatMessage.getSender()));
        }

        return entity;
    }
}
