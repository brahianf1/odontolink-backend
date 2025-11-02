package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.repository.ChatMessageRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatSessionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaChatMessageRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.ChatMessagePersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.ChatSessionPersistenceMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para ChatMessage (Hexagonal Architecture).
 * Implementa la interfaz del dominio ChatMessageRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * Este adaptador maneja la persistencia de mensajes de chat y proporciona métodos
 * especializados para consultas con polling (obtener mensajes nuevos desde un timestamp).
 *
 * @author OdontoLink Team
 */
@Component
public class ChatMessagePersistenceAdapter implements ChatMessageRepository {

    private final JpaChatMessageRepository jpaChatMessageRepository;

    public ChatMessagePersistenceAdapter(JpaChatMessageRepository jpaChatMessageRepository) {
        this.jpaChatMessageRepository = jpaChatMessageRepository;
    }

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        var entity = ChatMessagePersistenceMapper.toEntity(chatMessage);
        var savedEntity = jpaChatMessageRepository.save(entity);
        return ChatMessagePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ChatMessage> findById(Long id) {
        return jpaChatMessageRepository.findById(id)
                .map(ChatMessagePersistenceMapper::toDomain);
    }

    @Override
    public List<ChatMessage> findByChatSessionOrderBySentAtAsc(ChatSession session) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository.findByChatSessionOrderBySentAtAsc(sessionEntity).stream()
                .map(ChatMessagePersistenceMapper::toDomainShallow)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findByChatSessionAndSentAtAfterOrderBySentAtAsc(ChatSession session, Instant sinceTimestamp) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository.findByChatSessionAndSentAtAfterOrderBySentAtAsc(sessionEntity, sinceTimestamp).stream()
                .map(ChatMessagePersistenceMapper::toDomainShallow)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findByChatSessionIdOrderBySentAtAsc(Long chatSessionId) {
        return jpaChatMessageRepository.findByChatSessionIdOrderBySentAtAsc(chatSessionId).stream()
                .map(ChatMessagePersistenceMapper::toDomainShallow)
                .collect(Collectors.toList());
    }

    @Override
    public long countByChatSession(ChatSession session) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository.countByChatSession(sessionEntity);
    }

    @Override
    public long countByChatSessionAndSentAtAfter(ChatSession session, Instant sinceTimestamp) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository.countByChatSessionAndSentAtAfter(sessionEntity, sinceTimestamp);
    }
}
