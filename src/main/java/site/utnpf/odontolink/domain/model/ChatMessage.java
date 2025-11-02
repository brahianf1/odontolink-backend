package site.utnpf.odontolink.domain.model;

import java.time.Instant;

/**
 * Representa un único mensaje dentro de una ChatSession.
 * Los mensajes son inmutables una vez creados y se ordenan cronológicamente.
 *
 * Responsabilidades:
 * - Almacenar el contenido del mensaje
 * - Registrar el remitente (User)
 * - Mantener el timestamp de envío
 * - Relacionarse con la sesión de chat padre
 *
 * @author OdontoLink Team
 */
public class ChatMessage {
    private Long id;

    /** Relación N-a-1: La sesión a la que pertenece este mensaje */
    private ChatSession chatSession;

    /** Relación N-a-1: El User (Paciente o Practicante) que envió el mensaje */
    private User sender;

    /** Contenido textual del mensaje */
    private String content;

    /** Timestamp de envío del mensaje */
    private Instant sentAt;

    /**
     * Constructor sin argumentos (requerido por mappers de persistencia)
     */
    public ChatMessage() {
        this.sentAt = Instant.now();
    }

    /**
     * Constructor para crear un nuevo mensaje.
     *
     * @param chatSession La sesión de chat a la que pertenece el mensaje
     * @param sender El usuario que envía el mensaje
     * @param content El contenido textual del mensaje
     */
    public ChatMessage(ChatSession chatSession, User sender, String content) {
        this();
        this.chatSession = chatSession;
        this.sender = sender;
        this.content = content;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatSession getChatSession() {
        return chatSession;
    }

    public void setChatSession(ChatSession chatSession) {
        this.chatSession = chatSession;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
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