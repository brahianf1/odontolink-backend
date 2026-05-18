package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.ChatbotMessage;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida para los mensajes del rolling buffer del chatbot (RF29).
 *
 * <p>El cap se aplica desde el servicio (no aqui) llamando a
 * {@link #deleteOldestKeepingLast(UUID, int)} tras cada inserto cuando el
 * count supera el limite configurado.
 */
public interface ChatbotMessageRepository {

    /** Devuelve los ultimos N mensajes de la sesion en orden cronologico ASC. */
    List<ChatbotMessage> findLastNBySessionId(UUID sessionId, int n);

    ChatbotMessage save(ChatbotMessage message);

    /**
     * Mantiene solo los {@code keepLast} mensajes mas recientes de la sesion;
     * borra los demas (FIFO). Devuelve la cantidad de mensajes borrados.
     */
    int deleteOldestKeepingLast(UUID sessionId, int keepLast);

    /** Borra todos los mensajes de la sesion (al cerrar/borrar la sesion). */
    int deleteAllBySessionId(UUID sessionId);

    long countBySessionId(UUID sessionId);
}
