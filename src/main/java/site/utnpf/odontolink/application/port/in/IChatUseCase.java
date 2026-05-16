package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.application.port.in.dto.ChatPollResult;
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
 * - RF27 / CU 6.1: Listar sesiones (inbox), con creación explícita de sesión
 * - CU 6.3: Polling y paginación
 * - CU012: Read receipts y conteo de no-leídos (incluido contador global del sidebar)
 * - RF28: Bloqueo y desbloqueo auditable
 *
 * @author OdontoLink Team
 */
public interface IChatUseCase {

    /**
     * Lista las sesiones del usuario enriquecidas con unreadCount y lastMessage para construir
     * el inbox con badges y orden por actividad real (CU012 paso 9).
     *
     * @param since si no es {@code null}, filtra a las sesiones que tuvieron actividad
     *              relevante desde ese instante (al menos un mensaje con
     *              {@code sentAt > since}). Útil para polling delta del inbox.
     */
    List<ChatSessionView> getMyChatSessions(User user, Instant since);

    ChatMessage sendMessage(Long chatSessionId, String content, User sender);

    /**
     * Polling unificado para una sesión (CU 6.3 + CU012).
     *
     * <p>Si {@code sinceTimestamp == null} devuelve el historial completo en orden ASC;
     * si está presente, devuelve solo los mensajes nuevos posteriores al cursor más los
     * read-receipts (cambios de {@code readAt}) sobre mensajes propios desde el cursor.
     * En ambos casos lleva {@code serverTime} para usarlo como próximo {@code since}.
     */
    ChatPollResult getMessagesPoll(Long chatSessionId, User user, Instant sinceTimestamp);

    /**
     * Devuelve una página DESC del historial. Usado para carga perezosa al hacer scroll-up.
     */
    PagedMessages getMessagesPaged(Long chatSessionId, User user, int page, int size);

    /**
     * Obtiene la sesión entre paciente y practicante, creándola si no existe pero
     * <b>solo</b> si los dos tienen relación clínica previa (RF27: al menos un appointment
     * registrado entre ambos). Idempotente: si ya existe la devuelve sin re-crearla.
     *
     * @param actor el usuario autenticado, que debe ser uno de los dos participantes
     * @param patientId el paciente de la sesión (cuando actor es practitioner debe coincidir consigo mismo si lo manda; cuando actor es patient se ignora y se toma del actor)
     * @param practitionerId análogo
     */
    ChatSession getOrCreateSession(User actor, Long patientId, Long practitionerId);

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
     * <b>Funciona aunque la sesión esté bloqueada</b>: el lado silenciado igual debe poder cerrar
     * su contador de no-leídos sobre el historial existente.
     * @return cantidad de mensajes marcados; útil para que el frontend decida si refrescar.
     */
    int markMessagesAsRead(Long chatSessionId, User receiver);

    /**
     * Total de mensajes no-leídos sumando todas las sesiones del usuario. Alimenta el badge
     * global de notificaciones (sidebar/AppBar) sin necesidad de iterar el inbox.
     */
    long getTotalUnreadCount(User user);
}
