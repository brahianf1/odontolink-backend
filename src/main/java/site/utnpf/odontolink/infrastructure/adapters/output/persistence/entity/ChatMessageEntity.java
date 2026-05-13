package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entidad JPA para la tabla 'chat_messages'.
 *
 * Modela RF26 (envío y consulta) y CU012 (read receipts) mediante la columna read_at:
 * - read_at NULL = mensaje aún no leído por el destinatario
 * - read_at != NULL = timestamp en el que el destinatario abrió el chat por primera vez
 *
 * @author OdontoLink Team
 */
@Entity
@Table(name = "chat_messages",
        indexes = {
            @Index(name = "idx_chat_message_session", columnList = "chat_session_id"),
            @Index(name = "idx_chat_message_sender", columnList = "sender_id"),
            @Index(name = "idx_chat_message_sent_at", columnList = "sent_at"),
            // Índice compuesto para la query crítica de "no leídos" (countByChatSession + readAt IS NULL + sender != receiver).
            @Index(name = "idx_chat_message_session_read", columnList = "chat_session_id, read_at")
        })
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSessionEntity chatSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    /**
     * Timestamp en el que el receptor leyó el mensaje. null = aún no leído.
     * Nullable porque el mensaje nace "no leído" y se actualiza vía bulk-update cuando
     * el receptor abre la conversación.
     */
    @Column(name = "read_at", nullable = true)
    private Instant readAt;

    public ChatMessageEntity() {
        this.sentAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }

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

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
