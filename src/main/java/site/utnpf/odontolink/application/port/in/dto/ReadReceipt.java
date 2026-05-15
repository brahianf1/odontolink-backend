package site.utnpf.odontolink.application.port.in.dto;

import java.time.Instant;

/**
 * Notificación de lectura de un mensaje propio: el receptor abrió la conversación y
 * el mensaje pasó de no-leído a leído (CU012). Habilita que el frontend actualice el
 * indicador "doble check azul" sin re-pedir el historial.
 *
 * <p>Solo carga el {@code messageId} y el {@code readAt}: el contenido del mensaje ya
 * está en el lado del cliente desde que se envió, y reenviarlo sería desperdicio.
 *
 * @author OdontoLink Team
 */
public class ReadReceipt {

    private final Long messageId;
    private final Instant readAt;

    public ReadReceipt(Long messageId, Instant readAt) {
        this.messageId = messageId;
        this.readAt = readAt;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
