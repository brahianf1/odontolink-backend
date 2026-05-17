package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.AiAgentLifecycle;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;

import java.math.BigDecimal;
import java.time.Instant;

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
}
