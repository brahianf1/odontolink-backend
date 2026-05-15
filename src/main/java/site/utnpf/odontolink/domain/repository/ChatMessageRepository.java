package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Output Port) para operaciones de persistencia de ChatMessage.
 *
 * Cubre las necesidades de CU012:
 * - Historial completo / polling RESTful
 * - Historial paginado (no saturar memoria con conversaciones largas)
 * - Conteo de no-leídos para badges del inbox
 * - Bulk update de read receipts
 * - Detección de read-receipts entrantes para sincronizar al sender (P1)
 * - Contador global de no-leídos para el badge del sidebar (P8)
 *
 * @author OdontoLink Team
 */
public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    Optional<ChatMessage> findById(Long id);

    /** Historial completo, ascendente (más antiguo primero) — primera carga. */
    List<ChatMessage> findByChatSessionOrderBySentAtAsc(ChatSession session);

    /** Polling: solo mensajes nuevos desde el timestamp. */
    List<ChatMessage> findByChatSessionAndSentAtAfterOrderBySentAtAsc(ChatSession session, Instant sinceTimestamp);

    List<ChatMessage> findByChatSessionIdOrderBySentAtAsc(Long chatSessionId);

    long countByChatSession(ChatSession session);

    long countByChatSessionAndSentAtAfter(ChatSession session, Instant sinceTimestamp);

    /**
     * Página DESC del historial. Se devuelve en orden cronológico inverso (más reciente primero)
     * porque es el contrato estándar de paginación tipo "scroll infinito" de chats modernos.
     */
    List<ChatMessage> findByChatSessionPagedDesc(ChatSession session, int page, int size);

    /**
     * Cuenta mensajes no-leídos de los que el receptor NO es el sender.
     * Usado para construir el badge "X no leídos" del inbox.
     */
    long countUnreadByChatSessionAndReceiver(ChatSession session, Long receiverUserId);

    /**
     * Suma los no-leídos de TODAS las sesiones donde el usuario es participante. Alimenta el
     * badge global del sidebar/AppBar (CU012 - P8). Una sola query agregada para evitar el
     * fan-out de iterar sesiones desde la capa de aplicación.
     */
    long countTotalUnreadByReceiver(Long receiverUserId);

    /**
     * Marca como leídos en una sola sentencia UPDATE todos los mensajes pendientes de la contraparte.
     * @return número de filas afectadas.
     */
    int markAllAsReadInSession(ChatSession session, Long receiverUserId, Instant readAt);

    /** Último mensaje de la sesión, para ordenar el inbox por actividad real. */
    Optional<ChatMessage> findLastMessageInSession(ChatSession session);

    /**
     * Mensajes <i>enviados por</i> {@code senderUserId} en la sesión cuya marca de lectura
     * ({@code readAt}) cambió a un valor posterior a {@code since}. Resuelve el agujero del
     * polling clásico: con esta query el sender puede sincronizar el "doble check azul"
     * sobre sus mensajes sin re-pedir el historial.
     *
     * <p>Los retorna ordenados por {@code readAt} ASC para que el frontend los aplique en
     * orden cronológico.
     */
    List<ChatMessage> findReadReceiptsForSenderSince(ChatSession session, Long senderUserId, Instant since);
}
