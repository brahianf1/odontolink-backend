package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.ChatMessage;
import site.utnpf.odontolink.domain.model.ChatSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Output Port) para operaciones de persistencia de ChatMessage.
 * Sigue los principios de Arquitectura Hexagonal (Ports and Adapters).
 *
 * Este repositorio proporciona métodos para:
 * - Crear y guardar mensajes de chat
 * - Consultar mensajes por sesión
 * - Implementar polling: consultar mensajes nuevos desde un timestamp
 * - Obtener el historial completo de mensajes ordenado cronológicamente
 *
 * @author OdontoLink Team
 */
public interface ChatMessageRepository {

    /**
     * Guarda un nuevo mensaje de chat.
     *
     * @param chatMessage El mensaje a guardar
     * @return El mensaje guardado con su ID asignado
     */
    ChatMessage save(ChatMessage chatMessage);

    /**
     * Busca un mensaje por su ID.
     *
     * @param id El ID del mensaje
     * @return Optional conteniendo el mensaje si existe
     */
    Optional<ChatMessage> findById(Long id);

    /**
     * Obtiene todos los mensajes de una sesión de chat ordenados cronológicamente.
     * Implementa CU 6.3: Obtener Mensajes (primera carga del historial).
     *
     * @param session La sesión de chat cuyo historial se quiere consultar
     * @return Lista de mensajes ordenados por sentAt ascendente (del más antiguo al más nuevo)
     */
    List<ChatMessage> findByChatSessionOrderBySentAtAsc(ChatSession session);

    /**
     * Obtiene los mensajes nuevos de una sesión de chat desde un timestamp específico.
     * Implementa CU 6.3: Obtener Mensajes (polling para nuevos mensajes).
     *
     * Este método es crucial para el sistema de polling RESTful, permitiendo al frontend
     * solicitar solo los mensajes que se enviaron después del último que recibió.
     *
     * @param session La sesión de chat
     * @param sinceTimestamp Timestamp desde el cual buscar mensajes nuevos (exclusivo)
     * @return Lista de mensajes enviados después del timestamp, ordenados por sentAt ascendente
     */
    List<ChatMessage> findByChatSessionAndSentAtAfterOrderBySentAtAsc(ChatSession session, Instant sinceTimestamp);

    /**
     * Obtiene todos los mensajes de una sesión de chat por su ID, ordenados cronológicamente.
     *
     * @param chatSessionId El ID de la sesión de chat
     * @return Lista de mensajes ordenados por sentAt ascendente
     */
    List<ChatMessage> findByChatSessionIdOrderBySentAtAsc(Long chatSessionId);

    /**
     * Cuenta el número de mensajes en una sesión de chat.
     *
     * @param session La sesión de chat
     * @return El número total de mensajes en la sesión
     */
    long countByChatSession(ChatSession session);

    /**
     * Cuenta el número de mensajes nuevos desde un timestamp específico.
     * Útil para mostrar badges de "mensajes no leídos" en el frontend.
     *
     * @param session La sesión de chat
     * @param sinceTimestamp Timestamp desde el cual contar mensajes nuevos
     * @return El número de mensajes nuevos
     */
    long countByChatSessionAndSentAtAfter(ChatSession session, Instant sinceTimestamp);
}
