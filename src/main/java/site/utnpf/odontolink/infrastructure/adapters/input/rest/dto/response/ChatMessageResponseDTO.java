package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.time.Instant;

/**
 * DTO de respuesta para un mensaje de chat.
 * Implementa CU 6.2, CU 6.3 y CU012 (read receipts).
 *
 * El timestamp sentAt es crucial para el mecanismo de polling del frontend (CU 6.3):
 * el cliente envía el sentAt del último mensaje recibido como query param 'since' para
 * obtener solo los mensajes nuevos.
 *
 * El timestamp readAt habilita los read receipts (CU012): el frontend lo usa para mostrar
 * el "doble check azul" en los mensajes propios cuando ya fueron leídos por la contraparte.
 *
 * @author OdontoLink Team
 */
public class ChatMessageResponseDTO {

    private Long id;

    /**
     * ID de la sesión a la que pertenece el mensaje. Útil para el frontend cuando recibe
     * mensajes "sueltos" (ej. del polling) y necesita rutearlos al hilo correcto.
     */
    private Long chatSessionId;

    /**
     * ID del User que envió el mensaje (no del Patient/Practitioner). El frontend compara
     * este valor con el del usuario autenticado para alinear el mensaje a izquierda o derecha.
     */
    private Long senderId;

    /**
     * Nombre completo (firstName + lastName) del remitente. Denormalizado para evitar
     * lookups adicionales en el frontend.
     */
    private String senderName;

    /**
     * URL pública de la foto de perfil del remitente. Null si el usuario no subió avatar.
     * Denormalizado igual que {@link #senderName} para evitar lookups adicionales en el frontend.
     */
    private String senderProfilePictureUrl;

    /**
     * Contenido textual del mensaje. Máximo 2000 caracteres (validado en SendMessageRequestDTO).
     */
    private String content;

    /**
     * Timestamp de envío del mensaje. Usado por el frontend como cursor del polling RESTful.
     */
    private Instant sentAt;

    /**
     * Timestamp de lectura del mensaje. Null = aún no leído por el receptor.
     * Usado para el indicador "doble check azul" (CU012). Solo se completa cuando el
     * receptor invoca POST /sessions/{id}/messages/read.
     */
    private Instant readAt;

    // Constructores
    public ChatMessageResponseDTO() {
    }

    public ChatMessageResponseDTO(Long id, Long chatSessionId, Long senderId,
                                  String senderName, String content, Instant sentAt) {
        this.id = id;
        this.chatSessionId = chatSessionId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.sentAt = sentAt;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatSessionId() {
        return chatSessionId;
    }

    public void setChatSessionId(Long chatSessionId) {
        this.chatSessionId = chatSessionId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderProfilePictureUrl() {
        return senderProfilePictureUrl;
    }

    public void setSenderProfilePictureUrl(String senderProfilePictureUrl) {
        this.senderProfilePictureUrl = senderProfilePictureUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
