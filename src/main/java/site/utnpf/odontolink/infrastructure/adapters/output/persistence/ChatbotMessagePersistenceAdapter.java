package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.ChatbotMessage;
import site.utnpf.odontolink.domain.repository.ChatbotMessageRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatbotMessageEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaChatbotMessageRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.ChatbotMessagePersistenceMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ChatbotMessagePersistenceAdapter implements ChatbotMessageRepository {

    private final JpaChatbotMessageRepository jpa;

    public ChatbotMessagePersistenceAdapter(JpaChatbotMessageRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ChatbotMessage> findLastNBySessionId(UUID sessionId, int n) {
        if (n <= 0) {
            return List.of();
        }
        // Pedimos los N mas recientes en DESC y revertimos a ASC para que el
        // caller los vea en orden cronologico (lo que necesita el wire-format
        // de chat completions).
        List<ChatbotMessageEntity> recentDesc = jpa.findRecentBySessionId(sessionId, PageRequest.of(0, n));
        List<ChatbotMessage> asList = new ArrayList<>(recentDesc.size());
        for (int i = recentDesc.size() - 1; i >= 0; i--) {
            asList.add(ChatbotMessagePersistenceMapper.toDomain(recentDesc.get(i)));
        }
        return asList;
    }

    @Override
    @Transactional
    public ChatbotMessage save(ChatbotMessage message) {
        ChatbotMessageEntity entity = ChatbotMessagePersistenceMapper.toEntity(message);
        ChatbotMessageEntity saved = jpa.save(entity);
        return ChatbotMessagePersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public int deleteOldestKeepingLast(UUID sessionId, int keepLast) {
        if (keepLast <= 0) {
            return jpa.deleteBySessionId(sessionId);
        }
        // Resolvemos el id de la fila en la posicion keepLast-1 desde el mas
        // reciente. Todo lo que tenga id menor a ese se borra. Aprovecha el
        // indice de PK para el DELETE.
        List<ChatbotMessageEntity> recent = jpa.findRecentBySessionId(sessionId, PageRequest.of(0, keepLast));
        if (recent.size() < keepLast) {
            return 0;
        }
        long threshold = recent.get(recent.size() - 1).getId();
        return jpa.deleteOldestBefore(sessionId, threshold);
    }

    @Override
    @Transactional
    public int deleteAllBySessionId(UUID sessionId) {
        return jpa.deleteBySessionId(sessionId);
    }

    @Override
    public long countBySessionId(UUID sessionId) {
        return jpa.countBySessionId(sessionId);
    }

    @SuppressWarnings("unused")
    private static List<ChatbotMessage> empty() {
        return Collections.emptyList();
    }
}
