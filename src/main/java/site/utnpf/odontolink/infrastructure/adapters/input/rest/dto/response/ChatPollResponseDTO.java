package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta unificada del polling del chat (CU 6.3 + CU012).
 *
 * <p>Es el envoltorio que devuelve {@code GET /api/chat/sessions/{id}/messages} cuando
 * no se está paginando. Combina mensajes nuevos y actualizaciones de lectura en una
 * sola respuesta para minimizar round-trips:
 *
 * <ul>
 *   <li><b>messages</b>: mensajes con {@code sentAt > since} (orden ASC). Si {@code since}
 *       es {@code null}, contiene el historial completo en la carga inicial.</li>
 *   <li><b>readReceipts</b>: actualizaciones de {@code readAt} sobre mensajes propios
 *       cuando la contraparte abrió la conversación desde el cursor. Vacío en la carga
 *       inicial (el cliente recién está sincronizando estado).</li>
 *   <li><b>serverTime</b>: cursor de referencia para el próximo poll. Capturado por el
 *       servidor <b>antes</b> de leer la base, así cualquier mensaje que llegue durante
 *       el procesamiento se entrega en el siguiente ciclo sin lagunas. Evita clock skew.</li>
 * </ul>
 *
 * @author OdontoLink Team
 */
public class ChatPollResponseDTO {

    private List<ChatMessageResponseDTO> messages;
    private List<ChatReadReceiptDTO> readReceipts;
    private Instant serverTime;

    public ChatPollResponseDTO() {
    }

    public ChatPollResponseDTO(List<ChatMessageResponseDTO> messages,
                               List<ChatReadReceiptDTO> readReceipts,
                               Instant serverTime) {
        this.messages = messages;
        this.readReceipts = readReceipts;
        this.serverTime = serverTime;
    }

    public List<ChatMessageResponseDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageResponseDTO> messages) {
        this.messages = messages;
    }

    public List<ChatReadReceiptDTO> getReadReceipts() {
        return readReceipts;
    }

    public void setReadReceipts(List<ChatReadReceiptDTO> readReceipts) {
        this.readReceipts = readReceipts;
    }

    public Instant getServerTime() {
        return serverTime;
    }

    public void setServerTime(Instant serverTime) {
        this.serverTime = serverTime;
    }
}
