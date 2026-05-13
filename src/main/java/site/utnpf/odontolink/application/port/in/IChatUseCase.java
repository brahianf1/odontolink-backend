package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.application.port.in.dto.ChatSessionView;
import site.utnpf.odontolink.application.port.in.dto.PagedMessages;
import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.User;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de entrada (Input Port) para los casos de uso de Chat.
 *
 * Cubre:
 * - RF26 / CU 6.2: Enviar mensajes
 * - RF27 / CU 6.1: Listar sesiones (inbox)
 * - CU 6.3: Polling y paginación
 * - CU012: Read receipts y conteo de no-leídos
 * - RF28: Bloqueo y desbloqueo auditable
 *
 * @author OdontoLink Team
 */
public interface IChatUseCase {

    /**
     * Lista las sesiones del usuario enriquecidas con unreadCount y lastMessage para construir
     * el inbox con badges y orden por actividad real (CU012 paso 9).
     */
    List<ChatSessionView> getMyChatSessions(User user);

    ChatMessage sendMessage(Long chatSessionId, String content, User sender);

    /**
     * Obtiene mensajes en modo polling (since != null) o el historial completo (since == null).
     * Para historial paginado usar {@link #getMessagesPaged}.
     */
    List<ChatMessage> getMessages(Long chatSessionId, User user, Instant sinceTimestamp);

    /**
     * Devuelve una página DESC del historial. Usado para carga perezosa al hacer scroll-up.
     */
    PagedMessages getMessagesPaged(Long chatSessionId, User user, int page, int size);

    /**
     * Bloquea la sesión (RF28). Solo el practicante de la sesión puede ejecutarlo.
     * @return La sesión actualizada con el rastro de bloqueo.
     */
    ChatSession blockChatSession(Long chatSessionId, User actor, String reason);

    /**
     * Desbloquea la sesión (RF28 reversible).
     */
    ChatSession unblockChatSession(Long chatSessionId, User actor);

    /**
     * Marca como leídos todos los mensajes pendientes de los que el usuario no es sender (CU012).
     * @return cantidad de mensajes marcados; útil para que el frontend decida si refrescar.
     */
    int markMessagesAsRead(Long chatSessionId, User receiver);
}
