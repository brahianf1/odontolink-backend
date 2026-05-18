package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.ChatbotPiiType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Response de {@code POST /api/chatbot/messages} (RF29/RF31/RF32/RF34).
 *
 * <p>Estructura rica para que el FE pueda renderizar variaciones de UI sin
 * parsear el texto:
 * <ul>
 *   <li>{@code confidence}: 0-100 ({@code null} si emergencia o fallback).</li>
 *   <li>{@code basedOnKnowledgeBase}: si el modelo uso RAG.</li>
 *   <li>{@code emergencyDetected}: derivacion: pintar banner rojo.</li>
 *   <li>{@code piiBlocked}: pedido educativo: pintar advertencia ambar.</li>
 *   <li>{@code fallbackTriggered}: proveedor caido: pintar gris.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatbotMessageResponseDTO {

    private UUID sessionId;
    /** Solo presente en sesiones anonimas; el FE lo persiste en localStorage. */
    private UUID anonymousToken;
    private String reply;
    private Integer confidence;
    private boolean basedOnKnowledgeBase;
    private boolean emergencyDetected;
    private boolean piiBlocked;
    private Set<ChatbotPiiType> detectedPiiTypes;
    private boolean fallbackTriggered;
    private long latencyMs;
    private List<String> retrievedDocumentIds;

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getAnonymousToken() { return anonymousToken; }
    public void setAnonymousToken(UUID anonymousToken) { this.anonymousToken = anonymousToken; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public Integer getConfidence() { return confidence; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }

    public boolean isBasedOnKnowledgeBase() { return basedOnKnowledgeBase; }
    public void setBasedOnKnowledgeBase(boolean basedOnKnowledgeBase) { this.basedOnKnowledgeBase = basedOnKnowledgeBase; }

    public boolean isEmergencyDetected() { return emergencyDetected; }
    public void setEmergencyDetected(boolean emergencyDetected) { this.emergencyDetected = emergencyDetected; }

    public boolean isPiiBlocked() { return piiBlocked; }
    public void setPiiBlocked(boolean piiBlocked) { this.piiBlocked = piiBlocked; }

    public Set<ChatbotPiiType> getDetectedPiiTypes() { return detectedPiiTypes; }
    public void setDetectedPiiTypes(Set<ChatbotPiiType> detectedPiiTypes) { this.detectedPiiTypes = detectedPiiTypes; }

    public boolean isFallbackTriggered() { return fallbackTriggered; }
    public void setFallbackTriggered(boolean fallbackTriggered) { this.fallbackTriggered = fallbackTriggered; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public List<String> getRetrievedDocumentIds() { return retrievedDocumentIds; }
    public void setRetrievedDocumentIds(List<String> retrievedDocumentIds) { this.retrievedDocumentIds = retrievedDocumentIds; }
}
