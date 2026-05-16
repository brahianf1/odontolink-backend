package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.repository.ChatMessageRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatMessageEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatSessionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaChatMessageRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.ChatMessagePersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.ChatSessionPersistenceMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para ChatMessage (Hexagonal).
 *
 * Implementa el puerto del dominio {@link ChatMessageRepository} y traduce las llamadas
 * a operaciones JPA, incluyendo el bulk-update de read receipts, la carga acotada inicial
 * y la paginación.
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
    public List<ChatMessage> findLatestInSessionAsc(ChatSession session, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        // Fetch DESC limit N (los más recientes) y revertimos a ASC para que el más reciente
        // quede al final — orden natural de render del chat.
        List<ChatMessageEntity> latestDesc = jpaChatMessageRepository
                .findInSessionOrderedDesc(sessionEntity, PageRequest.of(0, limit));
        List<ChatMessageEntity> ascending = new ArrayList<>(latestDesc);
        Collections.reverse(ascending);
        return ascending.stream()
                .map(ChatMessagePersistenceMapper::toDomainShallow)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findInSessionSinceInclusiveAsc(ChatSession session, Instant since) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository.findInSessionSinceInclusiveAsc(sessionEntity, since).stream()
                .map(ChatMessagePersistenceMapper::toDomainShallow)
                .collect(Collectors.toList());
    }

    @Override
    public long countByChatSession(ChatSession session) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository.countByChatSession(sessionEntity);
    }

    @Override
    public List<ChatMessage> findByChatSessionPagedDesc(ChatSession session, int page, int size) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        // ORDER BY de la query JPQL (sentAt DESC, id DESC) tiene precedencia sobre cualquier
        // Sort en el Pageable, así que solo enviamos page y size.
        return jpaChatMessageRepository
                .findInSessionOrderedDesc(sessionEntity, PageRequest.of(page, size)).stream()
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
        List<ChatMessageEntity> top = jpaChatMessageRepository
                .findLastMessageInSession(sessionEntity, PageRequest.of(0, 1));
        return top.isEmpty()
                ? Optional.empty()
                : Optional.of(ChatMessagePersistenceMapper.toDomainShallow(top.get(0)));
    }

    @Override
    public long countTotalUnreadByReceiver(Long receiverUserId) {
        return jpaChatMessageRepository.countTotalUnreadByReceiver(receiverUserId);
    }

    @Override
    public List<ChatMessage> findReadReceiptsForSenderSinceInclusive(ChatSession session, Long senderUserId, Instant since) {
        ChatSessionEntity sessionEntity = ChatSessionPersistenceMapper.toEntityShallow(session);
        return jpaChatMessageRepository
                .findReadReceiptsForSenderSinceInclusive(sessionEntity, senderUserId, since)
                .stream()
                .map(ChatMessagePersistenceMapper::toDomainShallow)
                .collect(Collectors.toList());
    }
}
