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
 * - Carga inicial acotada (últimos N) para no traer historiales completos sin control
 * - Polling delta con cursor inclusivo (CU 6.3 + CU012)
 * - Historial paginado (no saturar memoria con conversaciones largas)
 * - Conteo de no-leídos para badges del inbox
 * - Bulk update de read receipts
 * - Detección de read-receipts entrantes para sincronizar al sender
 * - Contador global de no-leídos para el badge del sidebar
 *
 * <p>Convención de ordenamiento estable: todos los listados ordenan por {@code sentAt} y
 * usan {@code id} como tie-breaker en la misma dirección. Sin secundaria el FE puede
 * sufrir saltos visuales o duplicados que no deduplican cuando dos mensajes comparten
 * timestamp exacto (seed batch, alta concurrencia).
 *
 * @author OdontoLink Team
 */
public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    Optional<ChatMessage> findById(Long id);

    /**
     * Últimos N mensajes de la sesión devueltos en orden cronológico ASC
     * (el más reciente queda al final, listo para renderizar).
     *
     * <p>Es la carga inicial acotada del cliente: evita el anti-pattern de devolver
     * historiales completos sin control. Para conversaciones más grandes, el FE debe
     * pasar a paginación explícita.
     */
    List<ChatMessage> findLatestInSessionAsc(ChatSession session, int limit);

    /**
     * Polling delta: mensajes con {@code sentAt >= since}, orden ASC por {@code sentAt}
     * con tie-break por {@code id} ASC.
     *
     * <p>El cursor es <b>inclusivo</b> a propósito: con cursor estricto, un mensaje que
     * se commita exactamente en el instante {@code since} (cuando el snapshot anterior
     * no llegó a verlo) se pierde para siempre. Con {@code >=} el mensaje aparece de
     * nuevo en el próximo poll y el FE lo deduplica por {@code id} (idempotencia).
     * Esta semántica está alineada con APIs modernas tipo Slack/GitHub timeline.
     */
    List<ChatMessage> findInSessionSinceInclusiveAsc(ChatSession session, Instant since);

    long countByChatSession(ChatSession session);

    /**
     * Página DESC del historial (orden por {@code sentAt DESC}, tie-break por {@code id DESC}).
     * Se devuelve en orden cronológico inverso (más reciente primero) — convención estándar
     * de paginación tipo "scroll infinito" de chats modernos.
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
     * ({@code readAt}) es posterior o igual a {@code since}. Resuelve el agujero del
     * polling clásico: con esta query el sender sincroniza el "doble check azul" sobre sus
     * mensajes sin re-pedir el historial.
     *
     * <p>Cursor inclusivo ({@code >=}) por el mismo motivo que los mensajes nuevos: evita
     * pérdida de events en el borde y permite dedupe idempotente (aplicar el mismo
     * {@code readAt} dos veces no cambia el estado).
     *
     * <p>Orden: {@code readAt ASC}, tie-break {@code id ASC}.
     */
    List<ChatMessage> findReadReceiptsForSenderSinceInclusive(ChatSession session, Long senderUserId, Instant since);
}
