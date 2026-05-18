package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import site.utnpf.odontolink.domain.model.AiAgentAccessMode;
import site.utnpf.odontolink.domain.model.AiPiiPolicy;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.domain.model.Role;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Payload del PUT a {@code /api/admin/ai-agent/configuration}.
 *
 * <p>Bean Validation duplica las invariantes del dominio para devolver
 * 400 con detalle por campo antes de llegar al servicio (defensa en
 * profundidad). El servicio sigue validando.
 *
 * <p>Los guardrails y emergency keywords NO viajan aqui: se administran via
 * sus propios endpoints. La instruccion final que viaja al proveedor en
 * {@code POST /publish} se compone con los guardrails activos al momento
 * del publish.
 *
 * <p>Incluye el bloque chatbot (RF29/RF31/RF32/RF34): accessMode + roles +
 * politica PII + cap conversacional + rate limits + banner de emergencia.
 */
public class UpdateAiAgentConfigurationRequestDTO {

    @NotBlank
    private String displayName;

    @NotBlank
    private String systemPromptCore;

    private String welcomeMessage;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal temperature;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal topP;

    @Min(1)
    @Max(512)
    private int maxTokens;

    @Min(1)
    @Max(50)
    private int k;

    @NotNull
    private AiRetrievalMethod retrievalMethod;

    @NotNull
    private AiAgentAccessMode accessMode;

    /** Set de roles permitidos cuando accessMode==PRIVATE. Opcional en otros modos. */
    private Set<Role> allowedRoles;

    @NotNull
    private AiPiiPolicy piiPolicy;

    @Min(4)
    @Max(50)
    private int conversationBufferSize;

    @Min(1)
    @Max(1000)
    private int rateLimitAnonymousPerHour;

    @Min(1)
    @Max(5000)
    private int rateLimitAuthenticatedPerHour;

    @NotBlank
    private String emergencyBannerText;

    /**
     * Si true, el agente devuelve citas inline (referencias a documentos KB).
     * Default false: el caso clinico prefiere respuestas limpias.
     */
    private boolean provideCitations = false;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getSystemPromptCore() { return systemPromptCore; }
    public void setSystemPromptCore(String systemPromptCore) { this.systemPromptCore = systemPromptCore; }

    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }

    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }

    public BigDecimal getTopP() { return topP; }
    public void setTopP(BigDecimal topP) { this.topP = topP; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getK() { return k; }
    public void setK(int k) { this.k = k; }

    public AiRetrievalMethod getRetrievalMethod() { return retrievalMethod; }
    public void setRetrievalMethod(AiRetrievalMethod retrievalMethod) { this.retrievalMethod = retrievalMethod; }

    public AiAgentAccessMode getAccessMode() { return accessMode; }
    public void setAccessMode(AiAgentAccessMode accessMode) { this.accessMode = accessMode; }

    public Set<Role> getAllowedRoles() { return allowedRoles; }
    public void setAllowedRoles(Set<Role> allowedRoles) { this.allowedRoles = allowedRoles; }

    public AiPiiPolicy getPiiPolicy() { return piiPolicy; }
    public void setPiiPolicy(AiPiiPolicy piiPolicy) { this.piiPolicy = piiPolicy; }

    public int getConversationBufferSize() { return conversationBufferSize; }
    public void setConversationBufferSize(int conversationBufferSize) { this.conversationBufferSize = conversationBufferSize; }

    public int getRateLimitAnonymousPerHour() { return rateLimitAnonymousPerHour; }
    public void setRateLimitAnonymousPerHour(int rateLimitAnonymousPerHour) { this.rateLimitAnonymousPerHour = rateLimitAnonymousPerHour; }

    public int getRateLimitAuthenticatedPerHour() { return rateLimitAuthenticatedPerHour; }
    public void setRateLimitAuthenticatedPerHour(int rateLimitAuthenticatedPerHour) { this.rateLimitAuthenticatedPerHour = rateLimitAuthenticatedPerHour; }

    public String getEmergencyBannerText() { return emergencyBannerText; }
    public void setEmergencyBannerText(String emergencyBannerText) { this.emergencyBannerText = emergencyBannerText; }

    public boolean isProvideCitations() { return provideCitations; }
    public void setProvideCitations(boolean provideCitations) { this.provideCitations = provideCitations; }
}
