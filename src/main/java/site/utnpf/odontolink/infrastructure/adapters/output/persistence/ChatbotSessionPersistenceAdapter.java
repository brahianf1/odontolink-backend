package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.ChatbotSession;
import site.utnpf.odontolink.domain.repository.ChatbotSessionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatbotSessionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaChatbotSessionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.ChatbotSessionPersistenceMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ChatbotSessionPersistenceAdapter implements ChatbotSessionRepository {

    private final JpaChatbotSessionRepository jpa;

    public ChatbotSessionPersistenceAdapter(JpaChatbotSessionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ChatbotSession> findById(UUID id) {
        return jpa.findById(id).map(ChatbotSessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ChatbotSession> findByAnonymousToken(UUID anonymousToken) {
        return jpa.findByAnonymousToken(anonymousToken)
                .map(ChatbotSessionPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public ChatbotSession save(ChatbotSession session) {
        ChatbotSessionEntity entity = ChatbotSessionPersistenceMapper.toEntity(session);
        ChatbotSessionEntity saved = jpa.save(entity);
        return ChatbotSessionPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        if (jpa.existsById(id)) {
            jpa.deleteById(id);
        }
    }

    @Override
    @Transactional
    public int deleteIdleOlderThan(Instant cutoff) {
        return jpa.deleteIdleOlderThan(cutoff);
    }
}
