package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatbotMessageEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaChatbotMessageRepository extends JpaRepository<ChatbotMessageEntity, Long> {

    /** Devuelve los mensajes de la sesion en orden DESC para tomar los ultimos N via Pageable. */
    @Query("SELECT m FROM ChatbotMessageEntity m WHERE m.sessionId = :sessionId ORDER BY m.createdAt DESC")
    List<ChatbotMessageEntity> findRecentBySessionId(@Param("sessionId") UUID sessionId, Pageable pageable);

    long countBySessionId(UUID sessionId);

    @Modifying
    int deleteBySessionId(UUID sessionId);

    /**
     * Borra todos los mensajes de la sesion cuyo {@code id} sea menor que el
     * umbral indicado. Lo usa el adapter para mantener el cap FIFO: calcula
     * el id del mensaje en la posicion {@code keepLast} desde el mas reciente
     * y borra todo lo anterior. Usar id como cursor es mas barato que filtrar
     * por createdAt y aprovecha el indice de PK.
     */
    @Modifying
    @Query("DELETE FROM ChatbotMessageEntity m WHERE m.sessionId = :sessionId AND m.id < :idThreshold")
    int deleteOldestBefore(@Param("sessionId") UUID sessionId, @Param("idThreshold") long idThreshold);
}
