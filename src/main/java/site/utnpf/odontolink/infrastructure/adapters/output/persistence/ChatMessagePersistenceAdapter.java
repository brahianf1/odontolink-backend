package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
 * Adaptador de persistencia para ChatMessage (Hexagonal).
 *
 * Implementa el puerto del dominio {@link ChatMessageRepository} y traduce las llamadas
 * a operaciones JPA, incluyendo el bulk-update de read receipts y paginación.
 *
 * Politica transaccional uniforme con el resto de adapters; ver
 * {@link UserPersistenceAdapter} para el racional.
 *
 * @author OdontoLink Team
 */
@Component
@Transactional(readOnly = true)
public class ChatMessagePersistenceAdapter implements ChatMessageRepository {

    private final JpaChatMessageRepository jpaChatMessageRepository;

    public ChatMessagePersistenceAdapter(JpaChatMessageRepository jpaChatMessageRepository) {
        this.jpaChatMessageRepository = jpaChatMessageRepository;
    }

    @Override
    @Transactional
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

    @Override
    public List<ChatMessage> findByChatSessionPagedDesc(ChatSession session, int page, int size) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        // Sort DESC se redunda en el método del repo (findByChatSessionOrderBySentAtDesc) y aquí
        // por claridad — Spring respeta el sort del Pageable cuando el method-name no lo define.
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        return jpaChatMessageRepository.findByChatSessionOrderBySentAtDesc(sessionEntity, pageable).stream()
                .map(ChatMessagePersistenceMapper::toDomainShallow)
                .collect(Collectors.toList());
    }

    @Override
    public long countUnreadByChatSessionAndReceiver(ChatSession session, Long receiverUserId) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository.countUnreadByChatSessionAndReceiver(sessionEntity, receiverUserId);
    }

    @Override
    @Transactional
    public int markAllAsReadInSession(ChatSession session, Long receiverUserId, Instant readAt) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository.markAllAsReadInSession(sessionEntity, receiverUserId, readAt);
    }

    @Override
    public Optional<ChatMessage> findLastMessageInSession(ChatSession session) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository.findFirstByChatSessionOrderBySentAtDesc(sessionEntity)
                .map(ChatMessagePersistenceMapper::toDomainShallow);
    }
}
