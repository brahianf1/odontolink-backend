package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.time.Instant;

/**
 * DTO de respuesta para la operación bulk de read receipts.
 * Implementa CU012: el frontend usa esta respuesta para actualizar su estado local
 * sin tener que volver a pedir el historial completo.
 *
 * Composición de la respuesta:
 *  - chatSessionId: eco del recurso afectado (útil cuando el frontend dispara varias en paralelo).
 *  - messagesMarked: cuántos mensajes pasaron a "leídos" en esta llamada (puede ser 0 si la
 *    operación es idempotente: una segunda llamada no marca nada porque ya está todo leído).
 *  - readAt: timestamp uniforme que el frontend puede aplicar a los mensajes locales.
 *
 * @author OdontoLink Team
 */
public class MarkMessagesAsReadResponseDTO {

    /**
     * ID de la sesión a la que pertenecen los mensajes marcados como leídos.
     */
    private Long chatSessionId;

    /**
     * Cantidad de mensajes que fueron marcados como leídos en esta invocación.
     * 0 indica idempotencia: no había mensajes pendientes (ya estaban todos leídos).
     */
    private int messagesMarked;

    /**
     * Timestamp aplicado a los mensajes marcados.
     * El frontend puede usarlo para actualizar el estado local de cada mensaje afectado.
     */
    private Instant readAt;

    // Constructores
    public MarkMessagesAsReadResponseDTO() {
    }

    public MarkMessagesAsReadResponseDTO(Long chatSessionId, int messagesMarked, Instant readAt) {
        this.chatSessionId = chatSessionId;
        this.messagesMarked = messagesMarked;
        this.readAt = readAt;
    }

    // Getters y Setters
    public Long getChatSessionId() {
        return chatSessionId;
    }

    public void setChatSessionId(Long chatSessionId) {
        this.chatSessionId = chatSessionId;
    }

    public int getMessagesMarked() {
        return messagesMarked;
    }

    public void setMessagesMarked(int messagesMarked) {
        this.messagesMarked = messagesMarked;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
