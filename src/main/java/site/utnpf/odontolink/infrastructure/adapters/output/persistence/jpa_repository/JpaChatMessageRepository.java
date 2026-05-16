package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatMessageEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatSessionEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para ChatMessageEntity.
 *
 * Soporta tres modos de consulta del CU012:
 * 1. Historial completo / polling (existentes).
 * 2. Historial paginado para no saturar memoria en chats largos.
 * 3. Conteo de mensajes no-leídos del usuario autenticado (badges UX).
 *
 * @author OdontoLink Team
 */
@Repository
public interface JpaChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findByChatSessionOrderBySentAtAsc(ChatSessionEntity chatSession);

    List<ChatMessageEntity> findByChatSessionAndSentAtAfterOrderBySentAtAsc(
            ChatSessionEntity chatSession,
            Instant sinceTimestamp
    );

    List<ChatMessageEntity> findByChatSessionIdOrderBySentAtAsc(Long chatSessionId);

    long countByChatSession(ChatSessionEntity chatSession);

    long countByChatSessionAndSentAtAfter(ChatSessionEntity chatSession, Instant sinceTimestamp);

    /**
     * Página de mensajes ordenados por sentAt DESC. Diseñado para "carga inicial perezosa":
     * el cliente pide la página 0 (los más recientes), y al hacer scroll-up pide páginas
     * sucesivas. La página DESC es la convención estándar de WhatsApp/Telegram.
     */
    List<ChatMessageEntity> findByChatSessionOrderBySentAtDesc(ChatSessionEntity chatSession, Pageable pageable);

    /**
     * Cuenta los mensajes no-leídos de los que el receptor NO es el sender.
     * Esta query es la base del badge "X no leídos" del inbox (CU012 paso 9).
     */
    @Query("SELECT COUNT(m) FROM ChatMessageEntity m " +
           "WHERE m.chatSession = :session " +
           "AND m.readAt IS NULL " +
           "AND m.sender.id <> :receiverUserId")
    long countUnreadByChatSessionAndReceiver(@Param("session") ChatSessionEntity session,
                                             @Param("receiverUserId") Long receiverUserId);

    /**
     * Bulk-update que marca como leídos todos los mensajes no-leídos enviados por la contraparte.
     * Lo hacemos en una sola sentencia UPDATE para evitar el patrón N+1 (un select-update por mensaje)
     * cuando un usuario abre una conversación con cientos de mensajes pendientes.
     *
     * @return cantidad de filas actualizadas (útil para el frontend, p. ej. para decidir si refrescar).
     */
    @Modifying
    @Query("UPDATE ChatMessageEntity m SET m.readAt = :readAt " +
           "WHERE m.chatSession = :session " +
           "AND m.readAt IS NULL " +
           "AND m.sender.id <> :receiverUserId")
    int markAllAsReadInSession(@Param("session") ChatSessionEntity session,
                               @Param("receiverUserId") Long receiverUserId,
                               @Param("readAt") Instant readAt);

    /**
     * Último mensaje de la sesión. Se usa para ordenar el inbox por actividad real
     * (en lugar de por createdAt de la sesión, que solo refleja la primera atención).
     */
    Optional<ChatMessageEntity> findFirstByChatSessionOrderBySentAtDesc(ChatSessionEntity chatSession);

    /**
     * Suma los no-leídos de TODAS las sesiones (paciente o practicante) donde el usuario
     * indicado es participante. Usado por el badge global del sidebar (CU012 - P8).
     *
     * <p>Se filtra por participación (el usuario debe ser el patient.user o el practitioner.user
     * de la sesión) y por que el sender del mensaje NO sea él (los propios mensajes nunca
     * cuentan como no-leídos).
     */
    @Query("SELECT COUNT(m) FROM ChatMessageEntity m " +
           "WHERE m.readAt IS NULL " +
           "AND m.sender.id <> :userId " +
           "AND (m.chatSession.patient.user.id = :userId " +
           "     OR m.chatSession.practitioner.user.id = :userId)")
    long countTotalUnreadByReceiver(@Param("userId") Long userId);

    /**
     * Read-receipts entrantes para el sender (P1): mensajes que envió {@code senderUserId}
     * en la sesión, cuya marca {@code readAt} es posterior a {@code since}.
     *
     * <p>Devuelve los mensajes completos (el caller solo expone messageId + readAt al cliente).
     * Orden ASC por {@code readAt} para que el frontend los aplique en orden cronológico.
     */
    @Query("SELECT m FROM ChatMessageEntity m " +
           "WHERE m.chatSession = :session " +
           "AND m.sender.id = :senderUserId " +
           "AND m.readAt IS NOT NULL " +
           "AND m.readAt > :since " +
           "ORDER BY m.readAt ASC")
    List<ChatMessageEntity> findReadReceiptsForSenderSince(@Param("session") ChatSessionEntity session,
                                                           @Param("senderUserId") Long senderUserId,
                                                           @Param("since") Instant since);
}
