package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.AiAgentAccessMode;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.AiPiiPolicy;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.domain.model.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/**
 * Vista de respuesta para el agregado
 * {@code AiAgentConfiguration}. Incluye campos editables + estado
 * (lifecycle, providerSyncedAt, lastSyncError) + preview de la
 * instruccion concatenada que viajaria al proveedor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiAgentConfigurationResponseDTO {

    private String displayName;
    private String systemPromptCore;
    private String welcomeMessage;
    private BigDecimal temperature;
    private BigDecimal topP;
    private int maxTokens;
    private int k;
    private AiRetrievalMethod retrievalMethod;
    private AiAgentLifecycle lifecycle;
    private String finalInstructionPreview;
    private String providerAgentId;
    private Instant providerSyncedAt;
    private String lastSyncError;
    private Instant updatedAt;
    private AiAgentAccessMode accessMode;
    private Set<Role> allowedRoles;
    private AiPiiPolicy piiPolicy;
    private Integer conversationBufferSize;
    private Integer rateLimitAnonymousPerHour;
    private Integer rateLimitAuthenticatedPerHour;
    private String agentInvocationUrl;
    private String emergencyBannerText;
    private Boolean provideCitations;

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

    public AiAgentLifecycle getLifecycle() { return lifecycle; }
    public void setLifecycle(AiAgentLifecycle lifecycle) { this.lifecycle = lifecycle; }

    public String getFinalInstructionPreview() { return finalInstructionPreview; }
    public void setFinalInstructionPreview(String finalInstructionPreview) { this.finalInstructionPreview = finalInstructionPreview; }

    public String getProviderAgentId() { return providerAgentId; }
    public void setProviderAgentId(String providerAgentId) { this.providerAgentId = providerAgentId; }

    public Instant getProviderSyncedAt() { return providerSyncedAt; }
    public void setProviderSyncedAt(Instant providerSyncedAt) { this.providerSyncedAt = providerSyncedAt; }

    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public AiAgentAccessMode getAccessMode() { return accessMode; }
    public void setAccessMode(AiAgentAccessMode accessMode) { this.accessMode = accessMode; }

    public Set<Role> getAllowedRoles() { return allowedRoles; }
    public void setAllowedRoles(Set<Role> allowedRoles) { this.allowedRoles = allowedRoles; }

    public AiPiiPolicy getPiiPolicy() { return piiPolicy; }
    public void setPiiPolicy(AiPiiPolicy piiPolicy) { this.piiPolicy = piiPolicy; }

    public Integer getConversationBufferSize() { return conversationBufferSize; }
    public void setConversationBufferSize(Integer conversationBufferSize) { this.conversationBufferSize = conversationBufferSize; }

    public Integer getRateLimitAnonymousPerHour() { return rateLimitAnonymousPerHour; }
    public void setRateLimitAnonymousPerHour(Integer rateLimitAnonymousPerHour) { this.rateLimitAnonymousPerHour = rateLimitAnonymousPerHour; }

    public Integer getRateLimitAuthenticatedPerHour() { return rateLimitAuthenticatedPerHour; }
    public void setRateLimitAuthenticatedPerHour(Integer rateLimitAuthenticatedPerHour) { this.rateLimitAuthenticatedPerHour = rateLimitAuthenticatedPerHour; }

    public String getAgentInvocationUrl() { return agentInvocationUrl; }
    public void setAgentInvocationUrl(String agentInvocationUrl) { this.agentInvocationUrl = agentInvocationUrl; }

    public String getEmergencyBannerText() { return emergencyBannerText; }
    public void setEmergencyBannerText(String emergencyBannerText) { this.emergencyBannerText = emergencyBannerText; }

    public Boolean getProvideCitations() { return provideCitations; }
    public void setProvideCitations(Boolean provideCitations) { this.provideCitations = provideCitations; }
}
