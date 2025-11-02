package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatSessionEntity;

import java.util.stream.Collectors;

/**
 * Mapper para convertir entre ChatSession (dominio) y ChatSessionEntity (persistencia).
 *
 * Este mapper sigue el patrón de evitar ciclos infinitos al mapear relaciones bidireccionales.
 * Las versiones "shallow" excluyen las colecciones de mensajes para evitar recursión infinita.
 *
 * @author OdontoLink Team
 */
public class ChatSessionPersistenceMapper {

    private ChatSessionPersistenceMapper() {
        // Utility class
    }

    /**
     * Convierte de entidad JPA a modelo de dominio (versión completa con mensajes).
     *
     * @param entity Entidad JPA ChatSessionEntity
     * @return Modelo de dominio ChatSession
     */
    public static ChatSession toDomain(ChatSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        ChatSession chatSession = new ChatSession();
        chatSession.setId(entity.getId());
        chatSession.setCreatedAt(entity.getCreatedAt());

        // Mapear Patient y Practitioner
        if (entity.getPatient() != null) {
            chatSession.setPatient(PatientPersistenceMapper.toDomain(entity.getPatient()));
        }

        if (entity.getPractitioner() != null) {
            chatSession.setPractitioner(PractitionerPersistenceMapper.toDomain(entity.getPractitioner()));
        }

        // Mapear mensajes (si existen)
        if (entity.getMessages() != null) {
            chatSession.setMessages(
                entity.getMessages().stream()
                    .map(ChatMessagePersistenceMapper::toDomainShallow)
                    .collect(Collectors.toList())
            );
        }

        return chatSession;
    }

    /**
     * Convierte de entidad JPA a modelo de dominio (versión shallow sin mensajes).
     * Se usa cuando solo necesitamos la información básica de la sesión.
     *
     * @param entity Entidad JPA ChatSessionEntity
     * @return Modelo de dominio ChatSession sin mensajes
     */
    public static ChatSession toDomainShallow(ChatSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        ChatSession chatSession = new ChatSession();
        chatSession.setId(entity.getId());
        chatSession.setCreatedAt(entity.getCreatedAt());

        // Mapear Patient y Practitioner
        if (entity.getPatient() != null) {
            chatSession.setPatient(PatientPersistenceMapper.toDomain(entity.getPatient()));
        }

        if (entity.getPractitioner() != null) {
            chatSession.setPractitioner(PractitionerPersistenceMapper.toDomain(entity.getPractitioner()));
        }

        // No mapeamos mensajes en la versión shallow

        return chatSession;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA (versión completa).
     *
     * @param chatSession Modelo de dominio ChatSession
     * @return Entidad JPA ChatSessionEntity
     */
    public static ChatSessionEntity toEntity(ChatSession chatSession) {
        if (chatSession == null) {
            return null;
        }

        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setId(chatSession.getId());
        entity.setCreatedAt(chatSession.getCreatedAt());

        // Mapear Patient y Practitioner
        if (chatSession.getPatient() != null) {
            entity.setPatient(PatientPersistenceMapper.toEntity(chatSession.getPatient()));
        }

        if (chatSession.getPractitioner() != null) {
            entity.setPractitioner(PractitionerPersistenceMapper.toEntity(chatSession.getPractitioner()));
        }

        // Mapear mensajes (si existen)
        if (chatSession.getMessages() != null) {
            entity.setMessages(
                chatSession.getMessages().stream()
                    .map(ChatMessagePersistenceMapper::toEntity)
                    .collect(Collectors.toList())
            );
        }

        return entity;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA (versión shallow sin mensajes).
     *
     * @param chatSession Modelo de dominio ChatSession
     * @return Entidad JPA ChatSessionEntity sin mensajes
     */
    public static ChatSessionEntity toEntityShallow(ChatSession chatSession) {
        if (chatSession == null) {
            return null;
        }

        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setId(chatSession.getId());
        entity.setCreatedAt(chatSession.getCreatedAt());

        // Mapear Patient y Practitioner
        if (chatSession.getPatient() != null) {
            entity.setPatient(PatientPersistenceMapper.toEntity(chatSession.getPatient()));
        }

        if (chatSession.getPractitioner() != null) {
            entity.setPractitioner(PractitionerPersistenceMapper.toEntity(chatSession.getPractitioner()));
        }

        // No mapeamos mensajes en la versión shallow

        return entity;
    }
}
