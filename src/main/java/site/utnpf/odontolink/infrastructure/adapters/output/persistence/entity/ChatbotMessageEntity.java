package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA para la tabla {@code ai_chatbot_messages} (RF29).
 *
 * <p>Mensajes del rolling buffer. El cap se aplica desde el servicio
 * eliminando los mas viejos cada vez que se inserta uno nuevo y se supera el
 * limite. El indice compuesto (session_id, created_at) acelera el listado
 * por sesion en orden cronologico.
 */
@Entity
@Table(
        name = "ai_chatbot_messages",
        indexes = {
                @Index(name = "idx_chatbot_msg_session_time", columnList = "session_id,created_at")
        }
)
public class ChatbotMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "session_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ChatbotMessageEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
