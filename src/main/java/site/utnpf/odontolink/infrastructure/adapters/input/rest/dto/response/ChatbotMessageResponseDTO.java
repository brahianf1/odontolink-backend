package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.ChatbotPiiType;
import site.utnpf.odontolink.domain.model.ConfidenceCategory;

import java.util.Set;
import java.util.UUID;

/**
 * Response de {@code POST /api/chatbot/messages} (RF29/RF31/RF32/RF34).
 *
 * <p>Estructura rica para que el FE pueda renderizar variaciones de UI sin
 * parsear el texto:
 * <ul>
 *   <li>{@code confidenceCategory} (RF34): clave estable que el FE usa para
 *       elegir badge / color / icono. Una de
 *       {@code OFFICIAL | PARTIAL | GENERAL | OUT_OF_SCOPE}, o {@code null}
 *       cuando aplica fallback, emergencia, PII bloqueado, o el admin
 *       desactivo el indicador en el panel.</li>
 *   <li>{@code confidenceCategoryLabel}: titulo corto en castellano
 *       argentino, listo para mostrar al paciente.</li>
 *   <li>{@code confidenceCategoryMessage}: mensaje explicativo para el
 *       paciente (sin tecnicismos). El FE lo muestra junto al badge.</li>
 *   <li>{@code confidenceScore}: 0-100 entero, util para observabilidad y
 *       admin (logs, dashboards, A/B testing). NO se muestra al paciente.</li>
 *   <li>{@code emergencyDetected}: derivacion: pintar banner rojo.</li>
 *   <li>{@code piiBlocked}: pedido educativo: pintar advertencia ambar.</li>
 *   <li>{@code fallbackTriggered}: proveedor caido: pintar gris.</li>
 * </ul>
 *
 * <p>Decision RF34: el response NO expone {@code retrievedDocumentIds} al
 * paciente. El indicador categorico ya transmite el grado de respaldo; las
 * referencias a documentos quedan en logs/admin para auditoria.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatbotMessageResponseDTO {

    private UUID sessionId;
    /** Solo presente en sesiones anonimas; el FE lo persiste en localStorage. */
    private UUID anonymousToken;
    private String reply;
    private ConfidenceCategory confidenceCategory;
    private String confidenceCategoryLabel;
    private String confidenceCategoryMessage;
    private Integer confidenceScore;
    private boolean emergencyDetected;
    private boolean piiBlocked;
    private Set<ChatbotPiiType> detectedPiiTypes;
    private boolean fallbackTriggered;
    private long latencyMs;

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getAnonymousToken() { return anonymousToken; }
    public void setAnonymousToken(UUID anonymousToken) { this.anonymousToken = anonymousToken; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public ConfidenceCategory getConfidenceCategory() { return confidenceCategory; }
    public void setConfidenceCategory(ConfidenceCategory confidenceCategory) { this.confidenceCategory = confidenceCategory; }

    public String getConfidenceCategoryLabel() { return confidenceCategoryLabel; }
    public void setConfidenceCategoryLabel(String confidenceCategoryLabel) { this.confidenceCategoryLabel = confidenceCategoryLabel; }

    public String getConfidenceCategoryMessage() { return confidenceCategoryMessage; }
    public void setConfidenceCategoryMessage(String confidenceCategoryMessage) { this.confidenceCategoryMessage = confidenceCategoryMessage; }

    public Integer getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Integer confidenceScore) { this.confidenceScore = confidenceScore; }

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
}
