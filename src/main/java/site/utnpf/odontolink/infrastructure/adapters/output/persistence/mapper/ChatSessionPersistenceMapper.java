package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatSessionEntity;

import java.util.stream.Collectors;

/**
 * Mapper entre ChatSession (dominio) y ChatSessionEntity (persistencia).
 *
 * Las variantes "shallow" se usan para romper ciclos con ChatMessage en mappeos bidireccionales.
 *
 * @author OdontoLink Team
 */
public class ChatSessionPersistenceMapper {

    private ChatSessionPersistenceMapper() {
    }

    public static ChatSession toDomain(ChatSessionEntity entity) {
        if (entity == null) {
            return null;
        }
        ChatSession chatSession = mapBaseToDomain(entity);

        if (entity.getMessages() != null) {
            chatSession.setMessages(
                entity.getMessages().stream()
                    .map(ChatMessagePersistenceMapper::toDomainShallow)
                    .collect(Collectors.toList())
            );
        }
        return chatSession;
    }

    public static ChatSession toDomainShallow(ChatSessionEntity entity) {
        if (entity == null) {
            return null;
        }
        return mapBaseToDomain(entity);
    }

    public static ChatSessionEntity toEntity(ChatSession chatSession) {
        if (chatSession == null) {
            return null;
        }
        ChatSessionEntity entity = mapBaseToEntity(chatSession);

        if (chatSession.getMessages() != null) {
            entity.setMessages(
                chatSession.getMessages().stream()
                    .map(ChatMessagePersistenceMapper::toEntity)
                    .collect(Collectors.toList())
            );
        }
        return entity;
    }

    public static ChatSessionEntity toEntityShallow(ChatSession chatSession) {
        if (chatSession == null) {
            return null;
        }
        return mapBaseToEntity(chatSession);
    }

    // Helpers privados que centralizan el mapeo de campos base + bloqueo (RF28).

    private static ChatSession mapBaseToDomain(ChatSessionEntity entity) {
        ChatSession chatSession = new ChatSession();
        chatSession.setId(entity.getId());
        chatSession.setCreatedAt(entity.getCreatedAt());

        if (entity.getPatient() != null) {
            chatSession.setPatient(PatientPersistenceMapper.toDomain(entity.getPatient()));
        }
        if (entity.getPractitioner() != null) {
            chatSession.setPractitioner(PractitionerPersistenceMapper.toDomain(entity.getPractitioner()));
        }

        // Mapeo de campos de bloqueo (RF28)
        chatSession.setBlocked(entity.isBlocked());
        chatSession.setBlockedAt(entity.getBlockedAt());
        chatSession.setBlockedByRole(entity.getBlockedByRole());
        chatSession.setBlockReason(entity.getBlockReason());
        if (entity.getBlockedByUser() != null) {
            chatSession.setBlockedByUser(UserPersistenceMapper.toDomain(entity.getBlockedByUser()));
        }
        return chatSession;
    }

    private static ChatSessionEntity mapBaseToEntity(ChatSession chatSession) {
        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setId(chatSession.getId());
        entity.setCreatedAt(chatSession.getCreatedAt());

        if (chatSession.getPatient() != null) {
            entity.setPatient(PatientPersistenceMapper.toEntity(chatSession.getPatient()));
        }
        if (chatSession.getPractitioner() != null) {
            entity.setPractitioner(PractitionerPersistenceMapper.toEntity(chatSession.getPractitioner()));
        }

        // Mapeo de campos de bloqueo (RF28)
        entity.setBlocked(chatSession.isBlocked());
        entity.setBlockedAt(chatSession.getBlockedAt());
        entity.setBlockedByRole(chatSession.getBlockedByRole());
        entity.setBlockReason(chatSession.getBlockReason());
        if (chatSession.getBlockedByUser() != null) {
            entity.setBlockedByUser(UserPersistenceMapper.toEntity(chatSession.getBlockedByUser()));
        }
        return entity;
    }
}
