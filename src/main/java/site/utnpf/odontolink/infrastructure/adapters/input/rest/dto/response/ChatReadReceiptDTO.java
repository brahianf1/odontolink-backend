package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.time.Instant;

/**
 * Read-receipt entrante para un mensaje propio: la contraparte abrió la conversación y
 * el mensaje pasó de no-leído a leído (CU012). Solo lleva los campos necesarios para
 * actualizar el indicador "doble check azul" sin transmitir contenido redundante.
 *
 * @author OdontoLink Team
 */
public class ChatReadReceiptDTO {

    private Long messageId;
    private Instant readAt;

    public ChatReadReceiptDTO() {
    }

    public ChatReadReceiptDTO(Long messageId, Instant readAt) {
        this.messageId = messageId;
        this.readAt = readAt;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
