package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import java.time.Instant;

/**
 * DTO para respuesta de mensaje de chat.
 * Implementa CU 6.2, CU 6.3: Enviar y Obtener Mensajes.
 *
 * Contiene toda la información del mensaje para mostrar en el cliente.
 * El timestamp 'sentAt' es crucial para el mecanismo de polling del frontend.
 *
 * @author OdontoLink Team
 */
public class ChatMessageResponseDTO {

    private Long id;
    private Long chatSessionId;
    private Long senderId;
    private String senderName;
    private String content;
    private Instant sentAt;

    // Constructor sin argumentos
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
}
