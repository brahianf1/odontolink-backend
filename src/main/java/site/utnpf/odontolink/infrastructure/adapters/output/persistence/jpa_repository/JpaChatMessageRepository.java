package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatMessageEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatSessionEntity;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio JPA para ChatMessageEntity.
 * Extiende JpaRepository de Spring Data JPA para operaciones CRUD automáticas.
 *
 * Implementa queries especializadas para el sistema de polling RESTful,
 * permitiendo consultar mensajes nuevos desde un timestamp específico.
 *
 * @author OdontoLink Team
 */
@Repository
public interface JpaChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * Encuentra todos los mensajes de una sesión ordenados cronológicamente.
     * Se usa para la carga inicial del historial completo.
     *
     * @param chatSession La entidad de la sesión de chat
     * @return Lista de mensajes ordenados por sentAt ascendente
     */
    List<ChatMessageEntity> findByChatSessionOrderBySentAtAsc(ChatSessionEntity chatSession);

    /**
     * Encuentra mensajes nuevos desde un timestamp específico.
     * Este método es crucial para el mecanismo de polling del frontend.
     *
     * @param chatSession La entidad de la sesión de chat
     * @param sinceTimestamp Timestamp desde el cual buscar mensajes (exclusivo)
     * @return Lista de mensajes enviados después del timestamp
     */
    List<ChatMessageEntity> findByChatSessionAndSentAtAfterOrderBySentAtAsc(
            ChatSessionEntity chatSession,
            Instant sinceTimestamp
    );

    /**
     * Encuentra todos los mensajes de una sesión por su ID.
     *
     * @param chatSessionId El ID de la sesión de chat
     * @return Lista de mensajes ordenados cronológicamente
     */
    List<ChatMessageEntity> findByChatSessionIdOrderBySentAtAsc(Long chatSessionId);

    /**
     * Cuenta el número total de mensajes en una sesión.
     *
     * @param chatSession La entidad de la sesión de chat
     * @return Número total de mensajes
     */
    long countByChatSession(ChatSessionEntity chatSession);

    /**
     * Cuenta mensajes nuevos desde un timestamp específico.
     * Útil para implementar badges de "mensajes no leídos".
     *
     * @param chatSession La entidad de la sesión de chat
     * @param sinceTimestamp Timestamp desde el cual contar
     * @return Número de mensajes nuevos
     */
    long countByChatSessionAndSentAtAfter(ChatSessionEntity chatSession, Instant sinceTimestamp);
}
