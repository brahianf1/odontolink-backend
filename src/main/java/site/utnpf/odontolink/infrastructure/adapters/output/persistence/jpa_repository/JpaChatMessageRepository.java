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
 * <p>Convención: todas las queries que listan mensajes ordenan por {@code sentAt} y usan
 * {@code id} como tie-breaker en la misma dirección. Garantiza orden estable cuando dos
 * mensajes comparten {@code sentAt} (seeds batch, alta concurrencia) para que el dedupe +
 * scroll-up del FE no sufra saltos.
 *
 * <p>Los cursores temporales son <b>inclusivos</b> ({@code >=}) en lugar de estrictos
 * ({@code >}) para no perder eventos cuya marca cae exactamente en el instante del cursor
 * previo. El FE deduplica por id, así que ver el mensaje borde dos veces es seguro.
 *
 * @author OdontoLink Team
 */
@Repository
public interface JpaChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    long countByChatSession(ChatSessionEntity chatSession);

    /**
     * Página de mensajes ordenados DESC con tie-break por id (convención WhatsApp/Telegram).
     * Se usa también para la carga inicial acotada (los últimos N) — el caller indica el
     * tamaño vía {@link Pageable} y el adapter invierte el orden a ASC si lo necesita.
     */
    @Query("SELECT m FROM ChatMessageEntity m " +
           "WHERE m.chatSession = :session " +
           "ORDER BY m.sentAt DESC, m.id DESC")
    List<ChatMessageEntity> findInSessionOrderedDesc(@Param("session") ChatSessionEntity chatSession,
                                                     Pageable pageable);

    /**
     * Polling delta inclusivo: {@code sentAt >= since}, ASC + tie-break por id.
     */
    @Query("SELECT m FROM ChatMessageEntity m " +
           "WHERE m.chatSession = :session " +
           "AND m.sentAt >= :since " +
           "ORDER BY m.sentAt ASC, m.id ASC")
    List<ChatMessageEntity> findInSessionSinceInclusiveAsc(@Param("session") ChatSessionEntity chatSession,
                                                           @Param("since") Instant since);

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
     * Una sola sentencia UPDATE evita el patrón N+1 cuando un usuario abre una conversación con
     * cientos de mensajes pendientes.
     *
     * @return cantidad de filas actualizadas (útil para que el frontend decida si refrescar).
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
     * Último mensaje de la sesión, para ordenar el inbox por actividad real. Tie-break por id
     * para deduplicar el caso (raro) de dos mensajes con el mismo {@code sentAt}.
     */
    @Query("SELECT m FROM ChatMessageEntity m " +
           "WHERE m.chatSession = :session " +
           "ORDER BY m.sentAt DESC, m.id DESC")
    List<ChatMessageEntity> findLastMessageInSession(@Param("session") ChatSessionEntity chatSession,
                                                     Pageable pageable);

    /**
     * Suma los no-leídos de TODAS las sesiones (paciente o practicante) donde el usuario
     * indicado es participante. Usado por el badge global del sidebar (CU012 - P8).
     */
    @Query("SELECT COUNT(m) FROM ChatMessageEntity m " +
           "WHERE m.readAt IS NULL " +
           "AND m.sender.id <> :userId " +
           "AND (m.chatSession.patient.user.id = :userId " +
           "     OR m.chatSession.practitioner.user.id = :userId)")
    long countTotalUnreadByReceiver(@Param("userId") Long userId);

    /**
     * Read-receipts entrantes para el sender: mensajes que envió {@code senderUserId} en la
     * sesión, cuya marca {@code readAt} es posterior o igual a {@code since}. Orden ASC por
     * {@code readAt} con tie-break por id para aplicación cronológica idempotente.
     */
    @Query("SELECT m FROM ChatMessageEntity m " +
           "WHERE m.chatSession = :session " +
           "AND m.sender.id = :senderUserId " +
           "AND m.readAt IS NOT NULL " +
           "AND m.readAt >= :since " +
           "ORDER BY m.readAt ASC, m.id ASC")
    List<ChatMessageEntity> findReadReceiptsForSenderSinceInclusive(@Param("session") ChatSessionEntity session,
                                                                    @Param("senderUserId") Long senderUserId,
                                                                    @Param("since") Instant since);
}
