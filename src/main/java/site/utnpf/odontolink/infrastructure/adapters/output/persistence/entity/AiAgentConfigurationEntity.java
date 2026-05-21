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

    // -- Chatbot institucional (RF29/RF31/RF32/RF34) ---------------------
    // Nullable a nivel JPA para que ddl-auto=update no falle al agregar las
    // columnas a la fila singleton existente. La capa de aplicacion aplica
    // defaults al cargar (ver mapper) y valida no-null al guardar.

    @Column(name = "access_mode", length = 20)
    private String accessMode;

    /**
     * CSV de nombres del enum {@code Role}. Vacio o null cuando accessMode no
     * es PRIVATE. Persistido en VARCHAR para evitar tablas de join: el set
     * raramente supera 4 elementos y cambia con poca frecuencia.
     */
    @Column(name = "allowed_roles", length = 200)
    private String allowedRolesCsv;

    @Column(name = "pii_policy", length = 20)
    private String piiPolicy;

    @Column(name = "conversation_buffer_size")
    private Integer conversationBufferSize;

    @Column(name = "rate_limit_anon_per_hour")
    private Integer rateLimitAnonymousPerHour;

    @Column(name = "rate_limit_auth_per_hour")
    private Integer rateLimitAuthenticatedPerHour;

    @Column(name = "agent_invocation_url", length = 500)
    private String agentInvocationUrl;

    @Column(name = "emergency_banner_text", columnDefinition = "TEXT")
    private String emergencyBannerText;

    /**
     * Nullable a nivel JPA para que ddl-auto=update no rompa la fila singleton
     * existente al agregar la columna. El mapper aplica false como default si
     * la fila viene sin el campo.
     */
    @Column(name = "provide_citations")
    private Boolean provideCitations;

    /**
     * Toggle del indicador de confianza categorica (RF34). Nullable a nivel
     * JPA por la misma razon que {@link #provideCitations}: la fila singleton
     * existente en BD no tiene esta columna al actualizar a esta version. El
     * mapper aplica {@code true} como default (indicador visible) para
     * preservar el contrato historico.
     */
    @Column(name = "show_confidence_indicator")
    private Boolean showConfidenceIndicator;

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

    public String getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(String accessMode) {
        this.accessMode = accessMode;
    }

    public String getAllowedRolesCsv() {
        return allowedRolesCsv;
    }

    public void setAllowedRolesCsv(String allowedRolesCsv) {
        this.allowedRolesCsv = allowedRolesCsv;
    }

    public String getPiiPolicy() {
        return piiPolicy;
    }

    public void setPiiPolicy(String piiPolicy) {
        this.piiPolicy = piiPolicy;
    }

    public Integer getConversationBufferSize() {
        return conversationBufferSize;
    }

    public void setConversationBufferSize(Integer conversationBufferSize) {
        this.conversationBufferSize = conversationBufferSize;
    }

    public Integer getRateLimitAnonymousPerHour() {
        return rateLimitAnonymousPerHour;
    }

    public void setRateLimitAnonymousPerHour(Integer rateLimitAnonymousPerHour) {
        this.rateLimitAnonymousPerHour = rateLimitAnonymousPerHour;
    }

    public Integer getRateLimitAuthenticatedPerHour() {
        return rateLimitAuthenticatedPerHour;
    }

    public void setRateLimitAuthenticatedPerHour(Integer rateLimitAuthenticatedPerHour) {
        this.rateLimitAuthenticatedPerHour = rateLimitAuthenticatedPerHour;
    }

    public String getAgentInvocationUrl() {
        return agentInvocationUrl;
    }

    public void setAgentInvocationUrl(String agentInvocationUrl) {
        this.agentInvocationUrl = agentInvocationUrl;
    }

    public String getEmergencyBannerText() {
        return emergencyBannerText;
    }

    public void setEmergencyBannerText(String emergencyBannerText) {
        this.emergencyBannerText = emergencyBannerText;
    }

    public Boolean getProvideCitations() {
        return provideCitations;
    }

    public void setProvideCitations(Boolean provideCitations) {
        this.provideCitations = provideCitations;
    }

    public Boolean getShowConfidenceIndicator() {
        return showConfidenceIndicator;
    }

    public void setShowConfidenceIndicator(Boolean showConfidenceIndicator) {
        this.showConfidenceIndicator = showConfidenceIndicator;
    }
}
