package site.utnpf.odontolink.application.port.in.dto;

import site.utnpf.odontolink.domain.model.ChatMessage;

import java.time.Instant;
import java.util.List;

/**
 * Resultado unificado de un poll del chat (CU 6.3 + CU012).
 *
 * <p>Combina en una sola respuesta:
 * <ul>
 *   <li><b>messages</b>: mensajes nuevos desde el cursor {@code since}
 *       (mensajes con {@code sentAt > since}). Si {@code since == null} es el
 *       historial completo de la sesión (orden ASC).</li>
 *   <li><b>readReceipts</b>: actualizaciones de {@code readAt} sobre mensajes
 *       <i>propios</i> que la contraparte leyó desde el cursor. Esto es lo que
 *       resuelve el agujero histórico del polling: tu mensaje sigue siendo el
 *       mismo, pero su estado de lectura cambió.</li>
 *   <li><b>serverTime</b>: instante de referencia que el frontend debe usar como
 *       próximo {@code since}. Capturado <b>antes</b> de la query para que
 *       cualquier mensaje que llegue durante el procesamiento se entregue en el
 *       siguiente poll y no se pierda. Evita clock skew entre cliente y servidor.</li>
 * </ul>
 *
 * <p>Diseño deliberadamente independiente de Spring para no acoplar el puerto
 * de aplicación a la infraestructura web (el REST adapter lo traduce a su DTO).
 *
 * @author OdontoLink Team
 */
public class ChatPollResult {

    private final List<ChatMessage> messages;
    private final List<ReadReceipt> readReceipts;
    private final Instant serverTime;

    public ChatPollResult(List<ChatMessage> messages,
                          List<ReadReceipt> readReceipts,
                          Instant serverTime) {
        this.messages = messages;
        this.readReceipts = readReceipts;
        this.serverTime = serverTime;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public List<ReadReceipt> getReadReceipts() {
        return readReceipts;
    }

    public Instant getServerTime() {
        return serverTime;
    }
}
