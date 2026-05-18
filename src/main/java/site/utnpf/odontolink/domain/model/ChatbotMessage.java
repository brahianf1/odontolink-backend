package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje individual de un rolling buffer del chatbot.
 *
 * <p>Diseno deliberadamente "value-like": la sesion (agregado raiz) controla
 * el ciclo de vida. Los mensajes mas viejos se purgan automaticamente cuando
 * el buffer excede el cap configurado en
 * {@code AiAgentConfiguration.conversationBufferSize}.
 *
 * <p>Si la politica PII es {@link AiPiiPolicy#ANONYMIZE}, el {@code content}
 * que se persiste aqui es la version sanitizada (con placeholders). Si la
 * politica es {@link AiPiiPolicy#BLOCK} y se detecto PII, el mensaje del
 * usuario simplemente NO se persiste (evita retener PII aunque sea por error).
 */
public class ChatbotMessage {

    private Long id;
    private UUID sessionId;
    private ChatbotMessageRole role;
    private String content;
    private Instant createdAt;

    public ChatbotMessage() {
    }

    public ChatbotMessage(Long id,
                          UUID sessionId,
                          ChatbotMessageRole role,
                          String content,
                          Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static ChatbotMessage createNew(UUID sessionId, ChatbotMessageRole role, String content) {
        if (sessionId == null) {
            throw new InvalidBusinessRuleException("sessionId es obligatorio.");
        }
        if (role == null) {
            throw new InvalidBusinessRuleException("role es obligatorio.");
        }
        if (content == null || content.isBlank()) {
            throw new InvalidBusinessRuleException("content no puede ser vacio.");
        }
        return new ChatbotMessage(null, sessionId, role, content, Instant.now());
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

    public ChatbotMessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
