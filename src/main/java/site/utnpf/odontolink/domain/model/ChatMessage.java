package site.utnpf.odontolink.domain.model;

import java.time.Instant;

/**
 * Representa un único mensaje dentro de una ChatSession.
 * Los mensajes son inmutables en cuanto a contenido y autor, pero su estado de lectura
 * (readAt) sí muta cuando el destinatario abre la conversación (CU012 - Read Receipts).
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
     * Timestamp de lectura. null = aún no leído.
     * Usamos un timestamp en lugar de un boolean porque permite reconstruir el flujo
     * (cuándo se leyó cada mensaje) y habilita futuras métricas de latencia de respuesta.
     */
    private Instant readAt;

    public ChatMessage() {
        this.sentAt = Instant.now();
    }

    public ChatMessage(ChatSession chatSession, User sender, String content) {
        this();
        this.chatSession = chatSession;
        this.sender = sender;
        this.content = content;
    }

    // Comportamientos del Dominio Rico

    /**
     * Marca el mensaje como leído. Idempotente: si ya estaba leído conserva el timestamp original
     * para no perder la marca temporal original (queremos saber cuándo se leyó por primera vez).
     */
    public void markAsRead(Instant readAt) {
        if (this.readAt == null) {
            this.readAt = readAt != null ? readAt : Instant.now();
        }
    }

    public boolean isRead() {
        return readAt != null;
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

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
