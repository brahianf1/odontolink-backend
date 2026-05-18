package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA para la tabla {@code ai_chatbot_sessions} (RF29).
 *
 * <p>Sesion liviana: solo metadata. Los mensajes viven en
 * {@link ChatbotMessageEntity} con cap configurable. El invariante de
 * negocio "exactamente uno de {ownerUserId, anonymousToken} no-null" lo
 * valida el dominio antes de mappear; el storage MySQL no aplica CHECK en
 * versiones antiguas asi que no lo declaramos a nivel schema.
 */
@Entity
@Table(
        name = "ai_chatbot_sessions",
        indexes = {
                @Index(name = "idx_chatbot_session_owner", columnList = "owner_user_id"),
                @Index(name = "idx_chatbot_session_anon", columnList = "anonymous_token", unique = false),
                @Index(name = "idx_chatbot_session_last", columnList = "last_interaction_at")
        }
)
public class ChatbotSessionEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "anonymous_token", columnDefinition = "BINARY(16)")
    private UUID anonymousToken;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_interaction_at", nullable = false)
    private Instant lastInteractionAt;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    public ChatbotSessionEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public UUID getAnonymousToken() {
        return anonymousToken;
    }

    public void setAnonymousToken(UUID anonymousToken) {
        this.anonymousToken = anonymousToken;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void setLastInteractionAt(Instant lastInteractionAt) {
        this.lastInteractionAt = lastInteractionAt;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}
