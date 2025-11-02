package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entidad JPA para la tabla 'chat_messages'.
 * Representa un mensaje individual dentro de una sesión de chat en la base de datos.
 *
 * Esta entidad modela RF26 - CU 6.2, 6.3: Envío y consulta de mensajes de chat.
 * Los mensajes son inmutables una vez creados y se ordenan cronológicamente.
 *
 * @author OdontoLink Team
 */
@Entity
@Table(name = "chat_messages",
        indexes = {
            @Index(name = "idx_chat_message_session", columnList = "chat_session_id"),
            @Index(name = "idx_chat_message_sender", columnList = "sender_id"),
            @Index(name = "idx_chat_message_sent_at", columnList = "sent_at")
        })
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relación ManyToOne con ChatSessionEntity.
     * Un mensaje pertenece a una sesión de chat específica.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSessionEntity chatSession;

    /**
     * Relación ManyToOne con UserEntity.
     * Representa el usuario (paciente o practicante) que envió el mensaje.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    /**
     * Contenido textual del mensaje.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Timestamp de envío del mensaje.
     * Se usa para ordenamiento cronológico y para el mecanismo de polling.
     */
    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    /**
     * Constructor sin argumentos (requerido por JPA)
     */
    public ChatMessageEntity() {
        this.sentAt = Instant.now();
    }

    /**
     * Callback de JPA ejecutado antes de persistir la entidad.
     */
    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatSessionEntity getChatSession() {
        return chatSession;
    }

    public void setChatSession(ChatSessionEntity chatSession) {
        this.chatSession = chatSession;
    }

    public UserEntity getSender() {
        return sender;
    }

    public void setSender(UserEntity sender) {
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
