package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.User;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de entrada (Input Port) para los casos de uso de Chat.
 * Define el contrato para las operaciones del sistema de chat interno.
 *
 * Implementa los siguientes casos de uso:
 * - CU 6.1: Obtener Lista de Sesiones de Chat (El "Inbox")
 * - CU 6.2: Enviar un Mensaje (RF26)
 * - CU 6.3: Obtener Mensajes (El Endpoint de "Polling")
 *
 * @author OdontoLink Team
 */
public interface IChatUseCase {

    /**
     * Obtiene todas las sesiones de chat del usuario autenticado.
     * Implementa CU 6.1: Obtener Lista de Sesiones de Chat.
     *
     * El método determina automáticamente si el usuario es paciente o practicante
     * y devuelve sus sesiones correspondientes.
     *
     * @param user El usuario autenticado (paciente o practicante)
     * @return Lista de sesiones de chat del usuario
     */
    List<ChatSession> getMyChatSessions(User user);

    /**
     * Envía un nuevo mensaje a una sesión de chat existente.
     * Implementa CU 6.2: Enviar un Mensaje (RF26).
     *
     * Orquestación:
     * 1. Busca la ChatSession desde el repositorio
     * 2. Valida que el usuario es participante legítimo (ChatPolicyService)
     * 3. Crea el ChatMessage con el contenido
     * 4. Persiste el mensaje de forma transaccional
     * 5. Retorna el mensaje guardado
     *
     * @param chatSessionId El ID de la sesión de chat
     * @param content El contenido textual del mensaje
     * @param sender El usuario que envía el mensaje
     * @return El mensaje creado con su ID asignado
     * @throws site.utnpf.odontolink.domain.exception.ResourceNotFoundException si la sesión no existe
     * @throws site.utnpf.odontolink.domain.exception.UnauthorizedOperationException si el usuario no pertenece a la sesión
     */
    ChatMessage sendMessage(Long chatSessionId, String content, User sender);

    /**
     * Obtiene los mensajes de una sesión de chat, con soporte para polling.
     * Implementa CU 6.3: Obtener Mensajes (El Endpoint de "Polling").
     *
     * Este método soporta dos modos:
     * - Primera carga (sinceTimestamp = null): Devuelve todo el historial
     * - Polling (sinceTimestamp != null): Devuelve solo mensajes nuevos
     *
     * Orquestación:
     * 1. Busca la ChatSession desde el repositorio
     * 2. Valida que el usuario tiene acceso (ChatPolicyService)
     * 3. Si sinceTimestamp es null: obtiene todo el historial
     * 4. Si sinceTimestamp no es null: obtiene solo mensajes nuevos
     * 5. Retorna la lista de mensajes ordenada cronológicamente
     *
     * @param chatSessionId El ID de la sesión de chat
     * @param user El usuario que solicita los mensajes
     * @param sinceTimestamp Timestamp opcional para polling (null = primera carga)
     * @return Lista de mensajes ordenados por sentAt ascendente
     * @throws site.utnpf.odontolink.domain.exception.ResourceNotFoundException si la sesión no existe
     * @throws site.utnpf.odontolink.domain.exception.UnauthorizedOperationException si el usuario no tiene acceso
     */
    List<ChatMessage> getMessages(Long chatSessionId, User user, Instant sinceTimestamp);
}
