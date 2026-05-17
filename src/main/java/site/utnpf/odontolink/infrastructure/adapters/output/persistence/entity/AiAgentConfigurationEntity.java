package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code ai_agent_configuration} (RF31, RF32).
 *
 * <p>Singleton: se asigna explicitamente el ID en la capa de aplicacion
 * ({@code AiAgentConfiguration.SINGLETON_ID}), siguiendo el mismo patron
 * que {@link InstitutionalSettingsEntity}.
 *
 * <p>El campo {@code lifecycle} guarda el nombre del enum del dominio
 * ({@code DRAFT} o {@code PUBLISHED}). El valor {@code UNCONFIGURED} no
 * se persiste: es virtual al no encontrar la fila.
 *
 * <p>Los guardrails NO viven en esta tabla. Existen como entidades propias
 * en {@code ai_guardrails}; el servicio los carga aparte cuando necesita
 * componer la instruccion final.
 */
@Entity
@Table(name = "ai_agent_configuration")
public class AiAgentConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

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

    @Column(name = "lifecycle", nullable = false, length = 20)
    private String lifecycle;

    @Column(name = "provider_agent_id", length = 100)
    private String providerAgentId;

    @Column(name = "provider_synced_at")
    private Instant providerSyncedAt;

    @Column(name = "last_sync_error", columnDefinition = "TEXT")
    private String lastSyncError;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AiAgentConfigurationEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(String lifecycle) {
        this.lifecycle = lifecycle;
    }

    public String getProviderAgentId() {
        return providerAgentId;
    }

    public void setProviderAgentId(String providerAgentId) {
        this.providerAgentId = providerAgentId;
    }

    public Instant getProviderSyncedAt() {
        return providerSyncedAt;
    }

    public void setProviderSyncedAt(Instant providerSyncedAt) {
        this.providerSyncedAt = providerSyncedAt;
    }

    public String getLastSyncError() {
        return lastSyncError;
    }

    public void setLastSyncError(String lastSyncError) {
        this.lastSyncError = lastSyncError;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
