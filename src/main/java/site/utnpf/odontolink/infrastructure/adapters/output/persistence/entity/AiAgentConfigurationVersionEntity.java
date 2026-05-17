package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code ai_agent_configuration_versions} (RF31).
 *
 * <p>Snapshot inmutable de cada publicacion. Indice por {@code version_number}
 * para resolucion rapida de "buscar version N" (rollback).
 */
@Entity
@Table(name = "ai_agent_configuration_versions", indexes = {
        @Index(name = "ux_ai_config_versions_number", columnList = "version_number", unique = true)
})
public class AiAgentConfigurationVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "system_prompt_core", nullable = false, columnDefinition = "TEXT")
    private String systemPromptCore;

    @Column(name = "welcome_message", columnDefinition = "TEXT")
    private String welcomeMessage;

    @Column(name = "temperature", nullable = false, precision = 4, scale = 3)
    private BigDecimal temperature;

    @Column(name = "top_p", nullable = false, precision = 4, scale = 3)
    private BigDecimal topP;

    @Column(name = "max_tokens", nullable = false)
    private int maxTokens;

    @Column(name = "retrieval_k", nullable = false)
    private int k;

    @Column(name = "retrieval_method", nullable = false, length = 40)
    private String retrievalMethod;

    @Column(name = "composed_instruction", nullable = false, columnDefinition = "TEXT")
    private String composedInstruction;

    @Column(name = "guardrails_labels_snapshot", length = 1000)
    private String guardrailsLabelsSnapshot;

    @Column(name = "published_by_user_id")
    private Long publishedByUserId;

    @Column(name = "published_with_override", nullable = false)
    private boolean publishedWithOverride;

    @Column(name = "missing_requirements_at_publish", length = 500)
    private String missingRequirementsAtPublish;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    public AiAgentConfigurationVersionEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSystemPromptCore() {
        return systemPromptCore;
    }

    public void setSystemPromptCore(String systemPromptCore) {
        this.systemPromptCore = systemPromptCore;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getTopP() {
        return topP;
    }

    public void setTopP(BigDecimal topP) {
        this.topP = topP;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public String getRetrievalMethod() {
        return retrievalMethod;
    }

    public void setRetrievalMethod(String retrievalMethod) {
        this.retrievalMethod = retrievalMethod;
    }

    public String getComposedInstruction() {
        return composedInstruction;
    }

    public void setComposedInstruction(String composedInstruction) {
        this.composedInstruction = composedInstruction;
    }

    public String getGuardrailsLabelsSnapshot() {
        return guardrailsLabelsSnapshot;
    }

    public void setGuardrailsLabelsSnapshot(String guardrailsLabelsSnapshot) {
        this.guardrailsLabelsSnapshot = guardrailsLabelsSnapshot;
    }

    public Long getPublishedByUserId() {
        return publishedByUserId;
    }

    public void setPublishedByUserId(Long publishedByUserId) {
        this.publishedByUserId = publishedByUserId;
    }

    public boolean isPublishedWithOverride() {
        return publishedWithOverride;
    }

    public void setPublishedWithOverride(boolean publishedWithOverride) {
        this.publishedWithOverride = publishedWithOverride;
    }

    public String getMissingRequirementsAtPublish() {
        return missingRequirementsAtPublish;
    }

    public void setMissingRequirementsAtPublish(String missingRequirementsAtPublish) {
        this.missingRequirementsAtPublish = missingRequirementsAtPublish;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
