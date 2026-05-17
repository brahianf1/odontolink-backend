package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiAgentConfigurationVersionResponseDTO {

    private int versionNumber;
    private String displayName;
    private String systemPromptCore;
    private String welcomeMessage;
    private BigDecimal temperature;
    private BigDecimal topP;
    private int maxTokens;
    private int k;
    private AiRetrievalMethod retrievalMethod;
    private String composedInstruction;
    private String guardrailsLabelsSnapshot;
    private Long publishedByUserId;
    private boolean publishedWithOverride;
    private String missingRequirementsAtPublish;
    private Instant publishedAt;

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

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

    public String getComposedInstruction() { return composedInstruction; }
    public void setComposedInstruction(String composedInstruction) { this.composedInstruction = composedInstruction; }

    public String getGuardrailsLabelsSnapshot() { return guardrailsLabelsSnapshot; }
    public void setGuardrailsLabelsSnapshot(String guardrailsLabelsSnapshot) { this.guardrailsLabelsSnapshot = guardrailsLabelsSnapshot; }

    public Long getPublishedByUserId() { return publishedByUserId; }
    public void setPublishedByUserId(Long publishedByUserId) { this.publishedByUserId = publishedByUserId; }

    public boolean isPublishedWithOverride() { return publishedWithOverride; }
    public void setPublishedWithOverride(boolean publishedWithOverride) { this.publishedWithOverride = publishedWithOverride; }

    public String getMissingRequirementsAtPublish() { return missingRequirementsAtPublish; }
    public void setMissingRequirementsAtPublish(String missingRequirementsAtPublish) { this.missingRequirementsAtPublish = missingRequirementsAtPublish; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}
